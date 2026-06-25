package luzzr.ji.core.payment

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import luzzr.ji.MainActivity
import luzzr.ji.core.vlm.VlmTransactionResult
import java.util.concurrent.atomic.AtomicInteger

class AutoRecordNotifier(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "auto_record"
        private const val CHANNEL_NAME = "自动记账"
        private val nextNotificationId = AtomicInteger(3000)
    }

    fun showRecorded(result: VlmTransactionResult) {
        show(
            title = "已自动记账",
            text = "¥${"%.2f".format(result.amount / 100.0)} · ${result.category} · ${result.note}",
            icon = android.R.drawable.ic_menu_save
        )
    }

    fun showFailed(message: String) {
        show(
            title = "自动记账失败",
            text = message.take(96),
            icon = android.R.drawable.ic_dialog_alert
        )
    }

    private fun show(title: String, text: String, icon: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(nextNotificationId.getAndIncrement(), notification)
    }
}
