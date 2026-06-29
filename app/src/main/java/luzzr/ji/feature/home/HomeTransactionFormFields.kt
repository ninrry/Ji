package luzzr.ji.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import luzzr.ji.core.design.CalendarIcon
import luzzr.ji.core.design.JiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TypeToggleRow(selected: String, onTypeChanged: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(JiTheme.colors.cardBackground)
    ) {
        TypeToggleSegment(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "支出",
            value = "EXPENSE",
            selected = selected,
            onTypeChanged = onTypeChanged
        )
        TypeToggleSegment(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "收入",
            value = "INCOME",
            selected = selected,
            onTypeChanged = onTypeChanged
        )
    }
}

@Composable
private fun TypeToggleSegment(
    modifier: Modifier,
    label: String,
    value: String,
    selected: String,
    onTypeChanged: (String) -> Unit
) {
    val isSelected = selected == value
    Box(
        modifier = modifier
            .background(if (isSelected) JiTheme.colors.stroke else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTypeChanged(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) JiTheme.colors.background else JiTheme.colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    bringIntoView: BringIntoViewRequester,
    focusScope: kotlinx.coroutines.CoroutineScope
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("金额 (元)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = formFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) focusScope.launch { bringIntoView.bringIntoView() }
            },
        singleLine = true
    )
}

@Composable
fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    bringIntoView: BringIntoViewRequester,
    focusScope: kotlinx.coroutines.CoroutineScope
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("备注 (选填)") },
        colors = formFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) focusScope.launch { bringIntoView.bringIntoView() }
            },
        singleLine = true
    )
}

@Composable
fun CategoryField(
    value: String,
    onValueChange: (String) -> Unit,
    bringIntoView: BringIntoViewRequester,
    focusScope: kotlinx.coroutines.CoroutineScope
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("分类 (如 餐饮, 交通)") },
        colors = formFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) focusScope.launch { bringIntoView.bringIntoView() }
            },
        singleLine = true
    )
}

@Composable
fun QuickCategories(
    type: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val categories = if (type == "EXPENSE") {
        listOf("餐饮", "交通", "购物", "娱乐", "居住", "人情", "犒劳", "转账", "红包", "提现", "花呗还款", "白条还款", "其它")
    } else {
        listOf("工资", "理财", "兼职", "红包", "其它")
    }
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val isSelected = selected == cat
            Box(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.divider,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) JiTheme.colors.monetGreen.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = cat,
                    fontSize = 11.sp,
                    color = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DateShortcutRow(
    selectedTimestamp: Long?,
    onDateChanged: (Long) -> Unit
) {
    val dateScrollState = rememberScrollState()
    val context = LocalContext.current
    val currentTimestamp = selectedTimestamp ?: System.currentTimeMillis()
    val currentLocalDate = Instant.ofEpochMilli(currentTimestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formattedDate = currentLocalDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(dateScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "今天" to LocalDate.now(),
                "昨天" to LocalDate.now().minusDays(1),
                "前天" to LocalDate.now().minusDays(2)
            ).forEach { (label, date) ->
                val isSelected = currentLocalDate == date
                Box(
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.divider,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) JiTheme.colors.monetGreen.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val ts = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            onDateChanged(ts)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(JiTheme.colors.cardBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val year = currentLocalDate.year
                    val month = currentLocalDate.monthValue - 1
                    val day = currentLocalDate.dayOfMonth
                    android.app.DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val selectedLocalDate = LocalDate.of(y, m + 1, d)
                            val ts = selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            onDateChanged(ts)
                        },
                        year,
                        month,
                        day
                    ).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarIcon(color = JiTheme.colors.stroke, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = formattedDate, fontSize = 11.sp, color = JiTheme.colors.textPrimary)
        }
    }
}

@Composable
fun SaveButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(JiTheme.colors.monetGreen)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = JiTheme.colors.stroke, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JiTheme.colors.stroke,
    unfocusedBorderColor = JiTheme.colors.divider,
    focusedLabelColor = JiTheme.colors.stroke,
    unfocusedLabelColor = JiTheme.colors.textSecondary,
    focusedTextColor = JiTheme.colors.textPrimary,
    unfocusedTextColor = JiTheme.colors.textPrimary
)
