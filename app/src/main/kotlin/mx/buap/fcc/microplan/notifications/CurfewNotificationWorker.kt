package mx.buap.fcc.microplan.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CurfewNotificationWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val title = inputData.getString(KEY_TITLE) ?: "¡Hora de desconectar!"
    val body = inputData.getString(KEY_BODY)
      ?: "Has alcanzado tu límite nocturno para proteger tu meta de ≥ 7h de sueño."
    val isCurfew = inputData.getBoolean(KEY_IS_CURFEW, false)

    NotificationHelper.showSleepHygieneNotification(
      context = applicationContext,
      title = title,
      body = body,
      isCurfew = isCurfew
    )
    return Result.success()
  }

  companion object {
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"
    const val KEY_IS_CURFEW = "is_curfew"
  }
}
