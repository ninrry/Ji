package luzzr.ji.domain.usecase

import kotlinx.coroutines.flow.Flow
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.repository.TransactionRepository

class ObserveTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.observeAllTransactions()
    }
}
