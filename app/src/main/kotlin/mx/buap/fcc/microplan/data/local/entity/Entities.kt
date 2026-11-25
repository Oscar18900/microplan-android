package mx.buap.fcc.microplan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import mx.buap.fcc.microplan.domain.model.PomodoroOutcome
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType

@Entity(
  tableName = "local_users",
  indices = [Index(value = ["email"], unique = true)]
)
data class LocalUserEntity(
  @PrimaryKey val id: String,
  val fullName: String,
  val email: String,
  val passwordHash: String,
  val careerGroup: String,
  val initials: String,
  val sleepGoalHours: Float,
  val disconnectAlertMinutes: Int,
  val curfewBedtimeMinutes: Int,
  val rigidity: SleepRigidity,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val title: String,
  val courseName: String,
  val dueDateEpochDay: Long,
  val categoryIcon: ProjectCategoryIcon,
  val isAiGenerated: Boolean,
  val createdAtEpochDay: Long,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(
  tableName = "subtasks",
  foreignKeys = [
    ForeignKey(
      entity = ProjectEntity::class,
      parentColumns = ["id"],
      childColumns = ["projectId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["projectId"]),
    Index(value = ["assignedEpochDay"])
  ]
)
data class SubtaskEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val title: String,
  val assignedEpochDay: Long,
  val scheduledSlotMinutes: Int,
  val scheduledSlotLabel: String,
  val estimatedPomodoros: Int,
  val completedPomodoros: Int,
  val isCompleted: Boolean,
  val isOverCapacityAlert: Boolean,
  val sortOrder: Int,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(
  tableName = "pomodoro_sessions",
  indices = [Index(value = ["subtaskId"]), Index(value = ["startedAtMillis"])]
)
data class PomodoroSessionEntity(
  @PrimaryKey val id: String,
  val subtaskId: String,
  val subtaskTitle: String,
  val durationMinutes: Int,
  val startedAtMillis: Long,
  val endedAtMillis: Long,
  val outcome: PomodoroOutcome,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(tableName = "protected_blocks")
data class ProtectedBlockEntity(
  @PrimaryKey val id: String,
  val title: String,
  val type: ProtectedBlockType,
  val daysLabel: String,
  val startMinutes: Int,
  val endMinutes: Int,
  val description: String,
  val tagsSerialized: String,
  val isImmune: Boolean,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(tableName = "streak_history")
data class StreakDayEntity(
  @PrimaryKey val epochDay: Long,
  val completedTasksCount: Int,
  val respectedCurfew: Boolean,
  val emergencyUsed: Boolean,
  val streakMaintained: Boolean,
  val xpEarned: Int,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)

@Entity(
  tableName = "telemetry_events",
  indices = [Index(value = ["syncStatus"]), Index(value = ["createdAtMillis"])]
)
data class TelemetryEventEntity(
  @PrimaryKey val id: String,
  val eventType: TelemetryEventType,
  val payloadJson: String,
  val createdAtMillis: Long,
  val remoteId: String? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
  val updatedAt: Long
)
