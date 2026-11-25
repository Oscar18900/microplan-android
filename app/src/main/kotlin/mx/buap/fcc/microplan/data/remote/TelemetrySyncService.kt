package mx.buap.fcc.microplan.data.remote

import kotlinx.serialization.Serializable
import mx.buap.fcc.microplan.data.local.dao.TelemetryDao
import mx.buap.fcc.microplan.domain.model.TelemetryEvent

@Serializable
data class TelemetryEventDto(
  val eventId: String,
  val eventType: String,
  val payloadJson: String,
  val createdAtMillis: Long
)

@Serializable
data class TelemetryBatchDto(
  val devicePlatform: String = "android",
  val appVersion: String = "1.0.0",
  val events: List<TelemetryEventDto>
)

@Serializable
data class TelemetrySyncResponseDto(
  val acceptedCount: Int,
  val serverTimestampMillis: Long
)

/**
 * Cola local de eventos (adopción, foco, cumplimiento de sueño) lista para envío por lotes en Fase 2.
 */
interface TelemetrySyncService {
  suspend fun flushBatch(events: List<TelemetryEvent>): Result<TelemetrySyncResponseDto>
}

class OfflineTelemetrySyncServiceStub(
  private val telemetryDao: TelemetryDao
) : TelemetrySyncService {

  override suspend fun flushBatch(events: List<TelemetryEvent>): Result<TelemetrySyncResponseDto> {
    val pending = telemetryDao.getUnsyncedEvents()
    return Result.success(
      TelemetrySyncResponseDto(
        acceptedCount = 0,
        serverTimestampMillis = System.currentTimeMillis()
      )
    ).also {
      // Mantiene los registros en cola local hasta habilitar backend en Fase 2.
      pending.size
    }
  }
}
