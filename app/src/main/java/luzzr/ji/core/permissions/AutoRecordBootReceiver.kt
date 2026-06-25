package luzzr.ji.core.permissions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import luzzr.ji.core.shizuku.ShizukuScreenshotGateway
import luzzr.ji.core.shizuku.ShizukuStatusNotifier

/** Restarts the user-enabled automatic bookkeeping retention service after boot or app update. */
class AutoRecordBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (AutoRecordAccessibilityWatchdog.keepAliveIfEnabled(context)) {
                    if (ShizukuScreenshotGateway.status() == ShizukuScreenshotGateway.Status.AUTHORIZED) {
                        ShizukuStatusNotifier.cancel(context)
                    } else {
                        ShizukuStatusNotifier.notifyNeedsActivation(context)
                    }
                }
            }
        }
    }
}
