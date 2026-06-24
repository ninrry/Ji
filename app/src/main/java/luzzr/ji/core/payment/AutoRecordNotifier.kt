package luzzr.ji.core.payment

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import luzzr.ji.core.vlm.VlmTransactionResult

class AutoRecordNotifier(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "auto_record"
        private const val CHANNEL_NAME = "自动记账"
    }

    fun showRecorded(result: VlmTransactionResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("已自动记账")
            .setContentText("¥${"%.2f".format(result.amount / 100.0)} · ${result.category} · ${result.note}")
            .setAutoCancel(true)
            .build()
        manager.notify((result.tradeId ?: "${result.platform}-${System.currentTimeMillis()}").hashCode(), notification)
    }
}
