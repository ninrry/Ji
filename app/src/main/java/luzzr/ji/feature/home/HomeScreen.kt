package luzzr.ji.feature.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import luzzr.ji.core.design.*
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = viewModel.uiEffect) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is HomeUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
    ) {
        // 主内容区铺满屏幕底端，为底部导航栏垫高底部 Padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 顶端预算额度水池区 (占高240dp左右)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                BudgetPoolWidget(
                    budget = state.monthlyBudget,
                    expense = state.totalExpense
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            state.latestAutoRecordFailure?.let { failure ->
                Text(
                    text = "最近自动识别失败：$failure",
                    color = JiTheme.colors.monetRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 账单列表区
            TransactionGroupList(
                transactions = state.transactions,
                onMigrate = { onEvent(HomeUiEvent.MigrateToExtra(it)) },
                onEdit = { onEvent(HomeUiEvent.StartEdit(it)) },
                onDelete = { onEvent(HomeUiEvent.ShowDeleteConfirm(it)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        // 首页自绘悬浮加号按钮 (放在右下角，但要避开底栏，垫高100dp以上)
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
                        onEvent(HomeUiEvent.ToggleAddDialog)
                    },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = JiTheme.colors.stroke)
            }
        }

        // 新增账单大圆角底页弹窗 (自绘浮层)
        if (state.showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(HomeUiEvent.ToggleAddDialog)
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
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
                        ) { /* 阻断点击传递 */ }
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    AddTransactionForm(
                        state = state,
                        onEvent = onEvent
                    )
                }
            }
        }

        // 自绘二次确认删除 Dialog
        if (state.showDeleteConfirmDialog != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(HomeUiEvent.ShowDeleteConfirm(null))
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
                        text = "确定要删除这笔账单吗？此操作无法撤销。",
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
                                    onEvent(HomeUiEvent.ShowDeleteConfirm(null))
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
                                    onEvent(HomeUiEvent.ConfirmDelete)
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
fun BudgetPoolWidget(budget: Long, expense: Long) {
    val remaining = (budget - expense).coerceAtLeast(0L)
    val remainingPercent = if (budget > 0) (remaining.toDouble() / budget).coerceIn(0.0, 1.0) else 0.0

    // 水波波浪动画，利用 infiniteTransition 产生流动的相位
    val infiniteTransition = rememberInfiniteTransition(label = "water_flow")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val strokeColor = JiTheme.colors.stroke
    val waterColor = if (remainingPercent > 0.2f) JiTheme.colors.monetGreen else JiTheme.colors.monetRed
    val poolBgColor = JiTheme.colors.cardBackground

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(1.dp, strokeColor, CircleShape)
                .clip(CircleShape)
                .background(poolBgColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val circlePath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
                }

                // 剩余额度百分比对应水位高度 (0 代表满，1 代表空)
                val targetWaterY = height * (1f - remainingPercent.toFloat())

                clipPath(circlePath) {
                    val wavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, targetWaterY)
                        
                        // 正弦波形生成，平滑波动
                        val steps = 100
                        val waveLength = width
                        val amplitude = 8.dp.toPx()
                        for (i in 0..steps) {
                            val x = (i.toFloat() / steps) * width
                            val y = targetWaterY + amplitude * sin((2 * Math.PI * x / waveLength) + wavePhase).toFloat()
                            lineTo(x, y)
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(path = wavePath, color = waterColor.copy(alpha = 0.5f))
                }
            }

            // 额度数值信息
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "剩余额度池",
                    fontSize = 11.sp,
                    color = JiTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.getDefault(), "¥%.0f", remaining / 100.0),
                    fontSize = 24.sp,
                    color = JiTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "总预算 ¥%.0f", budget / 100.0),
                    fontSize = 10.sp,
                    color = JiTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun TransactionGroupList(
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
                text = "本月暂无记账数据\n点击下方 + 开始记账",
                color = JiTheme.colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        return
    }

    // 按日期分组 (例如 "2026-06-23")
    val grouped = remember(transactions) {
        transactions.groupBy {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            date.toString()
        }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        grouped.forEach { (dateStr, list) ->
            // 分割头部
            item {
                val localDate = LocalDate.parse(dateStr)
                val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
                val displayDate = localDate.format(formatter)

                val daySum = list.sumOf { if (it.type == TransactionType.EXPENSE) -it.amount else it.amount }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                    Text(
                        text = if (daySum >= 0) String.format(Locale.getDefault(), "+¥%.2f", daySum / 100.0) else String.format(Locale.getDefault(), "-¥%.2f", -daySum / 100.0),
                        fontSize = 12.sp,
                        color = if (daySum >= 0) JiTheme.colors.monetGreen else JiTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            items(list) { tx ->
                TransactionItem(
                    transaction = tx,
                    onMigrate = { onMigrate(tx) },
                    onEdit = { onEdit(tx) },
                    onDelete = { onDelete(tx) }
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onMigrate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                    Text(
                        text = transaction.category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            fontSize = 12.sp,
                            color = JiTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (transaction.source == "AUTO_VLM") {
                        Text(
                            text = "自动入账 · ${transaction.platform.name} · ${transaction.paymentKind.defaultCategory}",
                            fontSize = 10.sp,
                            color = JiTheme.colors.monetBlue,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Text(
                    text = String.format(Locale.getDefault(), "%s¥%.2f", if (transaction.type == TransactionType.EXPENSE) "-" else "+", transaction.amount / 100.0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.EXPENSE) JiTheme.colors.textPrimary else JiTheme.colors.monetGreen
                )
            }

            // 折叠的操作菜单（摒弃阴影，自绘扁平极细线条菜单）
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
                        // 迁移按钮
                        Text(
                            text = "迁入额外账单",
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
                                    onMigrate()
                                }
                                .border(0.5.dp, JiTheme.colors.monetBlue, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 编辑按钮
                        Text(
                            text = "编辑",
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
                                    onEdit()
                                }
                                .border(0.5.dp, JiTheme.colors.monetGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 删除按钮
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
fun AddTransactionForm(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit
) {
    val isEditMode = state.editingTransaction != null
    val scrollStateCat = rememberScrollState()
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

        // 收支类型选择器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(JiTheme.colors.cardBackground)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (state.addType == "EXPENSE") JiTheme.colors.stroke else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(HomeUiEvent.TypeChanged("EXPENSE"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "支出",
                    color = if (state.addType == "EXPENSE") JiTheme.colors.background else JiTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (state.addType == "INCOME") JiTheme.colors.stroke else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onEvent(HomeUiEvent.TypeChanged("INCOME"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "收入",
                    color = if (state.addType == "INCOME") JiTheme.colors.background else JiTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 金额输入框
        OutlinedTextField(
            value = state.addAmount,
            onValueChange = { onEvent(HomeUiEvent.AmountChanged(it)) },
            label = { Text("金额 (元)") },
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(amountBringIntoView)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) focusScope.launch { amountBringIntoView.bringIntoView() }
                },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 类别输入框
        OutlinedTextField(
            value = state.addCategory,
            onValueChange = { onEvent(HomeUiEvent.CategoryChanged(it)) },
            label = { Text("分类 (如 餐饮, 交通)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JiTheme.colors.stroke,
                unfocusedBorderColor = JiTheme.colors.divider,
                focusedLabelColor = JiTheme.colors.stroke,
                unfocusedLabelColor = JiTheme.colors.textSecondary,
                focusedTextColor = JiTheme.colors.textPrimary,
                unfocusedTextColor = JiTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(categoryBringIntoView)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) focusScope.launch { categoryBringIntoView.bringIntoView() }
                },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 常用分类快速选择标签芯片
        val categories = if (state.addType == "EXPENSE") {
            listOf("餐饮", "交通", "购物", "娱乐", "居住", "人情", "犒劳", "转账", "红包", "提现", "花呗还款", "白条还款", "其它")
        } else {
            listOf("工资", "理财", "兼职", "红包", "其它")
        }

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
                            onEvent(HomeUiEvent.CategoryChanged(cat))
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

        // 备注输入框
        OutlinedTextField(
            value = state.addNote,
            onValueChange = { onEvent(HomeUiEvent.NoteChanged(it)) },
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(noteBringIntoView)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) focusScope.launch { noteBringIntoView.bringIntoView() }
                },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 自定义日期选择模块
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
            // 今天、昨天、前天快速选择
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
                                onEvent(HomeUiEvent.DateChanged(ts))
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

            // 系统 DatePickerDialog 按钮（带手绘线条图标）
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
                                onEvent(HomeUiEvent.DateChanged(ts))
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

        // 保存按钮
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
                    onEvent(HomeUiEvent.SaveTransaction)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEditMode) "保存修改" else "保存",
                color = JiTheme.colors.stroke,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
