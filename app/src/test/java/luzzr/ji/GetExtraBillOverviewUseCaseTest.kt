package luzzr.ji

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.repository.BudgetRepository
import luzzr.ji.domain.repository.TransactionRepository
import luzzr.ji.domain.usecase.GetExtraBillOverviewUseCase
import java.time.LocalDate
import java.time.ZoneId

class GetExtraBillOverviewUseCaseTest {

    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun testCalculateOverview() = runTest {
        // 假设当前是 2026-06-23 (时间戳为 1782230400000 左右)
        // 2026-06-23 对应毫秒约为 1782211200000 (上海时区 2026-06-23 00:00:00)
        val currentMillis = 1782211200000L 

        // 构造历史月份 2026-05 的普通账单：12 笔消费发生在 12 个去重日期上，每笔消费 10.0 元，共 120.0 元
        val mayTransactions = (1..12).map { day ->
            val timestamp = LocalDate.of(2026, 5, day)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            Transaction(
                id = day.toLong(),
                amount = 1000L,
                type = TransactionType.EXPENSE,
                category = "Food",
                note = "day $day",
                timestamp = timestamp,
                isExtra = false
            )
        }

        // 构造历史月份 2026-04 的普通账单：5 笔消费在 5 个去重日期上，每笔 50.0 元，共 250.0 元 (不满10日，结余不计入)
        val aprilTransactions = (1..5).map { day ->
            val timestamp = LocalDate.of(2026, 4, day)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            Transaction(
                id = (100 + day).toLong(),
                amount = 5000L,
                type = TransactionType.EXPENSE,
                category = "Transit",
                note = "day $day",
                timestamp = timestamp,
                isExtra = false
            )
        }

        // 构造当月 2026-06 的消费，不参与历史结余池计算
        val juneTransaction = Transaction(
            id = 200L,
            amount = 10000L,
            type = TransactionType.EXPENSE,
            category = "Rent",
            note = "june",
            timestamp = currentMillis,
            isExtra = false
        )

        // 构造额外账单已用消费记录
        val extraTransaction = Transaction(
            id = 300L,
            amount = 5000L,
            type = TransactionType.EXPENSE,
            category = "Gift",
            note = "extra spend",
            timestamp = currentMillis,
            isExtra = true
        )

        val transactionsList = mayTransactions + aprilTransactions + listOf(juneTransaction, extraTransaction)

        // 预算列表 (2026-05 月预算 3000.0 元，2026-04 月预算 2000.0 元)
        val budgetsList = listOf(
            Budget("2026-05", 300000L),
            Budget("2026-04", 200000L)
        )

        val fakeTxRepo = object : TransactionRepository {
            override fun observeAllTransactions(): Flow<List<Transaction>> = flowOf(transactionsList)
            override suspend fun getTransactionById(id: Long): Transaction? = null
            override suspend fun saveTransaction(transaction: Transaction): Long = 0
            override suspend fun updateTransaction(transaction: Transaction) {}
            override suspend fun deleteTransaction(transaction: Transaction) {}
        }

        val fakeBudgetRepo = object : BudgetRepository {
            override fun observeBudget(yearMonth: String): Flow<Budget?> = flowOf(null)
            override fun observeAllBudgets(): Flow<List<Budget>> = flowOf(budgetsList)
            override suspend fun getBudget(yearMonth: String): Budget? = null
            override suspend fun getAllBudgets(): List<Budget> = budgetsList
            override suspend fun saveBudget(budget: Budget) {}
        }

        val useCase = GetExtraBillOverviewUseCase(fakeTxRepo, fakeBudgetRepo, zoneId)
        val overview = useCase(currentMillis, defaultMonthlyBudget = 300000L).first()

        // 2026-04 消费天数 5 < 10：结余贡献为 0.0 元
        // 2026-05 消费天数 12 >= 10：结余 = 预算 3000.0 - 消费 120.0 = 2880.0 元
        // 历史总省下池为 2880.0 元
        assertEquals(288000L, overview.totalPoolAmount)

        // 额外账单总支出为 50.0 元
        assertEquals(5000L, overview.extraSpendAmount)

        // 可用额外额度 = 2880.0 - 50.0 = 2830.0 元
        assertEquals(283000L, overview.remainingPoolAmount)
    }

    @Test
    fun testEmptyTransactionsOverview() = runTest {
        val fakeTxRepo = object : TransactionRepository {
            override fun observeAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
            override suspend fun getTransactionById(id: Long): Transaction? = null
            override suspend fun saveTransaction(transaction: Transaction): Long = 0
            override suspend fun updateTransaction(transaction: Transaction) {}
            override suspend fun deleteTransaction(transaction: Transaction) {}
        }
        val fakeBudgetRepo = object : BudgetRepository {
            override fun observeBudget(yearMonth: String): Flow<Budget?> = flowOf(null)
            override fun observeAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
            override suspend fun getBudget(yearMonth: String): Budget? = null
            override suspend fun getAllBudgets(): List<Budget> = emptyList()
            override suspend fun saveBudget(budget: Budget) {}
        }
        val useCase = GetExtraBillOverviewUseCase(fakeTxRepo, fakeBudgetRepo, zoneId)
        val overview = useCase(System.currentTimeMillis()).first()

        assertEquals(0L, overview.totalPoolAmount)
        assertEquals(0L, overview.extraSpendAmount)
        assertEquals(0L, overview.remainingPoolAmount)
    }
}
