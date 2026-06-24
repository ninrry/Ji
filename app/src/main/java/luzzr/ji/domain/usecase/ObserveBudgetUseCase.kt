package luzzr.ji.domain.usecase

import kotlinx.coroutines.flow.Flow
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.repository.BudgetRepository

class ObserveBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(yearMonth: String): Flow<Budget?> {
        return budgetRepository.observeBudget(yearMonth)
    }
}
