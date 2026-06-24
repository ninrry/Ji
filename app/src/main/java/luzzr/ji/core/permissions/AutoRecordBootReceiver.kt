package luzzr.ji.core.permissions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the user-enabled automatic bookkeeping retention service after boot or app update. */
class AutoRecordBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> AutoRecordKeepAliveService.startIfAccessibilityEnabled(context)
        }
    }
}
