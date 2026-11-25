package mx.buap.fcc.microplan.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mx.buap.fcc.microplan.MainActivity
import mx.buap.fcc.microplan.R

object NotificationHelper {
  const val CHANNEL_SLEEP_HYGIENE = "microplan_sleep_hygiene"
  const val CHANNEL_FOCUS = "microplan_focus_alerts"
  private const val NOTIFICATION_ID_DISCONNECT = 1001
  private const val NOTIFICATION_ID_CURFEW = 1002
  private const val NOTIFICATION_ID_POMODORO = 1003

  fun ensureChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val sleepChannel = NotificationChannel(
      CHANNEL_SLEEP_HYGIENE,
      "Higiene del Sueño y Desconexión",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Recordatorios de hora límite de desconexión y toque de queda para dormir ≥ 7h"
    }
    val focusChannel = NotificationChannel(
      CHANNEL_FOCUS,
      "Sesiones Pomodoro y Enfoque",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "Avisos al finalizar bloques de enfoque"
    }
    manager.createNotificationChannel(sleepChannel)
    manager.createNotificationChannel(focusChannel)
  }

  fun showSleepHygieneNotification(
    context: Context,
    title: String,
    body: String,
    isCurfew: Boolean = false
  ) {
    ensureChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      if (isCurfew) 2 else 1,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP_HYGIENE)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context)
      .notify(if (isCurfew) NOTIFICATION_ID_CURFEW else NOTIFICATION_ID_DISCONNECT, notification)
  }

  fun showPomodoroCompleteNotification(context: Context, taskTitle: String) {
    ensureChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("¡Pomodoro Terminado!")
      .setContentText("Concluiste tu bloque para: $taskTitle")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_POMODORO, notification)
  }
}
