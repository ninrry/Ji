package luzzr.ji.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeBudget(yearMonth: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getBudget(yearMonth: String): BudgetEntity?

    @Query("SELECT * FROM budgets")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)
}
