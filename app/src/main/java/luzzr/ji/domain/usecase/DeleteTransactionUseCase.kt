package luzzr.ji.domain.usecase

import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        return runCatching {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
