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
import luzzr.ji.core.vlm.LocalFallbackRuleEngine
import luzzr.ji.core.vlm.VlmClient
import luzzr.ji.core.vlm.VlmTransactionResult
import luzzr.ji.domain.model.TransactionType
import java.io.File
import java.util.concurrent.TimeUnit

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
        private const val MIN_DEDUP_WINDOW_MS = 1_000L
        private const val FALLBACK_TRANSACTION_DEDUP_WINDOW_MS = 15 * 60_000L

        internal fun fallbackTransactionDedupKey(
            result: VlmTransactionResult,
            eventFingerprint: String,
            capturedAt: Long
        ): String {
            val occurredAt = result.completedAt ?: capturedAt
            val noteKey = normalizedNoteKey(result.note)
            val identity = if (noteKey.isBlank()) eventFingerprint else noteKey
            return "${result.platform.name}:${result.paymentKind.name}:no-trade:${result.amount}:$identity:${occurredAt / FALLBACK_TRANSACTION_DEDUP_WINDOW_MS}"
        }

        private fun normalizedNoteKey(note: String): String =
            note.trim().replace(Regex("\\s+"), "").take(64)
    }

    private val appContext = context.applicationContext
    private val recordDao = database.recognitionRecordDao()
    private val screenshotStore = EncryptedScreenshotStore(File(appContext.filesDir, "recognition"))

    fun observeLatestFailure(): Flow<String?> = recordDao.observeLatestActionableFailure()
        .map { record -> record?.errorMessage }

    suspend fun enqueue(candidate: PaymentCandidate) = withContext(Dispatchers.IO) {
        // Reserve the semantic identity in Room. This protects against callbacks with different
        // node subsets and also survives an accessibility-service process restart.
        val dedupWindowMs = candidate.dedupWindowMs.coerceAtLeast(MIN_DEDUP_WINDOW_MS)
        val id = "${candidate.eventFingerprint}-${candidate.capturedAt}"
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
        val inserted = database.withTransaction {
            if (recordDao.hasRecordWithFingerprintBetween(
                    fingerprint = candidate.eventFingerprint,
                    fromCapturedAt = candidate.capturedAt - dedupWindowMs,
                    toCapturedAt = candidate.capturedAt + dedupWindowMs
                )
            ) {
                false
            } else {
                recordDao.insert(record) != -1L
            }
        }
        if (!inserted) {
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
        if (recordDao.claim(recordId) == 0) return@withContext RecognitionProcessResult.Ignored
        val record = recordDao.getById(recordId) ?: return@withContext RecognitionProcessResult.Failed("识别任务不存在")
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isBlank()) return@withContext fail(record, "请先在设置中保存云端识别密钥")

        try {
            val platform = luzzr.ji.domain.model.PaymentPlatform.valueOf(record.platform)
            val kind = luzzr.ji.domain.model.PaymentKind.valueOf(record.kindHint)
            if (!PaymentCompletionClassifier.isStillEligible(platform, kind, record.screenText)) {
                return@withContext ignore(record)
            }
            val image = record.screenshotPath?.let(screenshotStore::read)
            val model = sharedPreferences.getString("opencode_model_id", "mimo-v2.5") ?: "mimo-v2.5"
            val apiUrl = sharedPreferences.getString(VlmClient.PREF_API_URL, VlmClient.DEFAULT_API_URL)
                ?: VlmClient.DEFAULT_API_URL
            val result = VlmClient(
                apiKey = apiKey,
                modelId = model,
                fallbackRuleEngine = LocalFallbackRuleEngine.from(appContext, sharedPreferences),
                apiUrl = apiUrl
            ).parsePayment(record.screenText, image, platform, kind)
                ?: return@withContext fail(record, "未识别到可自动入账的支付完成页")
            val transactionId = completeAtomically(record, result)
            deleteScreenshot(record.screenshotPath)
            recordDao.deleteProcessedBefore(System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L)
            notifier.showRecorded(result)
            RecognitionProcessResult.Completed(result.copy(completedAt = result.completedAt ?: record.capturedAt))
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
        notifier.showFailed(message)
    }

    private suspend fun completeAtomically(record: RecognitionRecordEntity, result: VlmTransactionResult): Long {
        val dedupKey = result.tradeId?.let { "${result.platform.name}:$it" }
            ?: fallbackTransactionDedupKey(result, record.eventFingerprint, record.capturedAt)
        return database.withTransaction {
            val occurredAt = result.completedAt ?: record.capturedAt
            if (result.tradeId.isNullOrBlank()) {
                val existing = database.transactionDao().findAutoDuplicateWithoutTradeId(
                    platform = result.platform.name,
                    paymentKind = result.paymentKind.name,
                    amount = result.amount,
                    noteKey = normalizedNoteKey(result.note),
                    fromOccurredAt = occurredAt - FALLBACK_TRANSACTION_DEDUP_WINDOW_MS,
                    toOccurredAt = occurredAt + FALLBACK_TRANSACTION_DEDUP_WINDOW_MS,
                    occurredAt = occurredAt
                )
                if (existing != null) {
                    recordDao.markCompleted(record.id, existing.id)
                    return@withTransaction existing.id
                }
            }
            val transaction = TransactionEntity(
                amount = result.amount,
                type = TransactionType.EXPENSE.name,
                category = result.category,
                note = result.note,
                timestamp = occurredAt,
                isExtra = false,
                source = "AUTO_VLM",
                platform = result.platform.name,
                paymentKind = result.paymentKind.name,
                tradeId = result.tradeId,
                occurredAt = occurredAt,
                dedupKey = dedupKey
            )
            val inserted = database.transactionDao().insertAutoTransaction(transaction)
            val transactionId = if (inserted > 0L) {
                inserted
            } else {
                database.transactionDao().getTransactionByDedupKey(dedupKey)?.id
                    ?: error("自动账单写入冲突，但未找到已有记录")
            }
            recordDao.markCompleted(record.id, transactionId)
            transactionId
        }
    }

    private suspend fun fail(record: RecognitionRecordEntity, message: String): RecognitionProcessResult.Failed {
        recordDao.markFailed(record.id, message)
        deleteScreenshot(record.screenshotPath)
        notifier.showFailed(message)
        return RecognitionProcessResult.Failed(message)
    }

    private suspend fun ignore(record: RecognitionRecordEntity): RecognitionProcessResult.Ignored {
        recordDao.markIgnored(record.id)
        deleteScreenshot(record.screenshotPath)
        return RecognitionProcessResult.Ignored
    }

    private fun saveScreenshot(id: String, bytes: ByteArray): String {
        return screenshotStore.save(id, bytes)
    }

    private fun deleteScreenshot(path: String?) {
        path?.let(::File)?.delete()
    }
}
