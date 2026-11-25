package mx.buap.fcc.microplan.data.local

import androidx.room.TypeConverter
import mx.buap.fcc.microplan.domain.model.PomodoroOutcome
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType

class Converters {
  @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
  @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

  @TypeConverter fun fromSleepRigidity(value: SleepRigidity): String = value.name
  @TypeConverter fun toSleepRigidity(value: String): SleepRigidity = SleepRigidity.valueOf(value)

  @TypeConverter fun fromProtectedBlockType(value: ProtectedBlockType): String = value.name
  @TypeConverter fun toProtectedBlockType(value: String): ProtectedBlockType = ProtectedBlockType.valueOf(value)

  @TypeConverter fun fromProjectCategoryIcon(value: ProjectCategoryIcon): String = value.name
  @TypeConverter fun toProjectCategoryIcon(value: String): ProjectCategoryIcon = ProjectCategoryIcon.valueOf(value)

  @TypeConverter fun fromPomodoroOutcome(value: PomodoroOutcome): String = value.name
  @TypeConverter fun toPomodoroOutcome(value: String): PomodoroOutcome = PomodoroOutcome.valueOf(value)

  @TypeConverter fun fromTelemetryEventType(value: TelemetryEventType): String = value.name
  @TypeConverter fun toTelemetryEventType(value: String): TelemetryEventType = TelemetryEventType.valueOf(value)
}
