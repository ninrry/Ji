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
fun ExtraBillRoute(
    viewModel: ExtraBillViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = viewModel.uiEffect) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is ExtraBillUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ExtraBillScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun ExtraBillScreen(
    state: ExtraBillUiState,
    onEvent: (ExtraBillUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ExtraPoolOverviewWidget(
                    totalSaved = state.overview.totalPoolAmount,
                    remaining = state.overview.remainingPoolAmount
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExtraTransactionList(
                transactions = state.extraTransactions,
                onMigrate = { onEvent(ExtraBillUiEvent.MigrateToNormal(it)) },
                onEdit = { onEvent(ExtraBillUiEvent.StartEdit(it)) },
                onDelete = { onEvent(ExtraBillUiEvent.ShowDeleteConfirm(it)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        // 右下角悬浮按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(1.dp, JiTheme.colors.stroke, CircleShape)
                    .clip(CircleShape)
                    .background(JiTheme.colors.monetYellow)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(ExtraBillUiEvent.ToggleAddDialog)
                    },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = JiTheme.colors.stroke)
            }
        }

        // 记一笔浮动弹窗
        if (state.showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(ExtraBillUiEvent.ToggleAddDialog)
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .border(
                            width = 1.dp,
                            color = JiTheme.colors.stroke,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .background(JiTheme.colors.background)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* 阻断点击 */ }
                        .padding(24.dp)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    AddExtraForm(
                        state = state,
                        onEvent = onEvent
                    )
                }
            }
        }

        // 删除二次确认 Dialog
        if (state.showDeleteConfirmDialog != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(ExtraBillUiEvent.ShowDeleteConfirm(null))
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(JiTheme.colors.background)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "删除确认",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "确定要删除这笔额外账单吗？额度将被回滚。",
                        fontSize = 13.sp,
                        color = JiTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onEvent(ExtraBillUiEvent.ShowDeleteConfirm(null))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "取消", fontSize = 13.sp, color = JiTheme.colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(JiTheme.colors.monetRed)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onEvent(ExtraBillUiEvent.ConfirmDelete)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "删除", fontSize = 13.sp, color = JiTheme.colors.stroke, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtraPoolOverviewWidget(totalSaved: Long, remaining: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .size(160.dp)
                .border(1.dp, JiTheme.colors.stroke, CircleShape)
                .clip(CircleShape)
                .background(JiTheme.colors.monetYellow.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "省下可用钱数",
                    fontSize = 11.sp,
                    color = JiTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.getDefault(), "¥%.2f", remaining / 100.0),
                    fontSize = 24.sp,
                    color = JiTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "历次结余累计 ¥%.0f", totalSaved / 100.0),
                    fontSize = 10.sp,
                    color = JiTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun ExtraTransactionList(
    transactions: List<Transaction>,
    onMigrate: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "还没有额外消费记录\n普通账单长按菜单可迁入此处",
                color = JiTheme.colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        item {
            Text(
                text = "额外账单明细",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(transactions) { tx ->
            ExtraTransactionItem(
                transaction = tx,
                onMigrate = { onMigrate(tx) },
                onEdit = { onEdit(tx) },
                onDelete = { onDelete(tx) }
            )
        }
    }
}

@Composable
fun ExtraTransactionItem(
    transaction: Transaction,
    onMigrate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val localDate = Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val displayDate = localDate.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 0.5.dp,
                color = JiTheme.colors.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(JiTheme.colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showMenu = !showMenu
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JiTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayDate,
                            fontSize = 10.sp,
                            color = JiTheme.colors.textSecondary
                        )
                    }
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            fontSize = 12.sp,
                            color = JiTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Text(
                    text = String.format(Locale.getDefault(), "-¥%.2f", transaction.amount / 100.0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JiTheme.colors.textPrimary
                )
            }

            AnimatedVisibility(
                visible = showMenu,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(JiTheme.colors.divider)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "迁回普通账单",
                            color = JiTheme.colors.monetGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onMigrate()
                                }
                                .border(0.5.dp, JiTheme.colors.monetGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "编辑",
                            color = JiTheme.colors.monetBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onEdit()
                                }
                                .border(0.5.dp, JiTheme.colors.monetBlue, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "删除",
                            color = JiTheme.colors.monetRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onDelete()
                                }
                                .border(0.5.dp, JiTheme.colors.monetRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

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
