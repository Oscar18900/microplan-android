package mx.buap.fcc.microplan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mx.buap.fcc.microplan.data.local.entity.LocalUserEntity
import mx.buap.fcc.microplan.data.local.entity.PomodoroSessionEntity
import mx.buap.fcc.microplan.data.local.entity.ProjectEntity
import mx.buap.fcc.microplan.data.local.entity.ProtectedBlockEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.entity.SubtaskEntity
import mx.buap.fcc.microplan.data.local.entity.TelemetryEventEntity
import mx.buap.fcc.microplan.domain.model.SyncStatus

@Dao
interface UserDao {
  @Query("SELECT * FROM local_users WHERE id = :id LIMIT 1")
  fun observeUserById(id: String): Flow<LocalUserEntity?>

  @Query("SELECT * FROM local_users WHERE email = :email LIMIT 1")
  suspend fun findByEmail(email: String): LocalUserEntity?

  @Query("SELECT * FROM local_users WHERE id = :id LIMIT 1")
  suspend fun findById(id: String): LocalUserEntity?

  @Query("SELECT COUNT(*) FROM local_users")
  suspend fun countUsers(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(user: LocalUserEntity)
}

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY dueDateEpochDay ASC, createdAtEpochDay ASC")
  fun observeAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects ORDER BY dueDateEpochDay ASC, createdAtEpochDay ASC")
  suspend fun getAllProjects(): List<ProjectEntity>

  @Query("SELECT COUNT(*) FROM projects")
  suspend fun countProjects(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(project: ProjectEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(projects: List<ProjectEntity>)

  @Query("DELETE FROM projects WHERE id = :projectId")
  suspend fun deleteById(projectId: String)
}

@Dao
interface SubtaskDao {
  @Query("SELECT * FROM subtasks ORDER BY assignedEpochDay ASC, scheduledSlotMinutes ASC, sortOrder ASC")
  fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

  @Query("SELECT * FROM subtasks WHERE assignedEpochDay = :epochDay ORDER BY scheduledSlotMinutes ASC, sortOrder ASC")
  fun observeSubtasksForDay(epochDay: Long): Flow<List<SubtaskEntity>>

  @Query("SELECT * FROM subtasks ORDER BY assignedEpochDay ASC, scheduledSlotMinutes ASC, sortOrder ASC")
  suspend fun getAllSubtasks(): List<SubtaskEntity>

  @Query("SELECT * FROM subtasks WHERE id = :subtaskId LIMIT 1")
  suspend fun findById(subtaskId: String): SubtaskEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(subtask: SubtaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(subtasks: List<SubtaskEntity>)

  @Transaction
  suspend fun replaceAllSchedules(subtasks: List<SubtaskEntity>) {
    upsertAll(subtasks)
  }
}

@Dao
interface PomodoroSessionDao {
  @Query("SELECT * FROM pomodoro_sessions ORDER BY startedAtMillis DESC LIMIT 30")
  fun observeRecentSessions(): Flow<List<PomodoroSessionEntity>>

  @Query("SELECT * FROM pomodoro_sessions WHERE syncStatus != :syncedStatus")
  suspend fun getPendingSyncSessions(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<PomodoroSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: PomodoroSessionEntity)
}

@Dao
interface ProtectedBlockDao {
  @Query("SELECT * FROM protected_blocks ORDER BY startMinutes ASC")
  fun observeAllBlocks(): Flow<List<ProtectedBlockEntity>>

  @Query("SELECT * FROM protected_blocks ORDER BY startMinutes ASC")
  suspend fun getAllBlocks(): List<ProtectedBlockEntity>

  @Query("SELECT COUNT(*) FROM protected_blocks")
  suspend fun countBlocks(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(block: ProtectedBlockEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(blocks: List<ProtectedBlockEntity>)

  @Query("DELETE FROM protected_blocks WHERE id = :blockId")
  suspend fun deleteById(blockId: String)
}

@Dao
interface StreakHistoryDao {
  @Query("SELECT * FROM streak_history ORDER BY epochDay DESC LIMIT 30")
  fun observeHistory(): Flow<List<StreakDayEntity>>

  @Query("SELECT * FROM streak_history WHERE epochDay = :epochDay LIMIT 1")
  suspend fun findByDay(epochDay: Long): StreakDayEntity?

  @Query("SELECT * FROM streak_history")
  suspend fun getAll(): List<StreakDayEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(record: StreakDayEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(records: List<StreakDayEntity>)
}

@Dao
interface TelemetryDao {
  @Query("SELECT COUNT(*) FROM telemetry_events WHERE syncStatus != :syncedStatus")
  fun observePendingCount(syncedStatus: SyncStatus = SyncStatus.SYNCED): Flow<Int>

  @Query("SELECT * FROM telemetry_events ORDER BY createdAtMillis DESC LIMIT :limit")
  suspend fun getRecentEvents(limit: Int): List<TelemetryEventEntity>

  @Query("SELECT * FROM telemetry_events WHERE syncStatus != :syncedStatus ORDER BY createdAtMillis ASC")
  suspend fun getUnsyncedEvents(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<TelemetryEventEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: TelemetryEventEntity)
}
