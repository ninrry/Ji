package luzzr.ji.feature.home

import luzzr.ji.domain.model.Transaction

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val monthlyBudget: Long = 0L,
    val totalExpense: Long = 0L,
    val latestAutoRecordFailure: String? = null,
    val showAddDialog: Boolean = false,
    val addAmount: String = "",
    val addType: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val addCategory: String = "",
    val addNote: String = "",
    val addTimestamp: Long? = null,
    val editingTransaction: Transaction? = null,
    val showDeleteConfirmDialog: Transaction? = null,
    val errorMessage: String? = null
)

sealed interface HomeUiEvent {
    data object ToggleAddDialog : HomeUiEvent
    data class AmountChanged(val value: String) : HomeUiEvent
    data class TypeChanged(val value: String) : HomeUiEvent
    data class CategoryChanged(val value: String) : HomeUiEvent
    data class NoteChanged(val value: String) : HomeUiEvent
    data class DateChanged(val timestamp: Long) : HomeUiEvent
    data object SaveTransaction : HomeUiEvent
    data class DeleteTransaction(val transaction: Transaction) : HomeUiEvent
    data class MigrateToExtra(val transaction: Transaction) : HomeUiEvent
    data class StartEdit(val transaction: Transaction) : HomeUiEvent
    data class ShowDeleteConfirm(val transaction: Transaction?) : HomeUiEvent
    data object ConfirmDelete : HomeUiEvent
}

sealed interface HomeUiEffect {
    data class ShowToast(val message: String) : HomeUiEffect
}
