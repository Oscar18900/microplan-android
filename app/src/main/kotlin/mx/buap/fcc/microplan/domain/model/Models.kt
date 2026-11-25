package mx.buap.fcc.microplan.domain.model

import java.time.LocalDate
import java.util.Locale

object TimeFormatters {
  fun formatMinutesAs12Hour(minutesFromMidnight: Int): String {
    val safe = ((minutesFromMidnight % 1440) + 1440) % 1440
    val hour24 = safe / 60
    val minute = safe % 60
    val period = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
  }

  fun formatMinutesAs24Hour(minutesFromMidnight: Int): String {
    val safe = ((minutesFromMidnight % 1440) + 1440) % 1440
    val hour24 = safe / 60
    val minute = safe % 60
    return String.format(Locale.US, "%02d:%02d h", hour24, minute)
  }

  fun computeInitials(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
      parts.isEmpty() -> "ES"
      parts.size == 1 -> parts.first().take(2).uppercase(Locale.getDefault())
      else -> "${parts.first().first()}${parts[1].first()}".uppercase(Locale.getDefault())
    }
  }

  fun relativeDayLabel(assignedEpochDay: Long, todayEpochDay: Long): String {
    val diff = assignedEpochDay - todayEpochDay
    return when {
      diff < 0L -> "Atrasado"
      diff == 0L -> "Día 1 (Hoy)"
      diff == 1L -> "Día 2 (Mañana)"
      else -> {
        val date = LocalDate.ofEpochDay(assignedEpochDay)
        val dayAbbr = when (date.dayOfWeek.value) {
          1 -> "Lun"
          2 -> "Mar"
          3 -> "Mié"
          4 -> "Jue"
          5 -> "Vie"
          6 -> "Sáb"
          else -> "Dom"
        }
        "Día ${diff + 1} ($dayAbbr)"
      }
    }
  }
}

data class LocalUser(
  val id: String,
  val fullName: String,
  val email: String,
  val careerGroup: String,
  val initials: String,
  val sleepGoalHours: Float,
  val disconnectAlertMinutes: Int,
  val curfewBedtimeMinutes: Int,
  val rigidity: SleepRigidity,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
)

data class Subtask(
  val id: String,
  val projectId: String,
  val title: String,
  val assignedEpochDay: Long,
  val scheduledSlotMinutes: Int,
  val scheduledSlotLabel: String,
  val estimatedPomodoros: Int,
  val completedPomodoros: Int = 0,
  val isCompleted: Boolean = false,
  val isOverCapacityAlert: Boolean = false,
  val sortOrder: Int = 0,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
)

data class AcademicProject(
  val id: String,
  val title: String,
  val courseName: String,
  val dueDateEpochDay: Long,
  val categoryIcon: ProjectCategoryIcon,
  val isAiGenerated: Boolean = false,
  val createdAtEpochDay: Long,
  val subtasks: List<Subtask> = emptyList(),
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
) {
  val totalPomodoros: Int
    get() = subtasks.sumOf { it.estimatedPomodoros }

  val completedPomodoros: Int
    get() = subtasks.sumOf { if (it.isCompleted) it.estimatedPomodoros else it.completedPomodoros.coerceAtMost(it.estimatedPomodoros) }

  val remainingPomodoros: Int
    get() = (totalPomodoros - completedPomodoros).coerceAtLeast(0)

  fun daysRemaining(todayEpochDay: Long): Int =
    (dueDateEpochDay - todayEpochDay).coerceAtLeast(0L).toInt()
}

data class PomodoroSession(
  val id: String,
  val subtaskId: String,
  val subtaskTitle: String,
  val durationMinutes: Int,
  val startedAtMillis: Long,
  val endedAtMillis: Long,
  val outcome: PomodoroOutcome,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
)

data class ProtectedBlock(
  val id: String,
  val title: String,
  val type: ProtectedBlockType,
  val daysLabel: String,
  val startMinutes: Int,
  val endMinutes: Int,
  val description: String,
  val tags: List<String>,
  val isImmune: Boolean,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun timeRangeLabel(): String =
    "${TimeFormatters.formatMinutesAs12Hour(startMinutes)} - ${TimeFormatters.formatMinutesAs12Hour(endMinutes)}"
}

data class StreakDayRecord(
  val epochDay: Long,
  val completedTasksCount: Int,
  val respectedCurfew: Boolean,
  val emergencyUsed: Boolean,
  val streakMaintained: Boolean,
  val xpEarned: Int,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
)

data class TelemetryEvent(
  val id: String,
  val eventType: TelemetryEventType,
  val payloadJson: String,
  val createdAtMillis: Long,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long = System.currentTimeMillis()
)

data class UserPreferences(
  val isLoggedIn: Boolean = false,
  val currentUserId: String = "",
  val currentUserName: String = "Angel García",
  val currentUserEmail: String = "estudiante@correo.edu.mx",
  val careerGroup: String = "Ciencias de la Computación · Grupo Piloto",
  val sleepGoalHours: Float = 7.5f,
  val disconnectAlertMinutes: Int = 22 * 60 + 30, // 10:30 PM
  val curfewBedtimeMinutes: Int = 23 * 60,        // 11:00 PM
  val rigidity: SleepRigidity = SleepRigidity.BALANCED,
  val dailyPomodoroCapacity: Int = 4,
  val darkModeEnabled: Boolean = false,
  val xpPoints: Int = 340,
  val currentStreakDays: Int = 5,
  val todayCurfewResolvedEpochDay: Long = -1L,
  val emergencyModeActiveToday: Boolean = false,
  val seedInitialized: Boolean = false
)

data class Medal(
  val id: String,
  val title: String,
  val description: String,
  val isUnlocked: Boolean
)

data class GamificationSummary(
  val xpPoints: Int,
  val level: Int,
  val levelTitle: String,
  val xpInCurrentLevel: Int,
  val xpForNextLevel: Int,
  val medals: List<Medal>
)

data class DayStreakIndicator(
  val dayLetter: String,
  val epochDay: Long,
  val status: WeekDayStatus
)
