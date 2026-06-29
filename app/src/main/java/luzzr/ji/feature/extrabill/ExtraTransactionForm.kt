package luzzr.ji.feature.extrabill

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import luzzr.ji.core.design.*
import luzzr.ji.domain.model.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddExtraForm(
    state: ExtraBillUiState,
    onEvent: (ExtraBillUiEvent) -> Unit
) {
    val isEditMode = state.editingTransaction != null
    val scrollStateCat = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isEditMode) "修改省钱消费" else "记一笔省钱消费",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = JiTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "此笔消费将直接扣减您的省钱额度池",
            fontSize = 11.sp,
            color = JiTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.addAmount,
            onValueChange = { onEvent(ExtraBillUiEvent.AmountChanged(it)) },
            label = { Text("消费金额 (元)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JiTheme.colors.stroke,
                unfocusedBorderColor = JiTheme.colors.divider,
                focusedLabelColor = JiTheme.colors.stroke,
                unfocusedLabelColor = JiTheme.colors.textSecondary,
                focusedTextColor = JiTheme.colors.textPrimary,
                unfocusedTextColor = JiTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.addCategory,
            onValueChange = { onEvent(ExtraBillUiEvent.CategoryChanged(it)) },
            label = { Text("分类 (如 犒劳, 娱乐)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JiTheme.colors.stroke,
                unfocusedBorderColor = JiTheme.colors.divider,
                focusedLabelColor = JiTheme.colors.stroke,
                unfocusedLabelColor = JiTheme.colors.textSecondary,
                focusedTextColor = JiTheme.colors.textPrimary,
                unfocusedTextColor = JiTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 快捷类别
        val categories = listOf("犒劳", "娱乐", "礼品", "餐饮", "其它")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollStateCat)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = state.addCategory == cat
                Box(
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.divider,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) JiTheme.colors.monetYellow.copy(alpha = 0.3f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onEvent(ExtraBillUiEvent.CategoryChanged(cat))
                        }
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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.addNote,
            onValueChange = { onEvent(ExtraBillUiEvent.NoteChanged(it)) },
            label = { Text("备注 (选填)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JiTheme.colors.stroke,
                unfocusedBorderColor = JiTheme.colors.divider,
                focusedLabelColor = JiTheme.colors.stroke,
                unfocusedLabelColor = JiTheme.colors.textSecondary,
                focusedTextColor = JiTheme.colors.textPrimary,
                unfocusedTextColor = JiTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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

        val dateScrollState = rememberScrollState()
        val context = LocalContext.current
        val currentTimestamp = state.addTimestamp ?: System.currentTimeMillis()
        val currentLocalDate = Instant.ofEpochMilli(currentTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
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
                val datesList = listOf(
                    "今天" to LocalDate.now(),
                    "昨天" to LocalDate.now().minusDays(1),
                    "前天" to LocalDate.now().minusDays(2)
                )
                datesList.forEach { (label, date) ->
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
                                onEvent(ExtraBillUiEvent.DateChanged(ts))
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
                                onEvent(ExtraBillUiEvent.DateChanged(ts))
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

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage,
                color = JiTheme.colors.monetRed,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                ) {
                    onEvent(ExtraBillUiEvent.SaveTransaction)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEditMode) "保存修改" else "扣减额度并保存",
                color = JiTheme.colors.stroke,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
