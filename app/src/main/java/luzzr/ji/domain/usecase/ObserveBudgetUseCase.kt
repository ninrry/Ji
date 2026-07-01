package luzzr.ji.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.repository.BudgetRepository
import java.time.YearMonth

class ObserveBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(yearMonth: String): Flow<Budget?> {
        val targetYearMonth = YearMonth.parse(yearMonth)
        return budgetRepository.observeAllBudgets().map { budgets ->
            budgets.effectiveBudgetFor(targetYearMonth)
        }
    }
}
