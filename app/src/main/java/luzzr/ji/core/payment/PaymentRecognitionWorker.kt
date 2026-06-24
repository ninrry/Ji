package luzzr.ji.core.payment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import luzzr.ji.JiApplication

class PaymentRecognitionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    companion object {
        const val RECORD_ID = "record_id"
        private const val MAX_ATTEMPTS = 3
        private val processingMutex = Mutex()
    }

    override suspend fun doWork(): Result {
        val recordId = inputData.getString(RECORD_ID) ?: return Result.failure()
        val manager = (applicationContext as JiApplication).container.paymentRecognitionManager
        return processingMutex.withLock {
            when (val process = manager.process(recordId)) {
                is RecognitionProcessResult.Completed,
                is RecognitionProcessResult.Failed -> Result.success()
                is RecognitionProcessResult.Retry -> {
                    if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                        manager.failAfterRetries(recordId, process.message)
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
            }
        }
    }
}
