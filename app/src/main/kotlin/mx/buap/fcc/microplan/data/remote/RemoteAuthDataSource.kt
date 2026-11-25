package mx.buap.fcc.microplan.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteAuthRequestDto(
  val email: String,
  val password: String
)

@Serializable
data class RemoteRegisterRequestDto(
  val fullName: String,
  val email: String,
  val password: String,
  val sleepGoalHours: Float,
  val disconnectAlertMinutes: Int
)

@Serializable
data class AuthTokenResponseDto(
  val accessToken: String,
  val refreshToken: String,
  val userId: String,
  val fullName: String,
  val email: String,
  val careerGroup: String
)

/**
 * Contrato desacoplado de autenticación remota para Fase 2 (REST/JWT).
 * En Fase 1 (Offline-First), LocalAuthRepositoryImpl atiende todo localmente.
 */
interface RemoteAuthDataSource {
  suspend fun authenticate(request: RemoteAuthRequestDto): Result<AuthTokenResponseDto>
  suspend fun register(request: RemoteRegisterRequestDto): Result<AuthTokenResponseDto>
  suspend fun revokeSession(accessToken: String): Result<Unit>
}

class StubRemoteAuthDataSource : RemoteAuthDataSource {
  override suspend fun authenticate(request: RemoteAuthRequestDto): Result<AuthTokenResponseDto> =
    Result.failure(UnsupportedOperationException("Modo offline activo: usando LocalAuthRepositoryImpl"))

  override suspend fun register(request: RemoteRegisterRequestDto): Result<AuthTokenResponseDto> =
    Result.failure(UnsupportedOperationException("Modo offline activo: registro guardado localmente en Room"))

  override suspend fun revokeSession(accessToken: String): Result<Unit> = Result.success(Unit)
}
