package mx.buap.fcc.microplan.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mx.buap.fcc.microplan.data.local.MicroPlanDatabase
import mx.buap.fcc.microplan.data.local.SeedDataInitializer
import mx.buap.fcc.microplan.data.local.UserPreferencesDataSource
import mx.buap.fcc.microplan.data.remote.AiBreakdownService
import mx.buap.fcc.microplan.data.remote.LocalHeuristicAiBreakdownService
import mx.buap.fcc.microplan.data.remote.LocalNotificationPushService
import mx.buap.fcc.microplan.data.remote.OfflineStubSyncManager
import mx.buap.fcc.microplan.data.remote.OfflineTelemetrySyncServiceStub
import mx.buap.fcc.microplan.data.remote.RemoteAuthDataSource
import mx.buap.fcc.microplan.data.remote.RemotePushService
import mx.buap.fcc.microplan.data.remote.StubRemoteAuthDataSource
import mx.buap.fcc.microplan.data.remote.SyncManager
import mx.buap.fcc.microplan.data.remote.TelemetrySyncService
import mx.buap.fcc.microplan.data.repository.LocalAuthRepositoryImpl
import mx.buap.fcc.microplan.data.repository.LocalFocusRepositoryImpl
import mx.buap.fcc.microplan.data.repository.LocalPlannerRepositoryImpl
import mx.buap.fcc.microplan.data.repository.LocalTelemetryRepositoryImpl
import mx.buap.fcc.microplan.data.repository.LocalWellnessRepositoryImpl
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.FocusRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.repository.WellnessRepository
import mx.buap.fcc.microplan.domain.usecase.RebalanceScheduleUseCase
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val database: MicroPlanDatabase by lazy { MicroPlanDatabase.build(appContext) }
  val preferencesDataSource: UserPreferencesDataSource by lazy { UserPreferencesDataSource(appContext) }
  val seedDataInitializer: SeedDataInitializer by lazy { SeedDataInitializer(database, preferencesDataSource) }

  // Domain UseCases
  val rebalanceScheduleUseCase: RebalanceScheduleUseCase by lazy { RebalanceScheduleUseCase() }
  val streakEvaluatorUseCase: StreakEvaluatorUseCase by lazy { StreakEvaluatorUseCase() }

  // Remote interfaces / Offline-First local & stub implementations (Fase 2 ready)
  val remoteAuthDataSource: RemoteAuthDataSource by lazy { StubRemoteAuthDataSource() }
  val aiBreakdownService: AiBreakdownService by lazy { LocalHeuristicAiBreakdownService() }
  val syncManager: SyncManager by lazy { OfflineStubSyncManager(database) }
  val remotePushService: RemotePushService by lazy { LocalNotificationPushService(appContext) }
  val telemetrySyncService: TelemetrySyncService by lazy { OfflineTelemetrySyncServiceStub(database.telemetryDao()) }

  // Repositories
  val telemetryRepository: TelemetryRepository by lazy {
    LocalTelemetryRepositoryImpl(database.telemetryDao())
  }

  val authRepository: AuthRepository by lazy {
    LocalAuthRepositoryImpl(
      database = database,
      prefs = preferencesDataSource,
      seedInitializer = seedDataInitializer,
      remoteAuthDataSource = remoteAuthDataSource,
      remotePushService = remotePushService,
      telemetryRepository = telemetryRepository
    )
  }

  val plannerRepository: PlannerRepository by lazy {
    LocalPlannerRepositoryImpl(
      database = database,
      prefs = preferencesDataSource,
      aiBreakdownService = aiBreakdownService,
      rebalanceUseCase = rebalanceScheduleUseCase,
      streakEvaluator = streakEvaluatorUseCase,
      telemetryRepository = telemetryRepository
    )
  }

  val focusRepository: FocusRepository by lazy {
    LocalFocusRepositoryImpl(
      database = database,
      prefs = preferencesDataSource,
      streakEvaluator = streakEvaluatorUseCase,
      remotePushService = remotePushService,
      telemetryRepository = telemetryRepository
    )
  }

  val wellnessRepository: WellnessRepository by lazy {
    LocalWellnessRepositoryImpl(
      database = database,
      prefs = preferencesDataSource,
      rebalanceUseCase = rebalanceScheduleUseCase,
      streakEvaluator = streakEvaluatorUseCase,
      remotePushService = remotePushService,
      telemetryRepository = telemetryRepository
    )
  }
}

class SimpleViewModelFactory<T : ViewModel>(
  private val creator: () -> T
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
