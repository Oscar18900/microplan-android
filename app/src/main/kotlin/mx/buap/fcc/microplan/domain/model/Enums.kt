package mx.buap.fcc.microplan.domain.model

enum class SyncStatus {
  LOCAL_ONLY,
  PENDING_SYNC,
  SYNCED
}

enum class SleepRigidity(val label: String, val description: String) {
  FLEXIBLE("Flexible", "Recordatorios suaves sin bloqueo estricto"),
  BALANCED("Balanceado", "Alerta a las 10:30 PM y reprogramación recomendada"),
  STRICT("Estricto", "Toque de queda firme para blindar 7h+ de sueño")
}

enum class ProtectedBlockType {
  DAYTIME_LEISURE,
  CLASS_SCHEDULE,
  CUSTOM
}

enum class ProjectCategoryIcon {
  BOOK,
  DATABASE,
  LIGHTBULB,
  ASSIGNMENT
}

enum class PomodoroOutcome {
  COMPLETED,
  EXTENDED,
  PAUSED_LOST_STREAK
}

enum class TelemetryEventType {
  APP_OPEN,
  LOGIN,
  REGISTER,
  LOGOUT,
  SUBTASK_COMPLETED,
  AI_BREAKDOWN_GENERATED,
  MANUAL_TASK_CREATED,
  SCHEDULE_REBALANCED,
  POMODORO_STARTED,
  POMODORO_COMPLETED,
  POMODORO_EXTENDED,
  POMODORO_PAUSED_STREAK_LOST,
  CURFEW_RESCHEDULE_SLEEP,
  EMERGENCY_MODE_STREAK_LOST,
  PROTECTED_BLOCK_UPDATED,
  SLEEP_SETTINGS_UPDATED,
  DARK_MODE_TOGGLED
}

enum class WeekDayStatus {
  COMPLETED,
  TODAY,
  PENDING,
  MISSED
}
