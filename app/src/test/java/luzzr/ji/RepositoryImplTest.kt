package luzzr.ji

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import luzzr.ji.core.database.*
import luzzr.ji.data.repositoryImpl.BudgetRepositoryImpl
import luzzr.ji.data.repositoryImpl.TransactionRepositoryImpl
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform

class RepositoryImplTest {

    private val sampleEntity = TransactionEntity(
        id = 1L,
        amount = 10000L,
        type = "EXPENSE",
        category = "Food",
        note = "lunch",
        timestamp = 123456789L,
        isExtra = false,
        source = "MANUAL"
    )

    private val sampleBudgetEntity = BudgetEntity(
        yearMonth = "2026-06",
        amount = 300000L
    )

    private class FakeTxDao : TransactionDao {
        var observedList = listOf<TransactionEntity>()
        var insertedEntity: TransactionEntity? = null
        var updatedEntity: TransactionEntity? = null
        var deletedEntity: TransactionEntity? = null
        var queriedEntity: TransactionEntity? = null

        override fun observeAllTransactions(): Flow<List<TransactionEntity>> = flowOf(observedList)
        override suspend fun getTransactionById(id: Long): TransactionEntity? = queriedEntity
        override suspend fun getTransactionByDedupKey(dedupKey: String): TransactionEntity? = queriedEntity?.takeIf { it.dedupKey == dedupKey }
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            insertedEntity = transaction
            return 88L
        }
        override suspend fun insertAutoTransaction(transaction: TransactionEntity): Long {
            insertedEntity = transaction
            return 89L
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {
            updatedEntity = transaction
        }
        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            deletedEntity = transaction
        }
    }

    private class FakeBudgetDao : BudgetDao {
        var observedBudget: BudgetEntity? = null
        var observedAll = listOf<BudgetEntity>()
        var queriedBudget: BudgetEntity? = null
        var budgetsList = listOf<BudgetEntity>()
        var insertedBudget: BudgetEntity? = null

        override fun observeBudget(yearMonth: String): Flow<BudgetEntity?> = flowOf(observedBudget)
        override fun observeAllBudgets(): Flow<List<BudgetEntity>> = flowOf(observedAll)
        override suspend fun getBudget(yearMonth: String): BudgetEntity? = queriedBudget
        override suspend fun getAllBudgets(): List<BudgetEntity> = budgetsList
        override suspend fun insertOrUpdateBudget(budget: BudgetEntity) {
            insertedBudget = budget
        }
    }

    @Test
    fun testTransactionRepositoryImpl() = runTest {
        val dao = FakeTxDao()
        val repository = TransactionRepositoryImpl(dao)

        // 1. 测试 observeAllTransactions
        dao.observedList = listOf(sampleEntity)
        val resultList = repository.observeAllTransactions().first()
        assertEquals(1, resultList.size)
        val domainTx = resultList[0]
        assertEquals(sampleEntity.id, domainTx.id)
        assertEquals(sampleEntity.amount, domainTx.amount)
        assertEquals(TransactionType.EXPENSE, domainTx.type)
        assertEquals(sampleEntity.category, domainTx.category)
        assertEquals(sampleEntity.note, domainTx.note)
        assertEquals(sampleEntity.timestamp, domainTx.timestamp)
        assertEquals(sampleEntity.isExtra, domainTx.isExtra)

        // 1.1 自动支付元数据应完整保留
        dao.observedList = listOf(sampleEntity.copy(
            platform = "WECHAT",
            paymentKind = "TRANSFER",
            tradeId = "wx-123",
            occurredAt = 223456789L,
            dedupKey = "WECHAT:wx-123"
        ))
        val autoTx = repository.observeAllTransactions().first().single()
        assertEquals(PaymentPlatform.WECHAT, autoTx.platform)
        assertEquals(PaymentKind.TRANSFER, autoTx.paymentKind)
        assertEquals("wx-123", autoTx.tradeId)
        assertEquals(223456789L, autoTx.occurredAt)

        // 2. 测试 insert/saveTransaction
        val saveResult = repository.saveTransaction(
            Transaction(
                id = 1L,
                amount = 10000L,
                type = TransactionType.INCOME, // 测试 INCOME 映射
                category = "Food",
                note = "lunch",
                timestamp = 123456789L,
                isExtra = false,
                source = "MANUAL"
            )
        )
        assertEquals(88L, saveResult)
        assertEquals("INCOME", dao.insertedEntity?.type)

        // 3. 测试 getTransactionById
        dao.queriedEntity = sampleEntity
        val queried = repository.getTransactionById(1L)
        assertEquals(1L, queried?.id)

        dao.queriedEntity = null
        assertNull(repository.getTransactionById(2L))

        // 4. 测试 delete
        repository.deleteTransaction(
            Transaction(
                id = 1L,
                amount = 10000L,
                type = TransactionType.EXPENSE,
                category = "Food",
                note = "lunch",
                timestamp = 123456789L,
                isExtra = false,
                source = "MANUAL"
            )
        )
        assertEquals(1L, dao.deletedEntity?.id)
    }

    @Test
    fun testBudgetRepositoryImpl() = runTest {
        val dao = FakeBudgetDao()
        val repository = BudgetRepositoryImpl(dao)

        // 1. 测试 observeBudget
        dao.observedBudget = sampleBudgetEntity
        val budgetFlowVal = repository.observeBudget("2026-06").first()
        assertEquals("2026-06", budgetFlowVal?.yearMonth)
        assertEquals(300000L, budgetFlowVal?.amount ?: 0L)

        dao.observedBudget = null
        assertNull(repository.observeBudget("2026-07").first())

        // 2. 测试 observeAllBudgets
        dao.observedAll = listOf(sampleBudgetEntity)
        assertEquals(1, repository.observeAllBudgets().first().size)

        // 3. 测试 getBudget
        dao.queriedBudget = sampleBudgetEntity
        assertEquals(300000L, repository.getBudget("2026-06")?.amount ?: 0L)

        // 4. 测试 getAllBudgets
        dao.budgetsList = listOf(sampleBudgetEntity)
        assertEquals(1, repository.getAllBudgets().size)

        // 5. 测试 saveBudget
        repository.saveBudget(Budget("2026-06", 400000L))
        assertEquals(400000L, dao.insertedBudget?.amount ?: 0L)
    }
}
