package luzzr.ji.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import luzzr.ji.core.common.MoneyAmountParser
import luzzr.ji.core.payment.PaymentRecognitionManager
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.usecase.CreateTransactionUseCase
import luzzr.ji.domain.usecase.DeleteTransactionUseCase
import luzzr.ji.domain.usecase.MigrateTransactionUseCase
import luzzr.ji.domain.usecase.ObserveBudgetUseCase
import luzzr.ji.domain.usecase.ObserveTransactionsUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class HomeViewModel(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val migrateTransactionUseCase: MigrateTransactionUseCase,
    private val observeBudgetUseCase: ObserveBudgetUseCase,
    private val paymentRecognitionManager: PaymentRecognitionManager? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeUiEffect>()
    val uiEffect: SharedFlow<HomeUiEffect> = _uiEffect.asSharedFlow()

    init {
        val currentMillis = System.currentTimeMillis()
        val currentYearMonth = YearMonth.from(Instant.ofEpochMilli(currentMillis).atZone(zoneId).toLocalDate()).toString()

        // 联合观察账单和当月预算
        observeTransactionsUseCase()
            .combine(observeBudgetUseCase(currentYearMonth)) { transactions, budgetEntity ->
                val budgetVal = budgetEntity?.amount ?: 0L
                
                // 本月普通支出过滤
                val currentLocalDate = Instant.ofEpochMilli(currentMillis).atZone(zoneId).toLocalDate()
                val currentYM = YearMonth.from(currentLocalDate)
                
                val normalTransactions = transactions.filter { !it.isExtra }
                
                val currentMonthExpenses = normalTransactions.filter {
                    val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
                    YearMonth.from(date) == currentYM && it.type == TransactionType.EXPENSE
                }
                val totalExpense = currentMonthExpenses.sumOf { it.amount }

                _uiState.update { state ->
                    state.copy(
                        transactions = normalTransactions,
                        monthlyBudget = budgetVal,
                        totalExpense = totalExpense
                    )
                }
            }
            .launchIn(viewModelScope)

        paymentRecognitionManager?.observeLatestFailure()
            ?.onEach { message -> _uiState.update { it.copy(latestAutoRecordFailure = message) } }
            ?.launchIn(viewModelScope)

    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ToggleAddDialog -> {
                _uiState.update { 
                    if (it.showAddDialog) {
                        // 隐藏时，清空输入和编辑状态
                        it.copy(
                            showAddDialog = false,
                            editingTransaction = null,
                            addTimestamp = null,
                            addAmount = "",
                            addCategory = "",
                            addNote = "",
                            errorMessage = null
                        )
                    } else {
                        it.copy(showAddDialog = true, errorMessage = null)
                    }
                }
            }
            is HomeUiEvent.AmountChanged -> {
                _uiState.update { it.copy(addAmount = event.value) }
            }
            is HomeUiEvent.TypeChanged -> {
                _uiState.update { it.copy(addType = event.value) }
            }
            is HomeUiEvent.CategoryChanged -> {
                _uiState.update { it.copy(addCategory = event.value) }
            }
            is HomeUiEvent.NoteChanged -> {
                _uiState.update { it.copy(addNote = event.value) }
            }
            is HomeUiEvent.DateChanged -> {
                _uiState.update { it.copy(addTimestamp = event.timestamp) }
            }
            HomeUiEvent.SaveTransaction -> {
                saveTransaction()
            }
            is HomeUiEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    deleteTransactionUseCase(event.transaction)
                    _uiEffect.emit(HomeUiEffect.ShowToast("账单已删除"))
                }
            }
            is HomeUiEvent.MigrateToExtra -> {
                viewModelScope.launch {
                    migrateTransactionUseCase(event.transaction, toExtra = true)
                    _uiEffect.emit(HomeUiEffect.ShowToast("已迁入额外账单"))
                }
            }
            is HomeUiEvent.StartEdit -> {
                _uiState.update {
                    val amountStr = if (event.transaction.amount % 100 == 0L) (event.transaction.amount / 100).toString() else String.format(Locale.US, "%.2f", event.transaction.amount / 100.0)
                    it.copy(
                        showAddDialog = true,
                        editingTransaction = event.transaction,
                        addAmount = amountStr,
                        addType = event.transaction.type.name,
                        addCategory = event.transaction.category,
                        addNote = event.transaction.note,
                        addTimestamp = event.transaction.timestamp,
                        errorMessage = null
                    )
                }
            }
            is HomeUiEvent.ShowDeleteConfirm -> {
                _uiState.update { it.copy(showDeleteConfirmDialog = event.transaction) }
            }
            HomeUiEvent.ConfirmDelete -> {
                val toDelete = _uiState.value.showDeleteConfirmDialog
                if (toDelete != null) {
                    viewModelScope.launch {
                        deleteTransactionUseCase(toDelete)
                        _uiState.update { it.copy(showDeleteConfirmDialog = null) }
                        _uiEffect.emit(HomeUiEffect.ShowToast("账单已删除"))
                    }
                }
            }
        }
    }

    private fun saveTransaction() {
        val amount = MoneyAmountParser.yuanToFenOrNull(_uiState.value.addAmount)
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "金额必须大于 0，且最多保留两位小数") }
            return
        }
        val type = if (_uiState.value.addType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        val category = _uiState.value.addCategory
        val note = _uiState.value.addNote
        val timestamp = _uiState.value.addTimestamp ?: System.currentTimeMillis()

        val editing = _uiState.value.editingTransaction
        val tx = if (editing != null) {
            editing.copy(
                amount = amount,
                type = type,
                category = category,
                note = note,
                timestamp = timestamp
            )
        } else {
            Transaction(
                amount = amount,
                type = type,
                category = category,
                note = note,
                timestamp = timestamp,
                isExtra = false,
                source = "MANUAL"
            )
        }

        viewModelScope.launch {
            createTransactionUseCase(tx)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showAddDialog = false,
                            editingTransaction = null,
                            addTimestamp = null,
                            addAmount = "",
                            addCategory = "",
                            addNote = ""
                        )
                    }
                    _uiEffect.emit(HomeUiEffect.ShowToast(if (editing != null) "账单修改成功" else "记账成功"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

}
