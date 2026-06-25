package luzzr.ji.core.permissions

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import luzzr.ji.MainActivity
import luzzr.ji.R

/**
 * User-visible process retention for automatic bookkeeping.
 *
 * It does not capture screens or payment data. The separately user-enabled accessibility service
 * performs recognition. START_STICKY covers ordinary process reclamation and the boot receiver
 * covers device restarts.
 */
class AutoRecordKeepAliveService : Service() {
    companion object {
        private const val TAG = "AutoRecordKeepAlive"
        private const val CHANNEL_ID = "auto_record_keep_alive"
        private const val NOTIFICATION_ID = 2001
        private const val CHECK_INTERVAL_MS = 60_000L

        fun startIfAccessibilityEnabled(context: Context, scheduleWatchdog: Boolean = true) {
            val appContext = context.applicationContext
            if (!PermissionManager.isAccessibilityServiceEnabled(appContext)) {
                AutoRecordAccessibilityWatchdog.stop(appContext)
                return
            }
            if (scheduleWatchdog) AutoRecordAccessibilityWatchdog.start(appContext)
            val intent = Intent(appContext, AutoRecordKeepAliveService::class.java)
            runCatching {
                ContextCompat.startForegroundService(appContext, intent)
            }.onFailure { error ->
                // Android can reject a background foreground-service launch in exceptional cases.
                // The enabled accessibility service and watchdog queue remain recoverable.
                Log.w(TAG, "Unable to start automatic bookkeeping foreground service", error)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val verifyAccessibilityEnabled = object : Runnable {
        override fun run() {
            if (!PermissionManager.isAccessibilityServiceEnabled(this@AutoRecordKeepAliveService)) {
                AutoRecordAccessibilityWatchdog.stop(this@AutoRecordKeepAliveService)
                stopSelf()
                return
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showForegroundNotification()
        handler.post(verifyAccessibilityEnabled)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForegroundNotification()
        AutoRecordAccessibilityWatchdog.start(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (PermissionManager.isAccessibilityServiceEnabled(this)) {
            AutoRecordAccessibilityWatchdog.start(this)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keep_alive_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.keep_alive_channel_description)
                setShowBadge(false)
            }
        )
    }

    private fun showForegroundNotification() {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(getString(R.string.keep_alive_notification_title))
            .setContentText(getString(R.string.keep_alive_notification_text))
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
