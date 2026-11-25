package mx.buap.fcc.microplan.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.data.local.UserPreferencesDataSource
import mx.buap.fcc.microplan.data.local.entity.ProtectedBlockEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.toDomain
import mx.buap.fcc.microplan.data.local.toEntity
import mx.buap.fcc.microplan.data.remote.RemotePushService
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.StreakDayRecord
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.repository.WellnessRepository
import mx.buap.fcc.microplan.domain.usecase.RebalanceScheduleUseCase
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase
import java.time.LocalDate
import java.util.UUID

class LocalWellnessRepositoryImpl(
  private val database: MicroPlanDatabase,
  private val prefs: UserPreferencesDataSource,
  private val rebalanceUseCase: RebalanceScheduleUseCase,
  private val streakEvaluator: StreakEvaluatorUseCase,
  private val remotePushService: RemotePushService,
  private val telemetryRepository: TelemetryRepository
) : WellnessRepository {

  override fun observeProtectedBlocks(): Flow<List<ProtectedBlock>> =
    database.protectedBlockDao().observeAllBlocks().map { list -> list.map { it.toDomain() } }

  override fun observeStreakHistory(): Flow<List<StreakDayRecord>> =
    database.streakHistoryDao().observeHistory().map { list -> list.map { it.toDomain() } }

  override suspend fun updateSleepSettings(
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    rigidity: SleepRigidity
  ) {
    val safeGoal = sleepGoalHours.coerceAtLeast(7.0f)
    val currentPrefs = prefs.getCurrent()
    val user = database.userDao().findById(currentPrefs.currentUserId)
    if (user != null) {
      database.userDao().upsert(
        user.copy(
          sleepGoalHours = safeGoal,
          disconnectAlertMinutes = disconnectAlertMinutes,
          curfewBedtimeMinutes = curfewBedtimeMinutes,
          rigidity = rigidity,
          syncStatus = if (user.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else user.syncStatus,
          updatedAt = System.currentTimeMillis()
        )
      )
    }

    prefs.updateSleepSettings(safeGoal, disconnectAlertMinutes, curfewBedtimeMinutes, rigidity)
    remotePushService.scheduleSleepHygieneReminders(
      disconnectAlertMinutes = disconnectAlertMinutes,
      curfewBedtimeMinutes = curfewBedtimeMinutes,
      sleepGoalHours = safeGoal
    )
    telemetryRepository.logEvent(
      TelemetryEventType.SLEEP_SETTINGS_UPDATED,
      """{"sleepGoal":$safeGoal,"disconnect":$disconnectAlertMinutes,"curfew":$curfewBedtimeMinutes,"rigidity":"${rigidity.name}"}"""
    )
  }

  override suspend fun upsertProtectedBlock(
    id: String?,
    title: String,
    type: ProtectedBlockType,
    daysLabel: String,
    startMinutes: Int,
    endMinutes: Int,
    description: String,
    tags: List<String>,
    isImmune: Boolean
  ) {
    val blockId = id ?: "block-${UUID.randomUUID()}"
    val safeEnd = if (endMinutes <= startMinutes) (startMinutes + 60).coerceAtMost(23 * 60 + 59) else endMinutes
    val entity = ProtectedBlockEntity(
      id = blockId,
      title = title.trim().ifBlank { "Bloque Protegido" },
      type = type,
      daysLabel = daysLabel.trim().ifBlank { "Lun - Vie" },
      startMinutes = startMinutes,
      endMinutes = safeEnd,
      description = description.trim().ifBlank { "Espacio reservado en agenda" },
      tagsSerialized = tags.filter { it.isNotBlank() }.joinToString("|"),
      isImmune = isImmune,
      syncStatus = SyncStatus.LOCAL_ONLY,
      updatedAt = System.currentTimeMillis()
    )
    database.protectedBlockDao().upsert(entity)
    rebalanceTasksWithCurrentBlocks()
    telemetryRepository.logEvent(
      TelemetryEventType.PROTECTED_BLOCK_UPDATED,
      """{"blockId":"$blockId","immune":$isImmune}"""
    )
  }

  override suspend fun deleteProtectedBlock(id: String) {
    database.protectedBlockDao().deleteById(id)
    rebalanceTasksWithCurrentBlocks()
  }

  override suspend fun rescheduleBalanceAndSleep(): Int {
    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val currentPrefs = prefs.getCurrent()
    val projects = database.projectDao().getAllProjects().associate { it.id to it.toDomain(emptyList()) }
    val allSubtasks = database.subtaskDao().getAllSubtasks().map { it.toDomain() }
    val pendingTodayCount = allSubtasks.count { !it.isCompleted && it.assignedEpochDay <= today }
    val blocks = database.protectedBlockDao().getAllBlocks().map { it.toDomain() }

    val rebalanced = rebalanceUseCase.reschedulePendingTodayToTomorrow(
      allSubtasks = allSubtasks,
      projectsById = projects,
      protectedBlocks = blocks,
      todayEpochDay = today,
      dailyPomodoroCapacity = currentPrefs.dailyPomodoroCapacity,
      disconnectAlertMinutes = currentPrefs.disconnectAlertMinutes,
      nowMillis = now
    )
    database.subtaskDao().replaceAllSchedules(rebalanced.updatedSubtasks.map { it.toEntity() })

    val alreadyResolved = currentPrefs.todayCurfewResolvedEpochDay == today
    val (nextStreak, nextXp) = streakEvaluator.onCurfewSleepRescheduled(
      currentStreakDays = currentPrefs.currentStreakDays,
      currentXp = currentPrefs.xpPoints,
      alreadyResolvedToday = alreadyResolved
    )
    prefs.updateGamification(
      xpPoints = nextXp,
      currentStreakDays = nextStreak,
      resolvedEpochDay = today,
      emergencyActiveToday = false
    )

    val existingDay = database.streakHistoryDao().findByDay(today)
    database.streakHistoryDao().upsert(
      StreakDayEntity(
        epochDay = today,
        completedTasksCount = existingDay?.completedTasksCount ?: allSubtasks.count { it.assignedEpochDay == today && it.isCompleted },
        respectedCurfew = true,
        emergencyUsed = false,
        streakMaintained = true,
        xpEarned = (existingDay?.xpEarned ?: 0) + StreakEvaluatorUseCase.XP_CURFEW_PROTECTED,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )

    telemetryRepository.logEvent(
      TelemetryEventType.CURFEW_RESCHEDULE_SLEEP,
      """{"movedTasks":$pendingTodayCount,"newStreak":$nextStreak}"""
    )
    return pendingTodayCount
  }

  override suspend fun activateEmergencyMode() {
    val today = LocalDate.now().toEpochDay()
    val now = System.currentTimeMillis()
    val currentPrefs = prefs.getCurrent()
    val (brokenStreak, sameXp) = streakEvaluator.onStreakBroken(currentPrefs.xpPoints)

    prefs.updateGamification(
      xpPoints = sameXp,
      currentStreakDays = brokenStreak,
      resolvedEpochDay = today,
      emergencyActiveToday = true
    )

    val existingDay = database.streakHistoryDao().findByDay(today)
    database.streakHistoryDao().upsert(
      StreakDayEntity(
        epochDay = today,
        completedTasksCount = existingDay?.completedTasksCount ?: 0,
        respectedCurfew = false,
        emergencyUsed = true,
        streakMaintained = false,
        xpEarned = existingDay?.xpEarned ?: 0,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )

    telemetryRepository.logEvent(
      TelemetryEventType.EMERGENCY_MODE_STREAK_LOST,
      """{"epochDay":$today}"""
    )
  }

  override suspend fun sendTestDisconnectNotification() {
    val currentPrefs = prefs.getCurrent()
    remotePushService.triggerImmediateDisconnectReminder(
      disconnectAlertMinutes = currentPrefs.disconnectAlertMinutes,
      sleepGoalHours = currentPrefs.sleepGoalHours
    )
  }

  private suspend fun rebalanceTasksWithCurrentBlocks() {
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
  }
}
