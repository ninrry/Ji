package luzzr.ji.data.repositoryImpl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import luzzr.ji.core.database.TransactionDao
import luzzr.ji.core.database.TransactionEntity
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import luzzr.ji.domain.repository.TransactionRepository

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun observeAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.observeAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun saveTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
            category = category,
            note = note,
            timestamp = timestamp,
            isExtra = isExtra,
            source = source,
            platform = runCatching { PaymentPlatform.valueOf(platform) }.getOrDefault(PaymentPlatform.MANUAL),
            paymentKind = runCatching { PaymentKind.valueOf(paymentKind) }.getOrDefault(PaymentKind.MERCHANT_PAYMENT),
            tradeId = tradeId,
            occurredAt = occurredAt,
            dedupKey = dedupKey
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type.name,
            category = category,
            note = note,
            timestamp = timestamp,
            isExtra = isExtra,
            source = source,
            platform = platform.name,
            paymentKind = paymentKind.name,
            tradeId = tradeId,
            occurredAt = occurredAt,
            dedupKey = dedupKey
        )
    }
}
