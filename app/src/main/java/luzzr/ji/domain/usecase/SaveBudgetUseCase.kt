package luzzr.ji.domain.usecase

import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.repository.BudgetRepository

class SaveBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Result<Unit> {
        if (budget.amount < 0) {
            return Result.failure(IllegalArgumentException("预算金额不能为负数"))
        }
        return runCatching {
            budgetRepository.saveBudget(budget)
        }
    }
}
