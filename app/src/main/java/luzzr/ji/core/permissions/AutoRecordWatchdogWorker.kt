package luzzr.ji.core.permissions

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoRecordWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        AutoRecordAccessibilityWatchdog.keepAliveIfEnabled(applicationContext, scheduleWatchdog = false)
        return Result.success()
    }
}
