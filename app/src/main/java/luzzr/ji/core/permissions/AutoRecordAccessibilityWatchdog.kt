package luzzr.ji.core.permissions

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Periodic liveness guard for the user-enabled accessibility recognizer.
 *
 * Android does not allow apps to silently grant or re-enable accessibility permissions. This
 * watchdog only runs after the user has enabled the service, and it cancels itself as soon as the
 * permission is gone.
 */
object AutoRecordAccessibilityWatchdog {
    private const val UNIQUE_WORK_NAME = "auto-record-accessibility-watchdog"
    private const val WATCHDOG_INTERVAL_MINUTES = 15L

    fun keepAliveIfEnabled(context: Context, scheduleWatchdog: Boolean = true): Boolean {
        val appContext = context.applicationContext
        if (!PermissionManager.isAccessibilityServiceEnabled(appContext)) {
            stop(appContext)
            return false
        }
        if (scheduleWatchdog) start(appContext)
        AutoRecordKeepAliveService.startIfAccessibilityEnabled(appContext, scheduleWatchdog = false)
        return true
    }

    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoRecordWatchdogWorker>(
            WATCHDOG_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        context.applicationContext.stopService(
            android.content.Intent(context.applicationContext, AutoRecordKeepAliveService::class.java)
        )
    }
}
