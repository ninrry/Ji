package luzzr.ji.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognitionRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: RecognitionRecordEntity): Long

    @Query("SELECT * FROM recognition_records WHERE id = :id")
    suspend fun getById(id: String): RecognitionRecordEntity?

    @Query("UPDATE recognition_records SET status = 'PROCESSING', attemptCount = attemptCount + 1, errorMessage = NULL WHERE id = :id AND status IN ('PENDING', 'RETRY')")
    suspend fun claim(id: String): Int

    @Query("UPDATE recognition_records SET status = 'RETRY', errorMessage = :message WHERE id = :id")
    suspend fun markRetry(id: String, message: String)

    @Query("UPDATE recognition_records SET status = 'FAILED', errorMessage = :message WHERE id = :id")
    suspend fun markFailed(id: String, message: String)

    @Query("UPDATE recognition_records SET status = 'IGNORED', errorMessage = NULL WHERE id = :id")
    suspend fun markIgnored(id: String)

    @Query("UPDATE recognition_records SET status = 'COMPLETED', transactionId = :transactionId, errorMessage = NULL WHERE id = :id")
    suspend fun markCompleted(id: String, transactionId: Long)

    @Query("SELECT * FROM recognition_records WHERE status = 'FAILED' ORDER BY capturedAt DESC LIMIT :limit")
    fun observeRecentFailures(limit: Int = 10): Flow<List<RecognitionRecordEntity>>

    @Query("""
        SELECT r.* FROM recognition_records r
        WHERE r.status = 'FAILED'
          AND NOT EXISTS (
            SELECT 1 FROM transactions t
            WHERE t.source = 'AUTO_VLM'
              AND t.platform = r.platform
              AND t.paymentKind = r.kindHint
              AND t.occurredAt BETWEEN r.capturedAt - 60000 AND r.capturedAt + 60000
          )
        ORDER BY r.capturedAt DESC
        LIMIT 1
    """)
    fun observeLatestActionableFailure(): Flow<RecognitionRecordEntity?>

    @Query("DELETE FROM recognition_records WHERE status IN ('COMPLETED', 'FAILED', 'IGNORED') AND capturedAt < :before")
    suspend fun deleteProcessedBefore(before: Long)
}
