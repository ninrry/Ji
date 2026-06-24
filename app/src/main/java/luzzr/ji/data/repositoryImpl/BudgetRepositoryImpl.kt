package luzzr.ji.data.repositoryImpl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import luzzr.ji.core.database.BudgetDao
import luzzr.ji.core.database.BudgetEntity
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.repository.BudgetRepository

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeBudget(yearMonth: String): Flow<Budget?> {
        return budgetDao.observeBudget(yearMonth).map { entity ->
            entity?.toDomain()
        }
    }

    override fun observeAllBudgets(): Flow<List<Budget>> {
        return budgetDao.observeAllBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBudget(yearMonth: String): Budget? {
        return budgetDao.getBudget(yearMonth)?.toDomain()
    }

    override suspend fun getAllBudgets(): List<Budget> {
        return budgetDao.getAllBudgets().map { it.toDomain() }
    }

    override suspend fun saveBudget(budget: Budget) {
        budgetDao.insertOrUpdateBudget(budget.toEntity())
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            yearMonth = yearMonth,
            amount = amount
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            yearMonth = yearMonth,
            amount = amount
        )
    }
}
