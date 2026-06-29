package luzzr.ji.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.ji.core.design.JiTheme

@Composable
fun BudgetSettingsSection(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = "月度收支预算",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = JiTheme.colors.textPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(JiTheme.colors.cardBackground)
            .padding(16.dp)
            .testTag("settings_budget_section")
    ) {
        Text(
            text = "在此设定您的每月默认消费预算额度。超支时预算额度池会相应扣减或给出警示。",
            fontSize = 12.sp,
            color = JiTheme.colors.textSecondary,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.budgetInput,
                onValueChange = { onEvent(SettingsUiEvent.BudgetInputChanged(it)) },
                label = { Text("默认预算额度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = settingsTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("settings_budget_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .width(80.dp)
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(JiTheme.colors.monetGreen)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEvent(SettingsUiEvent.SaveBudget) }
                    .testTag("settings_save_budget"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存",
                    color = JiTheme.colors.stroke,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = error, color = JiTheme.colors.monetRed, fontSize = 12.sp)
        }
    }
}

@Composable
fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JiTheme.colors.stroke,
    unfocusedBorderColor = JiTheme.colors.divider,
    focusedLabelColor = JiTheme.colors.stroke,
    unfocusedLabelColor = JiTheme.colors.textSecondary,
    focusedTextColor = JiTheme.colors.textPrimary,
    unfocusedTextColor = JiTheme.colors.textPrimary
)
