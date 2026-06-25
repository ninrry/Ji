package luzzr.ji.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE dedupKey = :dedupKey LIMIT 1")
    suspend fun getTransactionByDedupKey(dedupKey: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE source = 'AUTO_VLM'
          AND tradeId IS NULL
          AND platform = :platform
          AND paymentKind = :paymentKind
          AND amount = :amount
          AND REPLACE(TRIM(note), ' ', '') = :noteKey
          AND occurredAt BETWEEN :fromOccurredAt AND :toOccurredAt
        ORDER BY ABS(occurredAt - :occurredAt) ASC
        LIMIT 1
    """)
    suspend fun findAutoDuplicateWithoutTradeId(
        platform: String,
        paymentKind: String,
        amount: Long,
        noteKey: String,
        fromOccurredAt: Long,
        toOccurredAt: Long,
        occurredAt: Long
    ): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAutoTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
