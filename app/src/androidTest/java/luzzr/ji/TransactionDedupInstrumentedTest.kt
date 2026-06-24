package luzzr.ji

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import luzzr.ji.core.database.AppDatabase
import luzzr.ji.core.database.TransactionEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDedupInstrumentedTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun duplicateAutomaticPaymentUsesOneTransaction() = runBlocking {
        val transaction = TransactionEntity(
            amount = 1880,
            type = "EXPENSE",
            category = "餐饮",
            note = "便利店",
            timestamp = 1_700_000_000_000L,
            isExtra = false,
            source = "AUTO_VLM",
            platform = "WECHAT",
            paymentKind = "MERCHANT_PAYMENT",
            tradeId = "wx-order-1",
            occurredAt = 1_700_000_000_000L,
            dedupKey = "WECHAT:wx-order-1"
        )
        val first = database.transactionDao().insertAutoTransaction(transaction)
        val duplicate = database.transactionDao().insertAutoTransaction(transaction)

        assertTrue(first > 0)
        assertEquals(-1L, duplicate)
        assertEquals(1, database.transactionDao().observeAllTransactions().first().size)
    }
}
