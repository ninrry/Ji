package luzzr.ji.domain.usecase

import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.repository.TransactionRepository

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.amount <= 0) {
            return Result.failure(IllegalArgumentException("金额必须大于0"))
        }
        if (transaction.category.isBlank()) {
            return Result.failure(IllegalArgumentException("分类不能为空"))
        }
        return runCatching {
            transactionRepository.saveTransaction(transaction)
        }
    }
}
