package luzzr.ji.domain.repository

import kotlinx.coroutines.flow.Flow
import luzzr.ji.domain.model.Budget

interface BudgetRepository {
    fun observeBudget(yearMonth: String): Flow<Budget?>
    fun observeAllBudgets(): Flow<List<Budget>>
    suspend fun getBudget(yearMonth: String): Budget?
    suspend fun getAllBudgets(): List<Budget>
    suspend fun saveBudget(budget: Budget)
}
