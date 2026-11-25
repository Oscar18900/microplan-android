package mx.buap.fcc.microplan.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.data.local.UserPreferencesDataSource
import mx.buap.fcc.microplan.data.local.entity.ProjectEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.entity.SubtaskEntity
import mx.buap.fcc.microplan.data.local.toDomain
import mx.buap.fcc.microplan.data.local.toEntity
import mx.buap.fcc.microplan.data.remote.AiBreakdownRequestDto
import mx.buap.fcc.microplan.data.remote.AiBreakdownService
import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.usecase.RebalanceScheduleUseCase
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase
import java.time.LocalDate
import java.util.UUID

class LocalPlannerRepositoryImpl(
  private val database: MicroPlanDatabase,
  private val prefs: UserPreferencesDataSource,
  private val aiBreakdownService: AiBreakdownService,
  private val rebalanceUseCase: RebalanceScheduleUseCase,
  private val streakEvaluator: StreakEvaluatorUseCase,
  private val telemetryRepository: TelemetryRepository
) : PlannerRepository {

  override fun observeProjects(): Flow<List<AcademicProject>> =
    combine(
      database.projectDao().observeAllProjects(),
      database.subtaskDao().observeAllSubtasks()
    ) { projects, subtasks ->
      val grouped = subtasks.map { it.toDomain() }.groupBy { it.projectId }
      projects.map { project ->
        val items = (grouped[project.id] ?: emptyList())
          .sortedWith(compareBy({ it.assignedEpochDay }, { it.scheduledSlotMinutes }, { it.sortOrder }))
        project.toDomain(items)
      }
    }

  override fun observeSubtasksForDay(epochDay: Long): Flow<List<Subtask>> =
    database.subtaskDao().observeSubtasksForDay(epochDay).map { list ->
      list.map { it.toDomain() }
    }

  override fun observeAllSubtasks(): Flow<List<Subtask>> =
    database.subtaskDao().observeAllSubtasks().map { list -> list.map { it.toDomain() } }

  override suspend fun createManualProject(
    title: String,
    courseName: String,
    dueInDays: Int,
    estimatedPomodoros: Int
  ): Result<AcademicProject> {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) {
      return Result.failure(IllegalArgumentException("Escribe el título de la entrega académica."))
    }
    val cleanCourse = courseName.trim().ifBlank { "Asignatura Universitaria" }
    val safeDays = dueInDays.coerceIn(1, 60)
    val safePomodoros = estimatedPomodoros.coerceIn(1, 20)
    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val projectId = "proj-${UUID.randomUUID()}"

    val icon = inferCategoryIcon(cleanTitle, cleanCourse)
    val projectEntity = ProjectEntity(
      id = projectId,
      title = cleanTitle,
      courseName = cleanCourse,
      dueDateEpochDay = today + safeDays.toLong(),
      categoryIcon = icon,
      isAiGenerated = false,
      createdAtEpochDay = today,
      syncStatus = SyncStatus.LOCAL_ONLY,
      updatedAt = now
    )

    val generatedSubtasks = buildManualSubtasks(projectId, cleanTitle, safeDays, safePomodoros, today, now)
    database.projectDao().upsert(projectEntity)
    database.subtaskDao().upsertAll(generatedSubtasks)
    rebalanceAll()

    telemetryRepository.logEvent(
      TelemetryEventType.MANUAL_TASK_CREATED,
      """{"projectId":"$projectId","pomodoros":$safePomodoros}"""
    )

    val finalSubtasks = database.subtaskDao().getAllSubtasks().filter { it.projectId == projectId }.map { it.toDomain() }
    return Result.success(projectEntity.toDomain(finalSubtasks))
  }

  override suspend fun createAiBreakdownProject(activityPrompt: String): Result<AcademicProject> {
    val currentPrefs = prefs.getCurrent()
    val response = aiBreakdownService.breakdownActivity(
      AiBreakdownRequestDto(
        activityPrompt = activityPrompt,
        dailyPomodoroCapacity = currentPrefs.dailyPomodoroCapacity
      )
    ).getOrElse { return Result.failure(it) }

    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val projectId = "proj-${UUID.randomUUID()}"
    val icon = runCatching { ProjectCategoryIcon.valueOf(response.categoryHint) }
      .getOrDefault(ProjectCategoryIcon.LIGHTBULB)

    val projectEntity = ProjectEntity(
      id = projectId,
      title = response.projectTitle,
      courseName = response.suggestedCourse,
      dueDateEpochDay = today + response.recommendedDeadlineDays.toLong(),
      categoryIcon = icon,
      isAiGenerated = true,
      createdAtEpochDay = today,
      syncStatus = SyncStatus.LOCAL_ONLY,
      updatedAt = now
    )

    val subtaskEntities = response.subtasks.mapIndexed { index, dto ->
      SubtaskEntity(
        id = "sub-${UUID.randomUUID()}",
        projectId = projectId,
        title = dto.title,
        assignedEpochDay = today + dto.dayOffset.toLong(),
        scheduledSlotMinutes = 16 * 60,
        scheduledSlotLabel = "04:00 PM",
        estimatedPomodoros = dto.estimatedPomodoros,
        completedPomodoros = 0,
        isCompleted = false,
        isOverCapacityAlert = dto.estimatedPomodoros >= 3,
        sortOrder = index,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    }

    database.projectDao().upsert(projectEntity)
    database.subtaskDao().upsertAll(subtaskEntities)
    rebalanceAll()

    telemetryRepository.logEvent(
      TelemetryEventType.AI_BREAKDOWN_GENERATED,
      """{"projectId":"$projectId","mode":"${response.sourceMode}","subtasks":${subtaskEntities.size}}"""
    )

    val finalSubtasks = database.subtaskDao().getAllSubtasks().filter { it.projectId == projectId }.map { it.toDomain() }
    return Result.success(projectEntity.toDomain(finalSubtasks))
  }

  override suspend fun addManualSubtask(
    projectId: String,
    title: String,
    dueInDays: Int,
    estimatedPomodoros: Int
  ) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val existingCount = database.subtaskDao().getAllSubtasks().count { it.projectId == projectId }

    database.subtaskDao().upsert(
      SubtaskEntity(
        id = "sub-${UUID.randomUUID()}",
        projectId = projectId,
        title = cleanTitle,
        assignedEpochDay = today + dueInDays.coerceIn(0, 30).toLong(),
        scheduledSlotMinutes = 18 * 60,
        scheduledSlotLabel = "06:00 PM",
        estimatedPomodoros = estimatedPomodoros.coerceIn(1, 6),
        completedPomodoros = 0,
        isCompleted = false,
        isOverCapacityAlert = estimatedPomodoros >= 3,
        sortOrder = existingCount,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )
    rebalanceAll()
  }

  override suspend fun toggleSubtaskCompletion(subtaskId: String) {
    val entity = database.subtaskDao().findById(subtaskId) ?: return
    val wasCompleted = entity.isCompleted
    val nowCompleted = !wasCompleted
    val now = System.currentTimeMillis()
    val today = LocalDate.now().toEpochDay()

    database.subtaskDao().upsert(
      entity.copy(
        isCompleted = nowCompleted,
        completedPomodoros = if (nowCompleted) entity.estimatedPomodoros else 0,
        syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else entity.syncStatus,
        updatedAt = now
      )
    )

    val currentPrefs = prefs.getCurrent()
    val nextXp = streakEvaluator.calculateXpAfterTaskToggle(
      wasCompleted = wasCompleted,
      estimatedPomodoros = entity.estimatedPomodoros,
      currentXp = currentPrefs.xpPoints
    )
    prefs.updateGamification(xpPoints = nextXp, currentStreakDays = currentPrefs.currentStreakDays)

    val completedToday = database.subtaskDao().getAllSubtasks().count { it.assignedEpochDay == today && it.isCompleted }
    val existingDay = database.streakHistoryDao().findByDay(today)
    database.streakHistoryDao().upsert(
      StreakDayEntity(
        epochDay = today,
        completedTasksCount = completedToday,
        respectedCurfew = existingDay?.respectedCurfew ?: false,
        emergencyUsed = existingDay?.emergencyUsed ?: false,
        streakMaintained = existingDay?.streakMaintained ?: !currentPrefs.emergencyModeActiveToday,
        xpEarned = (existingDay?.xpEarned ?: 0) + if (nowCompleted) 25 else 0,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )

    if (nowCompleted) {
      telemetryRepository.logEvent(
        TelemetryEventType.SUBTASK_COMPLETED,
        """{"subtaskId":"$subtaskId","pomodoros":${entity.estimatedPomodoros}}"""
      )
    }
  }

  override suspend fun rebalanceAll() {
    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val currentPrefs = prefs.getCurrent()
    val projects = database.projectDao().getAllProjects().associate { it.id to it.toDomain(emptyList()) }
    val subtasks = database.subtaskDao().getAllSubtasks().map { it.toDomain() }
    val blocks = database.protectedBlockDao().getAllBlocks().map { it.toDomain() }

    val result = rebalanceUseCase.rebalance(
      subtasks = subtasks,
      projectsById = projects,
      protectedBlocks = blocks,
      todayEpochDay = today,
      dailyPomodoroCapacity = currentPrefs.dailyPomodoroCapacity,
      disconnectAlertMinutes = currentPrefs.disconnectAlertMinutes,
      nowMillis = now
    )

    database.subtaskDao().replaceAllSchedules(result.updatedSubtasks.map { it.toEntity() })
    telemetryRepository.logEvent(
      TelemetryEventType.SCHEDULE_REBALANCED,
      """{"overCapacityDays":${result.overCapacityDayEpochs.size}}"""
    )
  }

  override suspend fun deleteProject(projectId: String) {
    database.projectDao().deleteById(projectId)
    rebalanceAll()
  }

  private fun buildManualSubtasks(
    projectId: String,
    title: String,
    dueInDays: Int,
    totalPomodoros: Int,
    today: Long,
    now: Long
  ): List<SubtaskEntity> {
    if (totalPomodoros <= 2 || dueInDays <= 1) {
      return listOf(
        SubtaskEntity(
          id = "sub-${UUID.randomUUID()}",
          projectId = projectId,
          title = title,
          assignedEpochDay = today,
          scheduledSlotMinutes = 16 * 60,
          scheduledSlotLabel = "04:00 PM",
          estimatedPomodoros = totalPomodoros,
          completedPomodoros = 0,
          isCompleted = false,
          isOverCapacityAlert = totalPomodoros >= 3,
          sortOrder = 0,
          syncStatus = SyncStatus.LOCAL_ONLY,
          updatedAt = now
        )
      )
    }

    val chunks = mutableListOf<Int>()
    var remaining = totalPomodoros
    while (remaining > 0) {
      val block = remaining.coerceAtMost(2)
      chunks += block
      remaining -= block
    }
    return chunks.mapIndexed { index, pom ->
      val dayOffset = (index * dueInDays) / chunks.size.coerceAtLeast(1)
      val label = when (index) {
        0 -> "$title — Inicio"
        chunks.lastIndex -> "$title — Cierre y entrega"
        else -> "$title — Bloque ${index + 1}"
      }
      SubtaskEntity(
        id = "sub-${UUID.randomUUID()}",
        projectId = projectId,
        title = label,
        assignedEpochDay = today + dayOffset.toLong(),
        scheduledSlotMinutes = 16 * 60,
        scheduledSlotLabel = "04:00 PM",
        estimatedPomodoros = pom,
        completedPomodoros = 0,
        isCompleted = false,
        isOverCapacityAlert = pom >= 3,
        sortOrder = index,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    }
  }

  private fun inferCategoryIcon(title: String, course: String): ProjectCategoryIcon {
    val lower = "$title $course".lowercase()
    return when {
      lower.contains("base") || lower.contains("sql") || lower.contains("sistema") -> ProjectCategoryIcon.DATABASE
      lower.contains("ensayo") || lower.contains("tesis") || lower.contains("lectura") -> ProjectCategoryIcon.BOOK
      lower.contains("diseño") || lower.contains("cognitiv") || lower.contains("neuro") -> ProjectCategoryIcon.LIGHTBULB
      else -> ProjectCategoryIcon.ASSIGNMENT
    }
  }
}
