package mx.buap.fcc.microplan.data.repository

import kotlinx.coroutines.flow.Flow
import mx.buap.fcc.microplan.data.local.dao.TelemetryDao
import mx.buap.fcc.microplan.data.local.entity.TelemetryEventEntity
import mx.buap.fcc.microplan.data.local.toDomain
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEvent
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import java.util.UUID

class LocalTelemetryRepositoryImpl(
  private val telemetryDao: TelemetryDao
) : TelemetryRepository {

  override fun observePendingEventsCount(): Flow<Int> =
    telemetryDao.observePendingCount()

  override suspend fun logEvent(eventType: TelemetryEventType, payloadJson: String) {
    val now = System.currentTimeMillis()
    telemetryDao.insert(
      TelemetryEventEntity(
        id = "evt-${UUID.randomUUID()}",
        eventType = eventType,
        payloadJson = payloadJson,
        createdAtMillis = now,
        remoteId = null,
        syncStatus = SyncStatus.LOCAL_ONLY,
        updatedAt = now
      )
    )
  }

  override suspend fun getRecentEvents(limit: Int): List<TelemetryEvent> =
    telemetryDao.getRecentEvents(limit).map { it.toDomain() }
}
