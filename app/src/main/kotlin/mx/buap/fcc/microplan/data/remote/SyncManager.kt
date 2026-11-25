package mx.buap.fcc.microplan.data.remote

import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.domain.model.SyncStatus

data class SyncSummary(
  val pendingProjects: Int,
  val pendingSubtasks: Int,
  val pendingSessions: Int,
  val pendingTelemetryEvents: Int,
  val lastSyncTimestampMillis: Long?,
  val modeLabel: String
)

/**
 * Contrato de sincronización bidireccional Offline-First -> Cloud (Fase 2).
 */
interface SyncManager {
  suspend fun getPendingSummary(): SyncSummary
  suspend fun syncPendingChanges(): Result<SyncSummary>
}

class OfflineStubSyncManager(
  private val database: MicroPlanDatabase
) : SyncManager {

  override suspend fun getPendingSummary(): SyncSummary {
    val projects = database.projectDao().getAllProjects().count { it.syncStatus != SyncStatus.SYNCED }
    val subtasks = database.subtaskDao().getAllSubtasks().count { it.syncStatus != SyncStatus.SYNCED }
    val sessions = database.pomodoroSessionDao().getPendingSyncSessions().size
    val telemetry = database.telemetryDao().getUnsyncedEvents().size
    return SyncSummary(
      pendingProjects = projects,
      pendingSubtasks = subtasks,
      pendingSessions = sessions,
      pendingTelemetryEvents = telemetry,
      lastSyncTimestampMillis = null,
      modeLabel = "Offline-First local (nube en Fase 2)"
    )
  }

  override suspend fun syncPendingChanges(): Result<SyncSummary> {
    val summary = getPendingSummary()
    return Result.success(summary)
  }
}
