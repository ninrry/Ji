package luzzr.ji.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.collectLatest
import luzzr.ji.core.permissions.PermissionManager
import luzzr.ji.core.design.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import java.io.ByteArrayOutputStream

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 回到前台时刷新无障碍服务状态。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(key1 = viewModel.uiEffect) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is SettingsUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onEvent = { viewModel.onEvent(it, context) },
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "系统设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. 每月预算设定卡片
            Text(
                text = "月度收支预算",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(16.dp)
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
                        modifier = Modifier.weight(1f),
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
                            ) {
                                onEvent(SettingsUiEvent.SaveBudget)
                            },
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
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.errorMessage,
                        color = JiTheme.colors.monetRed,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1.5 OpenCode Go 智能订阅卡片
            Text(
                text = "OpenCode Go 智能订阅",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(16.dp)
            ) {
                Text(
                    text = "配置云端识别服务。自动识别时，支付完成页截图、页面文本、交易金额和交易号可能会发送到下方服务地址；请只使用你信任的 HTTPS 服务。",
                    fontSize = 12.sp,
                    color = JiTheme.colors.textSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.opencodeApiUrl,
                    onValueChange = { onEvent(SettingsUiEvent.ApiUrlChanged(it)) },
                    label = { Text("云端服务地址（HTTPS）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.opencodeApiKey,
                        onValueChange = { onEvent(SettingsUiEvent.ApiKeyChanged(it)) },
                        label = { Text("OpenCode API 密钥") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JiTheme.colors.stroke,
                            unfocusedBorderColor = JiTheme.colors.divider,
                            focusedLabelColor = JiTheme.colors.stroke,
                            unfocusedLabelColor = JiTheme.colors.textSecondary,
                            focusedTextColor = JiTheme.colors.textPrimary,
                            unfocusedTextColor = JiTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
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
                            ) {
                                onEvent(SettingsUiEvent.SaveApiKey)
                            },
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "已选 VLM 智能模型",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = JiTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(JiTheme.colors.monetGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "MiMo V2.5 (仅支持此唯一图像多模态模型)",
                        fontSize = 12.sp,
                        color = JiTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (state.isTestingConnection) JiTheme.colors.divider
                            else JiTheme.colors.cardBackground
                        )
                        .clickable(
                            enabled = !state.isTestingConnection,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onEvent(SettingsUiEvent.TestConnection)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isTestingConnection) "正在测试连接..." else "测试模型连接",
                        color = JiTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                state.connectionTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val isSuccess = result.startsWith("Success")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp, 
                                if (isSuccess) JiTheme.colors.monetGreen else JiTheme.colors.monetRed, 
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                if (isSuccess) JiTheme.colors.monetGreen.copy(alpha = 0.05f) 
                                else JiTheme.colors.monetRed.copy(alpha = 0.05f)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = result,
                            color = if (isSuccess) JiTheme.colors.monetGreen else JiTheme.colors.monetRed,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1.6 VLM 对话测试调试卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VLM 对话测试",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = JiTheme.colors.textPrimary
                )
                if (state.chatHistory.isNotEmpty()) {
                    Text(
                        text = "清空对话",
                        fontSize = 12.sp,
                        color = JiTheme.colors.monetRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onEvent(SettingsUiEvent.ClearChat)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(16.dp)
            ) {
                if (state.chatHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无对话记录。可在下方发送文字或测试图片与 MiMo V2.5 对话。",
                            fontSize = 12.sp,
                            color = JiTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.chatHistory.forEach { message ->
                            val isUser = message.sender == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isUser) JiTheme.colors.stroke else JiTheme.colors.divider,
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) JiTheme.colors.background
                                            else JiTheme.colors.monetGreen.copy(alpha = 0.1f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        val bitmap = remember(message.imageBytes) {
                                            message.imageBytes?.let { bytes ->
                                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Test Image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(135.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.dp, JiTheme.colors.divider, RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        Text(
                                            text = message.text,
                                            fontSize = 12.sp,
                                            color = JiTheme.colors.textPrimary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (state.isChatLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = JiTheme.colors.divider,
                                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                        )
                                        .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                        .background(JiTheme.colors.monetGreen.copy(alpha = 0.05f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "正在思考中...",
                                        fontSize = 12.sp,
                                        color = JiTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(JiTheme.colors.divider)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 快捷多模态图片测试按钮 (零波纹、大圆角)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localView = LocalView.current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.isChatLoading) JiTheme.colors.divider else JiTheme.colors.monetGreen.copy(alpha = 0.1f))
                            .clickable(
                                enabled = !state.isChatLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val testBitmap = createTestBillBitmap()
                                val stream = ByteArrayOutputStream()
                                testBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                onEvent(SettingsUiEvent.SendChatImageMessage(stream.toByteArray(), "请高精度识别这张测试账单图片上的消费金额、商户和消费明细。"))
                                testBitmap.recycle()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CameraIcon(color = JiTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("发送测试账单", fontSize = 11.sp, color = JiTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.isChatLoading) JiTheme.colors.divider else JiTheme.colors.monetGreen.copy(alpha = 0.1f))
                            .clickable(
                                enabled = !state.isChatLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    val screenshotBitmap = createViewScreenshot(localView)
                                    val scaledBitmap = scaleBitmapDown(screenshotBitmap, 720)
                                    val stream = ByteArrayOutputStream()
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                                    onEvent(SettingsUiEvent.SendChatImageMessage(stream.toByteArray(), "请描述一下这张当前界面截图展示了什么内容。"))
                                    if (scaledBitmap != screenshotBitmap) {
                                        scaledBitmap.recycle()
                                    }
                                    screenshotBitmap.recycle()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            PhoneIcon(color = JiTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("发送当前截图", fontSize = 11.sp, color = JiTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.chatInput,
                        onValueChange = { onEvent(SettingsUiEvent.ChatInputChanged(it)) },
                        placeholder = { Text("输入测试内容...", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JiTheme.colors.stroke,
                            unfocusedBorderColor = JiTheme.colors.divider,
                            focusedLabelColor = JiTheme.colors.stroke,
                            unfocusedLabelColor = JiTheme.colors.textSecondary,
                            focusedTextColor = JiTheme.colors.textPrimary,
                            unfocusedTextColor = JiTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .width(80.dp)
                            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (state.chatInput.isBlank() || state.isChatLoading) JiTheme.colors.divider
                                else JiTheme.colors.monetGreen
                            )
                            .clickable(
                                enabled = state.chatInput.isNotBlank() && !state.isChatLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onEvent(SettingsUiEvent.SendChatMessage)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "发送",
                            color = if (state.chatInput.isBlank() || state.isChatLoading) JiTheme.colors.textSecondary else JiTheme.colors.stroke,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 自动记账只需要无障碍服务。
            Text(
                text = "权限获取状态与引导",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(vertical = 8.dp)
            ) {
                PermissionItem(
                    title = "无障碍服务",
                    description = "用于监听微信、支付宝和京东的支付完成页，并自动调用云端 VLM 记账。",
                    isEnabled = state.isAccessibilityEnabled,
                    onClick = { PermissionManager.openAccessibilitySettings(context) }
                )
                PermissionItem(
                    title = "Shizuku 静默截图",
                    description = "${state.shizukuStatusLabel}。授权后优先静默截取支付完成页；不可用时自动使用无障碍截图兜底。",
                    isEnabled = state.isShizukuAuthorized,
                    onClick = { onEvent(SettingsUiEvent.RequestShizukuPermission) }
                )
                PermissionItem(
                    title = "忽略电池优化",
                    description = "前台保活服务已启用。允许忽略电池优化可降低系统省电策略回收服务的概率。",
                    isEnabled = state.isBatteryOptimizationIgnored,
                    onClick = { onEvent(SettingsUiEvent.RequestIgnoreBatteryOptimization) }
                )
            }
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = JiTheme.colors.textSecondary,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        // 状态标识区 (已开启显示绿色勾，未开启显示红色叉并提示去开启)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (isEnabled) {
                Text(
                    text = "已开启",
                    fontSize = 12.sp,
                    color = JiTheme.colors.monetGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                TickIcon(color = JiTheme.colors.monetGreen, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = "去开启",
                    fontSize = 12.sp,
                    color = JiTheme.colors.monetRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                CrossIcon(color = JiTheme.colors.monetRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun createTestBillBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    
    // 莫奈米黄背景
    paint.color = AndroidColor.parseColor("#F4F1EA")
    canvas.drawRect(0f, 0f, 400f, 300f, paint)
    
    // 1dp极细细边框
    paint.color = AndroidColor.parseColor("#D5CFC1")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawRect(10f, 10f, 390f, 290f, paint)
    
    // 标题商户
    paint.color = AndroidColor.parseColor("#433D35")
    paint.style = Paint.Style.FILL
    paint.textSize = 24f
    paint.isAntiAlias = true
    canvas.drawText("Lush 莫奈咖啡馆", 40f, 60f, paint)
    
    // 分割线
    paint.color = AndroidColor.parseColor("#D5CFC1")
    paint.strokeWidth = 1f
    canvas.drawLine(40f, 85f, 360f, 85f, paint)
    
    // 消费项
    paint.color = AndroidColor.parseColor("#433D35")
    paint.textSize = 16f
    canvas.drawText("手冲莫奈咖啡     x 2    ￥76.00", 40f, 120f, paint)
    canvas.drawText("法式拿破仑酥     x 1    ￥52.00", 40f, 160f, paint)
    
    // 支付方式
    paint.color = AndroidColor.parseColor("#8E887E")
    paint.textSize = 13f
    canvas.drawText("支付方式: 微信免密支付", 40f, 200f, paint)
    
    // 金额
    paint.color = AndroidColor.parseColor("#433D35")
    paint.textSize = 34f
    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
    canvas.drawText("￥128.00", 40f, 260f, paint)
    
    return bitmap
}

private fun createViewScreenshot(view: android.view.View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    var newWidth = originalWidth
    var newHeight = originalHeight

    if (originalWidth > originalHeight) {
        if (originalWidth > maxDimension) {
            newWidth = maxDimension
            newHeight = (newWidth * originalHeight) / originalWidth
        }
    } else {
        if (originalHeight > maxDimension) {
            newHeight = maxDimension
            newWidth = (newHeight * originalWidth) / originalHeight
        }
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
