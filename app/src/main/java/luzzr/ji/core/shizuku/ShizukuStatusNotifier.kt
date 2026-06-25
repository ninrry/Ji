package luzzr.ji.core.shizuku

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import luzzr.ji.MainActivity
import luzzr.ji.R

object ShizukuStatusNotifier {
    private const val CHANNEL_ID = "shizuku_status"
    private const val NOTIFICATION_ID = 4108

    fun notifyNeedsActivation(context: Context) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.shizuku_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = appContext.getString(R.string.shizuku_channel_description)
                setShowBadge(true)
            }
        )
        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(appContext.getString(R.string.shizuku_notification_title))
            .setContentText(appContext.getString(R.string.shizuku_notification_text))
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.applicationContext
            .getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
    }
}
