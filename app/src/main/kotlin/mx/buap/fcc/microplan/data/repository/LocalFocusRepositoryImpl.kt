package mx.buap.fcc.microplan.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.data.local.UserPreferencesDataSource
import mx.buap.fcc.microplan.data.local.entity.PomodoroSessionEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.toDomain
import mx.buap.fcc.microplan.data.remote.RemotePushService
import mx.buap.fcc.microplan.domain.model.PomodoroOutcome
import mx.buap.fcc.microplan.domain.model.PomodoroSession
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.repository.FocusRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase
import java.time.LocalDate
import java.util.UUID

class LocalFocusRepositoryImpl(
  private val database: MicroPlanDatabase,
  private val prefs: UserPreferencesDataSource,
  private val streakEvaluator: StreakEvaluatorUseCase,
  private val remotePushService: RemotePushService,
  private val telemetryRepository: TelemetryRepository
) : FocusRepository {

  override fun observeRecentSessions(): Flow<List<PomodoroSession>> =
    database.pomodoroSessionDao().observeRecentSessions().map { list ->
      list.map { it.toDomain() }
    }

  override suspend fun recordPomodoroOutcome(
    subtaskId: String?,
    subtaskTitle: String,
    durationMinutes: Int,
    startedAtMillis: Long,
    outcome: PomodoroOutcome
  ) {
    val now = System.currentTimeMillis()
    val today = LocalDate.now().toEpochDay()
    val currentPrefs = prefs.getCurrent()

    database.pomodoroSessionDao().insert(
      PomodoroSessionEntity(
        id = "pom-${UUID.randomUUID()}",
        subtaskId = subtaskId ?: "general-focus",
        subtaskTitle = subtaskTitle,
        durationMinutes = durationMinutes,
        startedAtMillis = startedAtMillis,
        endedAtMillis = now,
        outcome = outcome,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )

    val existingSubtask = if (subtaskId != null) database.subtaskDao().findById(subtaskId) else null
    val existingDay = database.streakHistoryDao().findByDay(today)

    when (outcome) {
      PomodoroOutcome.COMPLETED -> {
        if (existingSubtask != null) {
          database.subtaskDao().upsert(
            existingSubtask.copy(
              completedPomodoros = (existingSubtask.completedPomodoros + 1).coerceAtLeast(existingSubtask.estimatedPomodoros),
              isCompleted = true,
              syncStatus = if (existingSubtask.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else existingSubtask.syncStatus,
              updatedAt = now
            )
          )
        }
        val nextXp = streakEvaluator.onPomodoroCompleted(currentPrefs.xpPoints)
        prefs.updateGamification(xpPoints = nextXp, currentStreakDays = currentPrefs.currentStreakDays)

        val completedToday = database.subtaskDao().getAllSubtasks().count { it.assignedEpochDay == today && it.isCompleted }
        database.streakHistoryDao().upsert(
          StreakDayEntity(
            epochDay = today,
            completedTasksCount = completedToday,
            respectedCurfew = existingDay?.respectedCurfew ?: false,
            emergencyUsed = existingDay?.emergencyUsed ?: false,
            streakMaintained = existingDay?.streakMaintained ?: !currentPrefs.emergencyModeActiveToday,
            xpEarned = (existingDay?.xpEarned ?: 0) + StreakEvaluatorUseCase.XP_PER_POMODORO,
            syncStatus = SyncStatus.LOCAL_ONLY,
            updatedAt = now
          )
        )
        remotePushService.triggerPomodoroFinishedAlert(subtaskTitle)
        telemetryRepository.logEvent(
          TelemetryEventType.POMODORO_COMPLETED,
          """{"durationMinutes":$durationMinutes,"subtaskId":"${subtaskId ?: "none"}"}"""
        )
      }

      PomodoroOutcome.EXTENDED -> {
        if (existingSubtask != null) {
          database.subtaskDao().upsert(
            existingSubtask.copy(
              completedPomodoros = existingSubtask.completedPomodoros + 1,
              syncStatus = if (existingSubtask.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else existingSubtask.syncStatus,
              updatedAt = now
            )
          )
        }
        val nextXp = streakEvaluator.onPomodoroCompleted(currentPrefs.xpPoints)
        prefs.updateGamification(xpPoints = nextXp, currentStreakDays = currentPrefs.currentStreakDays)
        telemetryRepository.logEvent(
          TelemetryEventType.POMODORO_EXTENDED,
          """{"durationMinutes":$durationMinutes,"subtaskId":"${subtaskId ?: "none"}"}"""
        )
      }

      PomodoroOutcome.PAUSED_LOST_STREAK -> {
        val (brokenStreak, sameXp) = streakEvaluator.onStreakBroken(currentPrefs.xpPoints)
        prefs.updateGamification(
          xpPoints = sameXp,
          currentStreakDays = brokenStreak,
          emergencyActiveToday = true
        )
        database.streakHistoryDao().upsert(
          StreakDayEntity(
            epochDay = today,
            completedTasksCount = existingDay?.completedTasksCount ?: 0,
            respectedCurfew = false,
            emergencyUsed = existingDay?.emergencyUsed ?: false,
            streakMaintained = false,
            xpEarned = existingDay?.xpEarned ?: 0,
            syncStatus = SyncStatus.LOCAL_ONLY,
            updatedAt = now
          )
        )
        telemetryRepository.logEvent(
          TelemetryEventType.POMODORO_PAUSED_STREAK_LOST,
          """{"subtaskId":"${subtaskId ?: "none"}"}"""
        )
      }
    }
  }
}
