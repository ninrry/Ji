package luzzr.ji.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.ji.core.design.CalendarIcon
import luzzr.ji.core.design.JiTheme

@Composable
fun AddTransactionForm(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit
) {
    val isEditMode = state.editingTransaction != null
    val formScrollState = rememberScrollState()
    val focusScope = rememberCoroutineScope()
    val amountBringIntoView = remember { BringIntoViewRequester() }
    val categoryBringIntoView = remember { BringIntoViewRequester() }
    val noteBringIntoView = remember { BringIntoViewRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(formScrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isEditMode) "修改账单" else "新记一笔",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = JiTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        TypeToggleRow(
            selected = state.addType,
            onTypeChanged = { onEvent(HomeUiEvent.TypeChanged(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AmountField(
            value = state.addAmount,
            onValueChange = { onEvent(HomeUiEvent.AmountChanged(it)) },
            bringIntoView = amountBringIntoView,
            focusScope = focusScope
        )

        Spacer(modifier = Modifier.height(12.dp))

        CategoryField(
            value = state.addCategory,
            onValueChange = { onEvent(HomeUiEvent.CategoryChanged(it)) },
            bringIntoView = categoryBringIntoView,
            focusScope = focusScope
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickCategories(
            type = state.addType,
            selected = state.addCategory,
            onSelect = { onEvent(HomeUiEvent.CategoryChanged(it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        NoteField(
            value = state.addNote,
            onValueChange = { onEvent(HomeUiEvent.NoteChanged(it)) },
            bringIntoView = noteBringIntoView,
            focusScope = focusScope
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "记账日期",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JiTheme.colors.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        DateShortcutRow(
            selectedTimestamp = state.addTimestamp,
            onDateChanged = { onEvent(HomeUiEvent.DateChanged(it)) }
        )

        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = JiTheme.colors.monetRed,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SaveButton(text = if (isEditMode) "保存修改" else "保存") {
            onEvent(HomeUiEvent.SaveTransaction)
        }
    }
}
