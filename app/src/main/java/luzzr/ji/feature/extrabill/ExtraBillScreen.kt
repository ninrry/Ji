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
import androidx.compose.ui.platform.testTag
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
            .testTag("extra_bill_screen")
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
                    .testTag("extra_add_button")
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
