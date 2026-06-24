package luzzr.ji.core.payment

import android.content.Context
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import luzzr.ji.core.common.SecureStorage
import luzzr.ji.core.database.AppDatabase
import luzzr.ji.core.database.RecognitionRecordEntity
import luzzr.ji.core.database.TransactionEntity
import luzzr.ji.core.vlm.VlmClient
import luzzr.ji.core.vlm.VlmTransactionResult
import luzzr.ji.domain.model.TransactionType
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.Locale

sealed interface RecognitionProcessResult {
    data class Completed(val result: VlmTransactionResult) : RecognitionProcessResult
    data class Failed(val message: String) : RecognitionProcessResult
    data class Retry(val message: String) : RecognitionProcessResult
    data object Ignored : RecognitionProcessResult
}

class PaymentRecognitionManager(
    context: Context,
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val sharedPreferences: android.content.SharedPreferences,
    private val notifier: AutoRecordNotifier = AutoRecordNotifier(context.applicationContext)
) {
    companion object {
        private const val CANDIDATE_DEDUP_WINDOW_MS = 5 * 60_000L

        internal fun fallbackTransactionDedupKey(result: VlmTransactionResult, capturedAt: Long): String {
            val normalizedNote = result.note
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(100)
            return "${result.platform.name}:${result.paymentKind.name}:${result.amount}:$normalizedNote:${capturedAt / CANDIDATE_DEDUP_WINDOW_MS}"
        }
    }

    private val appContext = context.applicationContext
    private val recordDao = database.recognitionRecordDao()

    fun observeLatestFailure(): Flow<String?> = recordDao.observeLatestActionableFailure()
        .map { record -> record?.errorMessage }

    suspend fun enqueue(candidate: PaymentCandidate) = withContext(Dispatchers.IO) {
        // The primary key makes duplicate accessibility callbacks in the same completion-page
        // window a no-op even after the service process is recreated.
        val id = "${candidate.eventFingerprint}-${candidate.capturedAt / CANDIDATE_DEDUP_WINDOW_MS}"
        val screenshotPath = candidate.screenshotBytes?.let { saveScreenshot(id, it) }
        val record = RecognitionRecordEntity(
            id = id,
            eventFingerprint = candidate.eventFingerprint,
            platform = candidate.platform.name,
            kindHint = candidate.kindHint.name,
            screenText = PaymentFingerprint.normalizedText(candidate.screenText),
            screenshotPath = screenshotPath,
            capturedAt = candidate.capturedAt
        )
        if (recordDao.insert(record) == -1L) {
            deleteScreenshot(screenshotPath)
            return@withContext
        }
        val work = OneTimeWorkRequestBuilder<PaymentRecognitionWorker>()
            .setInputData(Data.Builder().putString(PaymentRecognitionWorker.RECORD_ID, id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "payment-recognition-$id",
            ExistingWorkPolicy.KEEP,
            work
        )
    }

    suspend fun process(recordId: String): RecognitionProcessResult = withContext(Dispatchers.IO) {
        if (recordDao.claim(recordId) == 0) return@withContext RecognitionProcessResult.Failed("任务已被处理")
        val record = recordDao.getById(recordId) ?: return@withContext RecognitionProcessResult.Failed("识别任务不存在")
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isBlank()) return@withContext fail(record, "未配置云端 VLM API 密钥")

        try {
            val platform = luzzr.ji.domain.model.PaymentPlatform.valueOf(record.platform)
            val kind = luzzr.ji.domain.model.PaymentKind.valueOf(record.kindHint)
            if (!PaymentCompletionClassifier.isStillEligible(platform, kind, record.screenText)) {
                return@withContext ignore(record)
            }
            val image = record.screenshotPath?.let { path -> File(path).takeIf(File::exists)?.readBytes() }
            val model = sharedPreferences.getString("opencode_model_id", "mimo-v2.5") ?: "mimo-v2.5"
            val result = VlmClient(apiKey, model).parsePayment(record.screenText, image, platform, kind)
                ?: return@withContext fail(record, "云端结果不是可自动入账的已完成交易")
            val transactionId = completeAtomically(record, result)
            deleteScreenshot(record.screenshotPath)
            recordDao.deleteProcessedBefore(System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L)
            notifier.showRecorded(result)
            RecognitionProcessResult.Completed(result.copy(completedAt = record.capturedAt))
        } catch (error: Exception) {
            recordDao.markRetry(record.id, error.message ?: "云端识别网络异常")
            RecognitionProcessResult.Retry(error.message ?: "云端识别网络异常")
        }
    }

    suspend fun failAfterRetries(recordId: String, message: String) = withContext(Dispatchers.IO) {
        val record = recordDao.getById(recordId) ?: return@withContext
        recordDao.markFailed(recordId, message)
        deleteScreenshot(record.screenshotPath)
        recordDao.deleteProcessedBefore(System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L)
    }

    private suspend fun completeAtomically(record: RecognitionRecordEntity, result: VlmTransactionResult): Long {
        val dedupKey = result.tradeId?.let { "${result.platform.name}:$it" }
            ?: fallbackTransactionDedupKey(result, record.capturedAt)
        return database.withTransaction {
            val transaction = TransactionEntity(
                amount = result.amount,
                type = TransactionType.EXPENSE.name,
                category = result.category,
                note = result.note,
                timestamp = record.capturedAt,
                isExtra = false,
                source = "AUTO_VLM",
                platform = result.platform.name,
                paymentKind = result.paymentKind.name,
                tradeId = result.tradeId,
                occurredAt = record.capturedAt,
                dedupKey = dedupKey
            )
            val inserted = database.transactionDao().insertAutoTransaction(transaction)
            val transactionId = if (inserted > 0L) inserted else database.transactionDao().getTransactionByDedupKey(dedupKey)?.id ?: 0L
            recordDao.markCompleted(record.id, transactionId)
            transactionId
        }
    }

    private suspend fun fail(record: RecognitionRecordEntity, message: String): RecognitionProcessResult.Failed {
        recordDao.markFailed(record.id, message)
        deleteScreenshot(record.screenshotPath)
        return RecognitionProcessResult.Failed(message)
    }

    private suspend fun ignore(record: RecognitionRecordEntity): RecognitionProcessResult.Ignored {
        recordDao.markIgnored(record.id)
        deleteScreenshot(record.screenshotPath)
        return RecognitionProcessResult.Ignored
    }

    private fun saveScreenshot(id: String, bytes: ByteArray): String {
        val directory = File(appContext.filesDir, "recognition").apply { mkdirs() }
        return File(directory, "$id.jpg").apply { writeBytes(bytes) }.absolutePath
    }

    private fun deleteScreenshot(path: String?) {
        path?.let(::File)?.delete()
    }
}
