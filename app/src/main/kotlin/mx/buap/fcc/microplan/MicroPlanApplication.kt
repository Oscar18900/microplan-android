package mx.buap.fcc.microplan

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.di.AppContainer
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import mx.buap.fcc.microplan.notifications.NotificationHelper

class MicroPlanApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    NotificationHelper.ensureChannels(this)
    appScope.launch {
      container.authRepository.ensureSeeded()
      container.telemetryRepository.logEvent(TelemetryEventType.APP_OPEN)
    }
  }
}
