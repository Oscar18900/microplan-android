package mx.buap.fcc.microplan.data.local

import mx.buap.fcc.microplan.data.local.entity.LocalUserEntity
import mx.buap.fcc.microplan.data.local.entity.PomodoroSessionEntity
import mx.buap.fcc.microplan.data.local.entity.ProjectEntity
import mx.buap.fcc.microplan.data.local.entity.ProtectedBlockEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.entity.SubtaskEntity
import mx.buap.fcc.microplan.data.local.entity.TelemetryEventEntity
import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.LocalUser
import mx.buap.fcc.microplan.domain.model.PomodoroSession
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.StreakDayRecord
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.TelemetryEvent

fun LocalUserEntity.toDomain(): LocalUser = LocalUser(
  id = id,
  fullName = fullName,
  email = email,
  careerGroup = careerGroup,
  initials = initials,
  sleepGoalHours = sleepGoalHours,
  disconnectAlertMinutes = disconnectAlertMinutes,
  curfewBedtimeMinutes = curfewBedtimeMinutes,
  rigidity = rigidity,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun SubtaskEntity.toDomain(): Subtask = Subtask(
  id = id,
  projectId = projectId,
  title = title,
  assignedEpochDay = assignedEpochDay,
  scheduledSlotMinutes = scheduledSlotMinutes,
  scheduledSlotLabel = scheduledSlotLabel,
  estimatedPomodoros = estimatedPomodoros,
  completedPomodoros = completedPomodoros,
  isCompleted = isCompleted,
  isOverCapacityAlert = isOverCapacityAlert,
  sortOrder = sortOrder,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun Subtask.toEntity(): SubtaskEntity = SubtaskEntity(
  id = id,
  projectId = projectId,
  title = title,
  assignedEpochDay = assignedEpochDay,
  scheduledSlotMinutes = scheduledSlotMinutes,
  scheduledSlotLabel = scheduledSlotLabel,
  estimatedPomodoros = estimatedPomodoros,
  completedPomodoros = completedPomodoros,
  isCompleted = isCompleted,
  isOverCapacityAlert = isOverCapacityAlert,
  sortOrder = sortOrder,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun ProjectEntity.toDomain(subtasks: List<Subtask>): AcademicProject = AcademicProject(
  id = id,
  title = title,
  courseName = courseName,
  dueDateEpochDay = dueDateEpochDay,
  categoryIcon = categoryIcon,
  isAiGenerated = isAiGenerated,
  createdAtEpochDay = createdAtEpochDay,
  subtasks = subtasks,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun PomodoroSessionEntity.toDomain(): PomodoroSession = PomodoroSession(
  id = id,
  subtaskId = subtaskId,
  subtaskTitle = subtaskTitle,
  durationMinutes = durationMinutes,
  startedAtMillis = startedAtMillis,
  endedAtMillis = endedAtMillis,
  outcome = outcome,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun ProtectedBlockEntity.toDomain(): ProtectedBlock = ProtectedBlock(
  id = id,
  title = title,
  type = type,
  daysLabel = daysLabel,
  startMinutes = startMinutes,
  endMinutes = endMinutes,
  description = description,
  tags = tagsSerialized.split("|").map { it.trim() }.filter { it.isNotEmpty() },
  isImmune = isImmune,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun StreakDayEntity.toDomain(): StreakDayRecord = StreakDayRecord(
  epochDay = epochDay,
  completedTasksCount = completedTasksCount,
  respectedCurfew = respectedCurfew,
  emergencyUsed = emergencyUsed,
  streakMaintained = streakMaintained,
  xpEarned = xpEarned,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)

fun TelemetryEventEntity.toDomain(): TelemetryEvent = TelemetryEvent(
  id = id,
  eventType = eventType,
  payloadJson = payloadJson,
  createdAtMillis = createdAtMillis,
  remoteId = remoteId,
  syncStatus = syncStatus,
  updatedAt = updatedAt
)
