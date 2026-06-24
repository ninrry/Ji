package luzzr.ji.feature.extrabill

import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.usecase.ExtraBillOverview

data class ExtraBillUiState(
    val overview: ExtraBillOverview = ExtraBillOverview(0L, 0L, 0L),
    val extraTransactions: List<Transaction> = emptyList(),
    val showAddDialog: Boolean = false,
    val addAmount: String = "",
    val addCategory: String = "",
    val addNote: String = "",
    val addTimestamp: Long? = null,
    val editingTransaction: Transaction? = null,
    val showDeleteConfirmDialog: Transaction? = null,
    val errorMessage: String? = null
)

sealed interface ExtraBillUiEvent {
    data object ToggleAddDialog : ExtraBillUiEvent
    data class AmountChanged(val value: String) : ExtraBillUiEvent
    data class CategoryChanged(val value: String) : ExtraBillUiEvent
    data class NoteChanged(val value: String) : ExtraBillUiEvent
    data class DateChanged(val timestamp: Long) : ExtraBillUiEvent
    data object SaveTransaction : ExtraBillUiEvent
    data class MigrateToNormal(val transaction: Transaction) : ExtraBillUiEvent
    data class DeleteTransaction(val transaction: Transaction) : ExtraBillUiEvent
    data class StartEdit(val transaction: Transaction) : ExtraBillUiEvent
    data class ShowDeleteConfirm(val transaction: Transaction?) : ExtraBillUiEvent
    data object ConfirmDelete : ExtraBillUiEvent
}

sealed interface ExtraBillUiEffect {
    data class ShowToast(val message: String) : ExtraBillUiEffect
}
