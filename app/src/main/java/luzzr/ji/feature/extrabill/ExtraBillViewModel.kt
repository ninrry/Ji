package luzzr.ji.feature.extrabill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import luzzr.ji.core.common.MoneyAmountParser
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.usecase.CreateTransactionUseCase
import luzzr.ji.domain.usecase.DeleteTransactionUseCase
import luzzr.ji.domain.usecase.GetExtraBillOverviewUseCase
import luzzr.ji.domain.usecase.MigrateTransactionUseCase
import luzzr.ji.domain.usecase.ObserveTransactionsUseCase
import luzzr.ji.domain.usecase.UpdateTransactionUseCase
import java.util.Locale

class ExtraBillViewModel(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val migrateTransactionUseCase: MigrateTransactionUseCase,
    private val getExtraBillOverviewUseCase: GetExtraBillOverviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtraBillUiState())
    val uiState: StateFlow<ExtraBillUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ExtraBillUiEffect>()
    val uiEffect: SharedFlow<ExtraBillUiEffect> = _uiEffect.asSharedFlow()

    init {
        // 观察额外账单列表
        observeTransactionsUseCase()
            .map { list -> list.filter { it.isExtra } }
            .onEach { extraList ->
                _uiState.update { it.copy(extraTransactions = extraList) }
            }
            .launchIn(viewModelScope)

        // 观察结余额度概览
        getExtraBillOverviewUseCase()
            .onEach { overview ->
                _uiState.update { it.copy(overview = overview) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ExtraBillUiEvent) {
        when (event) {
            ExtraBillUiEvent.ToggleAddDialog -> {
                _uiState.update {
                    if (it.showAddDialog) {
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
            is ExtraBillUiEvent.AmountChanged -> {
                _uiState.update { it.copy(addAmount = event.value) }
            }
            is ExtraBillUiEvent.CategoryChanged -> {
                _uiState.update { it.copy(addCategory = event.value) }
            }
            is ExtraBillUiEvent.NoteChanged -> {
                _uiState.update { it.copy(addNote = event.value) }
            }
            is ExtraBillUiEvent.DateChanged -> {
                _uiState.update { it.copy(addTimestamp = event.timestamp) }
            }
            ExtraBillUiEvent.SaveTransaction -> {
                saveTransaction()
            }
            is ExtraBillUiEvent.MigrateToNormal -> {
                viewModelScope.launch {
                    migrateTransactionUseCase(event.transaction, toExtra = false)
                    _uiEffect.emit(ExtraBillUiEffect.ShowToast("已移回普通账单"))
                }
            }
            is ExtraBillUiEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    deleteTransactionUseCase(event.transaction)
                    _uiEffect.emit(ExtraBillUiEffect.ShowToast("账单已删除"))
                }
            }
            is ExtraBillUiEvent.StartEdit -> {
                _uiState.update {
                    val amountStr = if (event.transaction.amount % 100 == 0L) (event.transaction.amount / 100).toString() else String.format(Locale.US, "%.2f", event.transaction.amount / 100.0)
                    it.copy(
                        showAddDialog = true,
                        editingTransaction = event.transaction,
                        addAmount = amountStr,
                        addCategory = event.transaction.category,
                        addNote = event.transaction.note,
                        addTimestamp = event.transaction.timestamp,
                        errorMessage = null
                    )
                }
            }
            is ExtraBillUiEvent.ShowDeleteConfirm -> {
                _uiState.update { it.copy(showDeleteConfirmDialog = event.transaction) }
            }
            ExtraBillUiEvent.ConfirmDelete -> {
                val toDelete = _uiState.value.showDeleteConfirmDialog
                if (toDelete != null) {
                    viewModelScope.launch {
                        deleteTransactionUseCase(toDelete)
                        _uiState.update { it.copy(showDeleteConfirmDialog = null) }
                        _uiEffect.emit(ExtraBillUiEffect.ShowToast("账单已删除"))
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
        val category = _uiState.value.addCategory
        val note = _uiState.value.addNote
        val timestamp = _uiState.value.addTimestamp ?: System.currentTimeMillis()

        val editing = _uiState.value.editingTransaction
        val tx = if (editing != null) {
            editing.copy(
                amount = amount,
                category = category,
                note = note,
                timestamp = timestamp
            )
        } else {
            Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                category = category,
                note = note,
                timestamp = timestamp,
                isExtra = true,
                source = "MANUAL"
            )
        }

        viewModelScope.launch {
            val result = if (editing != null) updateTransactionUseCase(tx).map { tx.id } else createTransactionUseCase(tx)
            result
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
                    _uiEffect.emit(ExtraBillUiEffect.ShowToast(if (editing != null) "账单修改成功" else "消费记录已保存"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }
}
