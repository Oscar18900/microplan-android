package mx.buap.fcc.microplan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mx.buap.fcc.microplan.data.local.dao.PomodoroSessionDao
import mx.buap.fcc.microplan.data.local.dao.ProjectDao
import mx.buap.fcc.microplan.data.local.dao.ProtectedBlockDao
import mx.buap.fcc.microplan.data.local.dao.StreakHistoryDao
import mx.buap.fcc.microplan.data.local.dao.SubtaskDao
import mx.buap.fcc.microplan.data.local.dao.TelemetryDao
import mx.buap.fcc.microplan.data.local.dao.UserDao
import mx.buap.fcc.microplan.data.local.entity.LocalUserEntity
import mx.buap.fcc.microplan.data.local.entity.PomodoroSessionEntity
import mx.buap.fcc.microplan.data.local.entity.ProjectEntity
import mx.buap.fcc.microplan.data.local.entity.ProtectedBlockEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.entity.SubtaskEntity
import mx.buap.fcc.microplan.data.local.entity.TelemetryEventEntity

@Database(
  entities = [
    LocalUserEntity::class,
    ProjectEntity::class,
    SubtaskEntity::class,
    PomodoroSessionEntity::class,
    ProtectedBlockEntity::class,
    StreakDayEntity::class,
    TelemetryEventEntity::class
  ],
  version = 1,
  exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MicroPlanDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun projectDao(): ProjectDao
  abstract fun subtaskDao(): SubtaskDao
  abstract fun pomodoroSessionDao(): PomodoroSessionDao
  abstract fun protectedBlockDao(): ProtectedBlockDao
  abstract fun streakHistoryDao(): StreakHistoryDao
  abstract fun telemetryDao(): TelemetryDao

  companion object {
    fun build(context: Context): MicroPlanDatabase =
      Room.databaseBuilder(
        context.applicationContext,
        MicroPlanDatabase::class.java,
        "microplan_offline.db"
      ).build()
  }
}
