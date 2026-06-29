package luzzr.ji.domain.usecase

import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.repository.TransactionRepository

class MigrateTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction, toExtra: Boolean): Result<Unit> {
        val migrated = transaction.copy(isExtra = toExtra)
        return runCatching {
            transactionRepository.updateTransaction(migrated)
        }
    }
}
