package luzzr.ji

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.repository.BudgetRepository
import luzzr.ji.domain.repository.TransactionRepository
import luzzr.ji.domain.usecase.*
import luzzr.ji.feature.extrabill.ExtraBillUiEvent
import luzzr.ji.feature.extrabill.ExtraBillViewModel
import luzzr.ji.feature.home.HomeUiEvent
import luzzr.ji.feature.home.HomeViewModel
import luzzr.ji.feature.settings.SettingsUiEvent
import luzzr.ji.feature.settings.SettingsViewModel
import luzzr.ji.feature.statistics.StatisticsDimension
import luzzr.ji.feature.statistics.StatisticsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTests {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zoneId = ZoneId.of("Asia/Shanghai")

    private val currentYM = YearMonth.now(zoneId)
    private val currentMonthStr = currentYM.toString()

    private val sampleTxThisMonth = Transaction(
        id = 1L,
        amount = 12000L,
        type = TransactionType.EXPENSE,
        category = "Food",
        note = "lunch",
        timestamp = System.currentTimeMillis(),
        isExtra = false
    )

    private val sampleExtraTx = Transaction(
        id = 2L,
        amount = 5000L,
        type = TransactionType.EXPENSE,
        category = "Reward",
        note = "extra spend",
        timestamp = System.currentTimeMillis(),
        isExtra = true
    )

    private val sampleBudget = Budget(currentMonthStr, 300000L)

    private class FakeTxRepo(var list: List<Transaction>) : TransactionRepository {
        var savedTx: Transaction? = null
        var updatedTx: Transaction? = null
        var deletedTx: Transaction? = null
        var saveCount = 0
        var updateCount = 0

        override fun observeAllTransactions(): Flow<List<Transaction>> = flowOf(list)
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun saveTransaction(transaction: Transaction): Long {
            saveCount++
            savedTx = transaction
            return 77L
        }
        override suspend fun updateTransaction(transaction: Transaction) {
            updateCount++
            updatedTx = transaction
            savedTx = transaction
        }
        override suspend fun deleteTransaction(transaction: Transaction) {
            deletedTx = transaction
        }
    }

    private class FakeBudgetRepo(var budgets: List<Budget>) : BudgetRepository {
        var savedBudget: Budget? = null

        override fun observeBudget(yearMonth: String): Flow<Budget?> = flowOf(budgets.find { it.yearMonth == yearMonth })
        override fun observeAllBudgets(): Flow<List<Budget>> = flowOf(budgets)
        override suspend fun getBudget(yearMonth: String): Budget? = budgets.find { it.yearMonth == yearMonth }
        override suspend fun getAllBudgets(): List<Budget> = budgets
        override suspend fun saveBudget(budget: Budget) {
            savedBudget = budget
        }
    }

    private class FakeContext : android.content.ContextWrapper(null) {
        override fun getPackageName(): String = "luzzr.ji"
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHomeViewModel() = runTest {
        val txRepo = FakeTxRepo(listOf(sampleTxThisMonth, sampleExtraTx))
        val budgetRepo = FakeBudgetRepo(listOf(sampleBudget))

        val homeViewModel = HomeViewModel(
            observeTransactionsUseCase = ObserveTransactionsUseCase(txRepo),
            createTransactionUseCase = CreateTransactionUseCase(txRepo),
            updateTransactionUseCase = UpdateTransactionUseCase(txRepo),
            deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
            migrateTransactionUseCase = MigrateTransactionUseCase(txRepo),
            observeBudgetUseCase = ObserveBudgetUseCase(budgetRepo),
            zoneId = zoneId
        )

        val initialState = homeViewModel.uiState.value
        assertEquals(1, initialState.transactions.size)
        assertEquals(sampleTxThisMonth, initialState.transactions[0])
        assertEquals(300000L, initialState.monthlyBudget)
        assertEquals(12000L, initialState.totalExpense)
        assertFalse(initialState.showAddDialog)

        homeViewModel.onEvent(HomeUiEvent.ToggleAddDialog)
        assertTrue(homeViewModel.uiState.value.showAddDialog)

        homeViewModel.onEvent(HomeUiEvent.AmountChanged("15.5"))
        homeViewModel.onEvent(HomeUiEvent.TypeChanged("EXPENSE"))
        homeViewModel.onEvent(HomeUiEvent.CategoryChanged("Drinks"))
        homeViewModel.onEvent(HomeUiEvent.NoteChanged("Cola"))
        homeViewModel.onEvent(HomeUiEvent.DateChanged(1719113600000L))

        val stateAfterInput = homeViewModel.uiState.value
        assertEquals("15.5", stateAfterInput.addAmount)
        assertEquals("Drinks", stateAfterInput.addCategory)
        assertEquals("Cola", stateAfterInput.addNote)
        assertEquals(1719113600000L, stateAfterInput.addTimestamp)

        homeViewModel.onEvent(HomeUiEvent.StartEdit(sampleTxThisMonth))
        assertTrue(homeViewModel.uiState.value.showAddDialog)
        assertEquals(sampleTxThisMonth, homeViewModel.uiState.value.editingTransaction)
        assertEquals("120", homeViewModel.uiState.value.addAmount)

        homeViewModel.onEvent(HomeUiEvent.SaveTransaction)
        assertEquals(0, txRepo.saveCount)
        assertEquals(1, txRepo.updateCount)
        assertNotNull(txRepo.updatedTx)
        assertEquals(sampleTxThisMonth.id, txRepo.updatedTx?.id)
        assertEquals(12000L, txRepo.updatedTx?.amount ?: 0L)

        homeViewModel.onEvent(HomeUiEvent.ToggleAddDialog)
        homeViewModel.onEvent(HomeUiEvent.AmountChanged("invalid"))
        homeViewModel.onEvent(HomeUiEvent.SaveTransaction)
        assertNotNull(homeViewModel.uiState.value.errorMessage)

        homeViewModel.onEvent(HomeUiEvent.ShowDeleteConfirm(sampleTxThisMonth))
        assertEquals(sampleTxThisMonth, homeViewModel.uiState.value.showDeleteConfirmDialog)
        homeViewModel.onEvent(HomeUiEvent.ConfirmDelete)
        assertEquals(sampleTxThisMonth, txRepo.deletedTx)

        homeViewModel.onEvent(HomeUiEvent.DeleteTransaction(sampleTxThisMonth))
        assertEquals(sampleTxThisMonth, txRepo.deletedTx)

        homeViewModel.onEvent(HomeUiEvent.MigrateToExtra(sampleTxThisMonth))
        assertTrue(txRepo.updatedTx?.isExtra ?: false)
    }

    @Test
    fun testStatisticsViewModel() = runTest {
        val txRepo = FakeTxRepo(listOf(sampleTxThisMonth))
        val statisticsViewModel = StatisticsViewModel(
            observeTransactionsUseCase = ObserveTransactionsUseCase(txRepo),
            zoneId = zoneId
        )

        val state = statisticsViewModel.uiState.value
        assertEquals(12000L, state.totalSpend)
        assertEquals(1, state.categorySpends.size)
        assertEquals("Food", state.categorySpends[0].category)
        assertEquals(1.0f, state.categorySpends[0].percentage, 0.001f)

        val currentDay = LocalDate.now(zoneId).dayOfMonth
        val todaySpend = state.dailySpends.find { it.label == "${currentDay}日" }
        assertEquals(12000L, todaySpend?.amount ?: 0L)

        statisticsViewModel.selectDimension(StatisticsDimension.WEEK)
        val weekState = statisticsViewModel.uiState.value
        assertEquals(StatisticsDimension.WEEK, weekState.selectedDimension)
        assertEquals(7, weekState.dailySpends.size)

        statisticsViewModel.selectDimension(StatisticsDimension.YEAR)
        val yearState = statisticsViewModel.uiState.value
        assertEquals(StatisticsDimension.YEAR, yearState.selectedDimension)
        assertEquals(12, yearState.dailySpends.size)
    }

    @Test
    fun testExtraBillViewModel() = runTest {
        val txRepo = FakeTxRepo(listOf(sampleExtraTx))
        val budgetRepo = FakeBudgetRepo(listOf(sampleBudget))

        val extraViewModel = ExtraBillViewModel(
            observeTransactionsUseCase = ObserveTransactionsUseCase(txRepo),
            createTransactionUseCase = CreateTransactionUseCase(txRepo),
            updateTransactionUseCase = UpdateTransactionUseCase(txRepo),
            deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
            migrateTransactionUseCase = MigrateTransactionUseCase(txRepo),
            getExtraBillOverviewUseCase = GetExtraBillOverviewUseCase(txRepo, budgetRepo, zoneId)
        )

        val state = extraViewModel.uiState.value
        assertEquals(1, state.extraTransactions.size)
        assertEquals(sampleExtraTx, state.extraTransactions[0])

        extraViewModel.onEvent(ExtraBillUiEvent.ToggleAddDialog)
        extraViewModel.onEvent(ExtraBillUiEvent.AmountChanged("20.0"))
        extraViewModel.onEvent(ExtraBillUiEvent.CategoryChanged("Book"))
        extraViewModel.onEvent(ExtraBillUiEvent.NoteChanged("buy novel"))
        extraViewModel.onEvent(ExtraBillUiEvent.DateChanged(1719113600000L))
        extraViewModel.onEvent(ExtraBillUiEvent.SaveTransaction)

        assertNotNull(txRepo.savedTx)
        assertEquals(2000L, txRepo.savedTx?.amount ?: 0L)
        assertTrue(txRepo.savedTx?.isExtra ?: false)
        assertEquals(1, txRepo.saveCount)
        assertEquals(0, txRepo.updateCount)

        extraViewModel.onEvent(ExtraBillUiEvent.StartEdit(sampleExtraTx))
        assertTrue(extraViewModel.uiState.value.showAddDialog)
        assertEquals(sampleExtraTx, extraViewModel.uiState.value.editingTransaction)
        extraViewModel.onEvent(ExtraBillUiEvent.SaveTransaction)
        assertEquals(1, txRepo.saveCount)
        assertEquals(1, txRepo.updateCount)
        assertEquals(sampleExtraTx.id, txRepo.updatedTx?.id)

        extraViewModel.onEvent(ExtraBillUiEvent.ShowDeleteConfirm(sampleExtraTx))
        assertEquals(sampleExtraTx, extraViewModel.uiState.value.showDeleteConfirmDialog)
        extraViewModel.onEvent(ExtraBillUiEvent.ConfirmDelete)
        assertEquals(sampleExtraTx, txRepo.deletedTx)

        extraViewModel.onEvent(ExtraBillUiEvent.MigrateToNormal(sampleExtraTx))
        assertFalse(txRepo.updatedTx?.isExtra ?: true)

        extraViewModel.onEvent(ExtraBillUiEvent.DeleteTransaction(sampleExtraTx))
        assertEquals(sampleExtraTx, txRepo.deletedTx)
    }

    class FakeSecureStorage : luzzr.ji.core.common.SecureStorage {
        var apiKeyVal = ""
        override fun getApiKey(): String = apiKeyVal
        override fun saveApiKey(key: String) {
            apiKeyVal = key
        }
    }

    @Test
    fun testSettingsViewModel() = runTest {
        val budgetRepo = FakeBudgetRepo(listOf(sampleBudget))
        val fakePrefs = FakeSharedPreferences()
        val fakeSecureStorage = FakeSecureStorage()

        val settingsViewModel = SettingsViewModel(
            observeBudgetUseCase = ObserveBudgetUseCase(budgetRepo),
            saveBudgetUseCase = SaveBudgetUseCase(budgetRepo),
            secureStorage = fakeSecureStorage,
            sharedPreferences = fakePrefs,
            zoneId = zoneId
        )

        assertEquals("3000", settingsViewModel.uiState.value.budgetInput)
        assertEquals("", settingsViewModel.uiState.value.opencodeApiKey)
        assertEquals("mimo-v2.5", settingsViewModel.uiState.value.opencodeModel)

        // 1. 测试预算额度修改保存
        settingsViewModel.onEvent(SettingsUiEvent.BudgetInputChanged("4500"))
        assertEquals("4500", settingsViewModel.uiState.value.budgetInput)

        settingsViewModel.onEvent(SettingsUiEvent.SaveBudget)
        assertEquals(450000L, budgetRepo.savedBudget?.amount ?: 0L)
        assertTrue(settingsViewModel.uiState.value.isBudgetSaved)

        // 2. 测试 API Key 修改与模型选择修改
        settingsViewModel.onEvent(SettingsUiEvent.ApiKeyChanged("opencode-go-api-key-test"))
        settingsViewModel.onEvent(SettingsUiEvent.ModelChanged("mimo-v2.5"))
        assertEquals("opencode-go-api-key-test", settingsViewModel.uiState.value.opencodeApiKey)
        assertEquals("mimo-v2.5", settingsViewModel.uiState.value.opencodeModel)
        assertFalse(settingsViewModel.uiState.value.isApiKeySaved)

        // 3. 保存配置
        settingsViewModel.onEvent(SettingsUiEvent.SaveApiKey)
        assertTrue(settingsViewModel.uiState.value.isApiKeySaved)
        assertEquals("opencode-go-api-key-test", fakeSecureStorage.getApiKey())
        assertEquals("mimo-v2.5", fakePrefs.getString("opencode_model_id", ""))

        // 4. 测试重新初始化时读取已存 API Key & Model ID
        val newSettingsViewModel = SettingsViewModel(
            observeBudgetUseCase = ObserveBudgetUseCase(budgetRepo),
            saveBudgetUseCase = SaveBudgetUseCase(budgetRepo),
            secureStorage = fakeSecureStorage,
            sharedPreferences = fakePrefs,
            zoneId = zoneId
        )
        assertEquals("opencode-go-api-key-test", newSettingsViewModel.uiState.value.opencodeApiKey)
        assertEquals("mimo-v2.5", newSettingsViewModel.uiState.value.opencodeModel)
    }

    class FakeSharedPreferences : android.content.SharedPreferences {
        private val map = mutableMapOf<String, String>()

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = map[key] ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long = defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): android.content.SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val map: MutableMap<String, String>) : android.content.SharedPreferences.Editor {
            private val temp = mutableMapOf<String, String>()
            override fun putString(key: String, value: String?): android.content.SharedPreferences.Editor {
                if (value != null) temp[key] = value else temp.remove(key)
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor = this
            override fun remove(key: String?): android.content.SharedPreferences.Editor = this
            override fun clear(): android.content.SharedPreferences.Editor {
                temp.clear()
                return this
            }
            override fun commit(): Boolean {
                map.putAll(temp)
                return true
            }
            override fun apply() {
                map.putAll(temp)
            }
        }
    }
}
