package luzzr.ji

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.repository.BudgetRepository
import luzzr.ji.domain.repository.TransactionRepository
import luzzr.ji.domain.usecase.*

class UseCaseTests {

    private val sampleTx = Transaction(
        id = 1L,
        amount = 10000L,
        type = TransactionType.EXPENSE,
        category = "Food",
        note = "lunch",
        timestamp = 123456789L,
        isExtra = false
    )

    private val sampleBudget = Budget(
        yearMonth = "2026-06",
        amount = 300000L
    )

    // 创建一个通用的 FakeRepository 协助测试
    private class FakeTxRepository : TransactionRepository {
        var savedTx: Transaction? = null
        var deletedTx: Transaction? = null

        override fun observeAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun saveTransaction(transaction: Transaction): Long {
            savedTx = transaction
            return 99L
        }
        override suspend fun updateTransaction(transaction: Transaction) {
            savedTx = transaction
        }
        override suspend fun deleteTransaction(transaction: Transaction) {
            deletedTx = transaction
        }
    }

    private class FakeBudgetRepository : BudgetRepository {
        var savedBudget: Budget? = null

        override fun observeBudget(yearMonth: String): Flow<Budget?> = flowOf(null)
        override fun observeAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun getBudget(yearMonth: String): Budget? = null
        override suspend fun getAllBudgets(): List<Budget> = emptyList()
        override suspend fun saveBudget(budget: Budget) {
            savedBudget = budget
        }
    }

    @Test
    fun testObserveTransactionsUseCase() = runTest {
        val list = listOf(sampleTx)
        val repo = object : TransactionRepository {
            override fun observeAllTransactions(): Flow<List<Transaction>> = flowOf(list)
            override suspend fun getTransactionById(id: Long): Transaction? = null
            override suspend fun saveTransaction(transaction: Transaction): Long = 0
            override suspend fun updateTransaction(transaction: Transaction) {}
            override suspend fun deleteTransaction(transaction: Transaction) {}
        }
        val useCase = ObserveTransactionsUseCase(repo)
        assertEquals(list, useCase().first())
    }

    @Test
    fun testCreateTransactionUseCase() = runTest {
        val repo = FakeTxRepository()
        val useCase = CreateTransactionUseCase(repo)

        // 1. 成功创建
        val result = useCase(sampleTx)
        assertTrue(result.isSuccess)
        assertEquals(99L, result.getOrNull())
        assertEquals(sampleTx, repo.savedTx)

        // 2. 金额小于等于0校验
        val invalidTx1 = sampleTx.copy(amount = -500L)
        val result1 = useCase(invalidTx1)
        assertTrue(result1.isFailure)
        assertEquals("金额必须大于0", result1.exceptionOrNull()?.message)

        // 3. 分类为空校验
        val invalidTx2 = sampleTx.copy(category = "  ")
        val result2 = useCase(invalidTx2)
        assertTrue(result2.isFailure)
        assertEquals("分类不能为空", result2.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateTransactionUseCase() = runTest {
        val repo = FakeTxRepository()
        val useCase = UpdateTransactionUseCase(repo)

        // 1. 成功更新
        val result = useCase(sampleTx)
        assertTrue(result.isSuccess)
        assertEquals(sampleTx, repo.savedTx)

        // 2. 校验负数金额
        val result1 = useCase(sampleTx.copy(amount = 0L))
        assertTrue(result1.isFailure)

        // 3. 校验空分类
        val result2 = useCase(sampleTx.copy(category = ""))
        assertTrue(result2.isFailure)
    }

    @Test
    fun testDeleteTransactionUseCase() = runTest {
        val repo = FakeTxRepository()
        val useCase = DeleteTransactionUseCase(repo)
        val result = useCase(sampleTx)
        assertTrue(result.isSuccess)
        assertEquals(sampleTx, repo.deletedTx)
    }

    @Test
    fun testMigrateTransactionUseCase() = runTest {
        val repo = FakeTxRepository()
        val useCase = MigrateTransactionUseCase(repo)
        
        // 普通迁移为额外
        val result = useCase(sampleTx, toExtra = true)
        assertTrue(result.isSuccess)
        assertTrue(repo.savedTx?.isExtra ?: false)
    }

    @Test
    fun testObserveBudgetUseCase() = runTest {
        val repo = object : BudgetRepository {
            override fun observeBudget(yearMonth: String): Flow<Budget?> = flowOf(sampleBudget)
            override fun observeAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
            override suspend fun getBudget(yearMonth: String): Budget? = null
            override suspend fun getAllBudgets(): List<Budget> = emptyList()
            override suspend fun saveBudget(budget: Budget) {}
        }
        val useCase = ObserveBudgetUseCase(repo)
        assertEquals(sampleBudget, useCase("2026-06").first())
    }

    @Test
    fun testSaveBudgetUseCase() = runTest {
        val repo = FakeBudgetRepository()
        val useCase = SaveBudgetUseCase(repo)

        // 成功保存
        val result = useCase(sampleBudget)
        assertTrue(result.isSuccess)
        assertEquals(sampleBudget, repo.savedBudget)

        // 异常预算金额
        val result1 = useCase(sampleBudget.copy(amount = -10000L))
        assertTrue(result1.isFailure)
        assertEquals("预算金额不能为负数", result1.exceptionOrNull()?.message)
    }
}
