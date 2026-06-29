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
import androidx.compose.ui.platform.testTag
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
            .testTag("home_screen")
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
                    .testTag("home_add_button")
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
