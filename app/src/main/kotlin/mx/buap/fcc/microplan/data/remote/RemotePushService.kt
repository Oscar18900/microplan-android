package mx.buap.fcc.microplan.data.remote

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.Serializable
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.notifications.CurfewNotificationWorker
import mx.buap.fcc.microplan.notifications.NotificationHelper
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Serializable
data class PushNotificationPayloadDto(
  val title: String,
  val body: String,
  val category: String
)

/**
 * Abstracción de notificaciones programadas.
 * Fase 1: WorkManager + NotificationManager locales.
 * Fase 2: Registro de token FCM y recepción remota sin alterar Wellness/Focus.
 */
interface RemotePushService {
  suspend fun scheduleSleepHygieneReminders(
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    sleepGoalHours: Float
  )
  suspend fun triggerImmediateDisconnectReminder(disconnectAlertMinutes: Int, sleepGoalHours: Float)
  suspend fun triggerPomodoroFinishedAlert(taskTitle: String)
  suspend fun registerDeviceTokenPlaceholder(token: String)
}

class LocalNotificationPushService(
  private val context: Context
) : RemotePushService {

  override suspend fun scheduleSleepHygieneReminders(
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    sleepGoalHours: Float
  ) {
    NotificationHelper.ensureChannels(context)
    val workManager = WorkManager.getInstance(context)

    scheduleDailyReminder(
      workManager = workManager,
      uniqueName = WORK_DISCONNECT_ALERT,
      targetMinutes = disconnectAlertMinutes,
      title = "¡${TimeFormatters.formatMinutesAs12Hour(disconnectAlertMinutes)}! Hora de desconectar",
      body = "Reprograma el saldo pendiente para mañana y protege tu meta de $sleepGoalHours h de sueño.",
      isCurfew = false
    )

    scheduleDailyReminder(
      workManager = workManager,
      uniqueName = WORK_CURFEW_ALERT,
      targetMinutes = curfewBedtimeMinutes,
      title = "Toque de queda nocturno (${TimeFormatters.formatMinutesAs12Hour(curfewBedtimeMinutes)})",
      body = "Hora límite para dormir y mantener tu racha saludable.",
      isCurfew = true
    )
  }

  override suspend fun triggerImmediateDisconnectReminder(
    disconnectAlertMinutes: Int,
    sleepGoalHours: Float
  ) {
    NotificationHelper.showSleepHygieneNotification(
      context = context,
      title = "¡${TimeFormatters.formatMinutesAs12Hour(disconnectAlertMinutes)}! Hora de desconectar",
      body = "Recordatorio de higiene del sueño: protege tu descanso de $sleepGoalHours h sin pantallas.",
      isCurfew = false
    )
  }

  override suspend fun triggerPomodoroFinishedAlert(taskTitle: String) {
    NotificationHelper.showPomodoroCompleteNotification(context, taskTitle)
  }

  override suspend fun registerDeviceTokenPlaceholder(token: String) {
    // No-op en Fase 1 offline; listo para enviar fcmToken al backend en Fase 2.
  }

  private fun scheduleDailyReminder(
    workManager: WorkManager,
    uniqueName: String,
    targetMinutes: Int,
    title: String,
    body: String,
    isCurfew: Boolean
  ) {
    val safeMinutes = ((targetMinutes % 1440) + 1440) % 1440
    val now = LocalDateTime.now()
    val targetTime = LocalTime.of(safeMinutes / 60, safeMinutes % 60)
    var nextRun = now.withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)
    if (!nextRun.isAfter(now)) {
      nextRun = nextRun.plusDays(1)
    }
    val initialDelayMinutes = Duration.between(now, nextRun).toMinutes().coerceAtLeast(1L)

    val data = Data.Builder()
      .putString(CurfewNotificationWorker.KEY_TITLE, title)
      .putString(CurfewNotificationWorker.KEY_BODY, body)
      .putBoolean(CurfewNotificationWorker.KEY_IS_CURFEW, isCurfew)
      .build()

    val request = PeriodicWorkRequestBuilder<CurfewNotificationWorker>(24, TimeUnit.HOURS)
      .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
      .setInputData(data)
      .build()

    workManager.enqueueUniquePeriodicWork(
      uniqueName,
      ExistingPeriodicWorkPolicy.UPDATE,
      request
    )
  }

  companion object {
    private const val WORK_DISCONNECT_ALERT = "microplan_disconnect_alert_work"
    private const val WORK_CURFEW_ALERT = "microplan_curfew_alert_work"
  }
}
