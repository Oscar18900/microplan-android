package mx.buap.fcc.microplan.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.data.local.PasswordHasher
import mx.buap.fcc.microplan.data.local.SeedDataInitializer
import mx.buap.fcc.microplan.data.local.UserPreferencesDataSource
import mx.buap.fcc.microplan.data.local.entity.LocalUserEntity
import mx.buap.fcc.microplan.data.local.toDomain
import mx.buap.fcc.microplan.data.remote.RemoteAuthDataSource
import mx.buap.fcc.microplan.data.remote.RemotePushService
import mx.buap.fcc.microplan.domain.model.LocalUser
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.domain.model.UserPreferences
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import java.util.UUID

class LocalAuthRepositoryImpl(
  private val database: MicroPlanDatabase,
  private val prefs: UserPreferencesDataSource,
  private val seedInitializer: SeedDataInitializer,
  @Suppress("unused") private val remoteAuthDataSource: RemoteAuthDataSource,
  private val remotePushService: RemotePushService,
  private val telemetryRepository: TelemetryRepository
) : AuthRepository {

  override val preferencesFlow: Flow<UserPreferences> = prefs.preferencesFlow

  @OptIn(ExperimentalCoroutinesApi::class)
  override val currentUserFlow: Flow<LocalUser?> = prefs.preferencesFlow.flatMapLatest { userPrefs ->
    if (!userPrefs.isLoggedIn || userPrefs.currentUserId.isBlank()) {
      flowOf(null)
    } else {
      database.userDao().observeUserById(userPrefs.currentUserId).map { it?.toDomain() }
    }
  }

  override suspend fun ensureSeeded() {
    seedInitializer.ensureSeeded()
  }

  override suspend fun login(email: String, password: String): Result<LocalUser> {
    seedInitializer.ensureSeeded()
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido."))
    }
    if (password.length < 6) {
      return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres."))
    }
    val user = database.userDao().findByEmail(cleanEmail)
      ?: return Result.failure(IllegalArgumentException("No existe una cuenta local con ese correo. Puedes registrarte en la pestaña Registrarse."))

    if (user.passwordHash != PasswordHasher.sha256(password)) {
      return Result.failure(IllegalArgumentException("Contraseña incorrecta. Verifica tus credenciales."))
    }

    prefs.setLoggedInUser(
      userId = user.id,
      fullName = user.fullName,
      email = user.email,
      careerGroup = user.careerGroup,
      sleepGoalHours = user.sleepGoalHours,
      disconnectAlertMinutes = user.disconnectAlertMinutes,
      curfewBedtimeMinutes = user.curfewBedtimeMinutes,
      rigidity = user.rigidity
    )
    remotePushService.scheduleSleepHygieneReminders(
      disconnectAlertMinutes = user.disconnectAlertMinutes,
      curfewBedtimeMinutes = user.curfewBedtimeMinutes,
      sleepGoalHours = user.sleepGoalHours
    )
    telemetryRepository.logEvent(TelemetryEventType.LOGIN, """{"mode":"local"}""")
    return Result.success(user.toDomain())
  }

  override suspend fun register(
    fullName: String,
    email: String,
    password: String,
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int
  ): Result<LocalUser> {
    seedInitializer.ensureSeeded()
    val cleanName = fullName.trim()
    val cleanEmail = email.trim().lowercase()
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Ingresa tu nombre completo."))
    }
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Ingresa un correo electrónico universitario válido."))
    }
    if (password.length < 8) {
      return Result.failure(IllegalArgumentException("La contraseña debe tener mínimo 8 caracteres."))
    }
    if (sleepGoalHours < 7.0f) {
      return Result.failure(IllegalArgumentException("La meta saludable mínima es de 7.0 horas."))
    }

    val existing = database.userDao().findByEmail(cleanEmail)
    if (existing != null) {
      return Result.failure(IllegalArgumentException("Ese correo ya está registrado localmente. Inicia sesión."))
    }

    val now = System.currentTimeMillis()
    val curfewMinutes = (disconnectAlertMinutes + 30).coerceAtMost(23 * 60 + 59)
    val entity = LocalUserEntity(
      id = "user-${UUID.randomUUID()}",
      fullName = cleanName,
      email = cleanEmail,
      passwordHash = PasswordHasher.sha256(password),
      careerGroup = "Estudiante Universitario · Modo Offline",
      initials = TimeFormatters.computeInitials(cleanName),
      sleepGoalHours = sleepGoalHours,
      disconnectAlertMinutes = disconnectAlertMinutes,
      curfewBedtimeMinutes = curfewMinutes,
      rigidity = SleepRigidity.BALANCED,
      remoteId = null,
      syncStatus = SyncStatus.LOCAL_ONLY,
      updatedAt = now
    )
    database.userDao().upsert(entity)
    prefs.setLoggedInUser(
      userId = entity.id,
      fullName = entity.fullName,
      email = entity.email,
      careerGroup = entity.careerGroup,
      sleepGoalHours = entity.sleepGoalHours,
      disconnectAlertMinutes = entity.disconnectAlertMinutes,
      curfewBedtimeMinutes = entity.curfewBedtimeMinutes,
      rigidity = entity.rigidity
    )
    remotePushService.scheduleSleepHygieneReminders(
      disconnectAlertMinutes = entity.disconnectAlertMinutes,
      curfewBedtimeMinutes = entity.curfewBedtimeMinutes,
      sleepGoalHours = entity.sleepGoalHours
    )
    telemetryRepository.logEvent(
      TelemetryEventType.REGISTER,
      """{"sleepGoalHours":$sleepGoalHours,"disconnectMinutes":$disconnectAlertMinutes}"""
    )
    return Result.success(entity.toDomain())
  }

  override suspend fun updateProfile(fullName: String, careerGroup: String) {
    val cleanName = fullName.trim().ifBlank { "Estudiante" }
    val cleanCareer = careerGroup.trim().ifBlank { "Ciencias de la Computación" }
    val currentPrefs = prefs.getCurrent()
    val user = database.userDao().findById(currentPrefs.currentUserId)
    if (user != null) {
      database.userDao().upsert(
        user.copy(
          fullName = cleanName,
          careerGroup = cleanCareer,
          initials = TimeFormatters.computeInitials(cleanName),
          syncStatus = if (user.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else user.syncStatus,
          updatedAt = System.currentTimeMillis()
        )
      )
    }
    prefs.updateProfileInfo(cleanName, cleanCareer)
  }

  override suspend fun setDarkMode(enabled: Boolean) {
    prefs.setDarkMode(enabled)
    telemetryRepository.logEvent(TelemetryEventType.DARK_MODE_TOGGLED, """{"darkMode":$enabled}""")
  }

  override suspend fun logout() {
    telemetryRepository.logEvent(TelemetryEventType.LOGOUT)
    prefs.logout()
  }
}
