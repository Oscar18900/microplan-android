package mx.buap.fcc.microplan.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.LocalUser
import mx.buap.fcc.microplan.domain.model.PomodoroOutcome
import mx.buap.fcc.microplan.domain.model.PomodoroSession
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.StreakDayRecord
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.TelemetryEvent
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.model.UserPreferences

interface AuthRepository {
  val preferencesFlow: Flow<UserPreferences>
  val currentUserFlow: Flow<LocalUser?>

  suspend fun ensureSeeded()
  suspend fun login(email: String, password: String): Result<LocalUser>
  suspend fun register(
    fullName: String,
    email: String,
    password: String,
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int
  ): Result<LocalUser>
  suspend fun updateProfile(fullName: String, careerGroup: String)
  suspend fun setDarkMode(enabled: Boolean)
  suspend fun logout()
}

interface PlannerRepository {
  fun observeProjects(): Flow<List<AcademicProject>>
  fun observeSubtasksForDay(epochDay: Long): Flow<List<Subtask>>
  fun observeAllSubtasks(): Flow<List<Subtask>>

  suspend fun createManualProject(
    title: String,
    courseName: String,
    dueInDays: Int,
    estimatedPomodoros: Int
  ): Result<AcademicProject>

  suspend fun createAiBreakdownProject(
    activityPrompt: String
  ): Result<AcademicProject>

  suspend fun addManualSubtask(
    projectId: String,
    title: String,
    dueInDays: Int,
    estimatedPomodoros: Int
  )

  suspend fun toggleSubtaskCompletion(subtaskId: String)
  suspend fun rebalanceAll()
  suspend fun deleteProject(projectId: String)
}

interface FocusRepository {
  fun observeRecentSessions(): Flow<List<PomodoroSession>>
  suspend fun recordPomodoroOutcome(
    subtaskId: String?,
    subtaskTitle: String,
    durationMinutes: Int,
    startedAtMillis: Long,
    outcome: PomodoroOutcome
  )
}

interface WellnessRepository {
  fun observeProtectedBlocks(): Flow<List<ProtectedBlock>>
  fun observeStreakHistory(): Flow<List<StreakDayRecord>>
  suspend fun updateSleepSettings(
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    rigidity: SleepRigidity
  )
  suspend fun upsertProtectedBlock(
    id: String?,
    title: String,
    type: ProtectedBlockType,
    daysLabel: String,
    startMinutes: Int,
    endMinutes: Int,
    description: String,
    tags: List<String>,
    isImmune: Boolean
  )
  suspend fun deleteProtectedBlock(id: String)
  suspend fun rescheduleBalanceAndSleep(): Int
  suspend fun activateEmergencyMode()
  suspend fun sendTestDisconnectNotification()
}

interface TelemetryRepository {
  fun observePendingEventsCount(): Flow<Int>
  suspend fun logEvent(eventType: TelemetryEventType, payloadJson: String = "{}")
  suspend fun getRecentEvents(limit: Int = 50): List<TelemetryEvent>
}
