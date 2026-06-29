package luzzr.ji.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import luzzr.ji.core.design.CameraIcon
import luzzr.ji.core.design.JiTheme
import luzzr.ji.core.design.PhoneIcon

@Composable
fun VlmChatDebugSection(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    ) { onEvent(SettingsUiEvent.ClearChat) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("settings_clear_chat")
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(JiTheme.colors.cardBackground)
            .padding(16.dp)
            .testTag("settings_vlm_chat_section")
    ) {
        ChatHistoryPanel(state = state)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(JiTheme.colors.divider)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ChatImageActions(state = state, onEvent = onEvent)
        ChatInputRow(state = state, onEvent = onEvent)
    }
}

@Composable
private fun ChatHistoryPanel(state: SettingsUiState) {
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
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.chatHistory.forEach { message ->
            ChatBubble(message)
        }
        if (state.isChatLoading) {
            LoadingBubble()
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
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
                .background(if (isUser) JiTheme.colors.background else JiTheme.colors.monetGreen.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Column {
                message.imageLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, JiTheme.colors.divider, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(JiTheme.colors.monetGreen.copy(alpha = 0.08f))
                            .padding(10.dp)
                    ) {
                        Text(text = label, fontSize = 11.sp, color = JiTheme.colors.textSecondary)
                    }
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

@Composable
private fun LoadingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .border(1.dp, JiTheme.colors.divider, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(JiTheme.colors.monetGreen.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Text(text = "正在思考中...", fontSize = 12.sp, color = JiTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun ChatImageActions(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit
) {
    val localView = LocalView.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatImageButton(
            text = "发送测试账单",
            enabled = !state.isChatLoading,
            icon = { CameraIcon(color = JiTheme.colors.textPrimary) },
            modifier = Modifier.weight(1f).testTag("settings_send_test_bill")
        ) {
            scope.launch {
                val bytes = withContext(Dispatchers.Default) { SettingsDebugImageFactory.createTestBillJpeg() }
                onEvent(SettingsUiEvent.SendChatImageMessage(bytes, "请高精度识别这张测试账单图片上的消费金额、商户和消费明细。"))
            }
        }
        ChatImageButton(
            text = "发送当前截图",
            enabled = !state.isChatLoading,
            icon = { PhoneIcon(color = JiTheme.colors.textPrimary) },
            modifier = Modifier.weight(1f).testTag("settings_send_current_screenshot")
        ) {
            scope.launch {
                val bytes = withContext(Dispatchers.Default) { SettingsDebugImageFactory.createViewScreenshotJpeg(localView) }
                onEvent(SettingsUiEvent.SendChatImageMessage(bytes, "请描述一下这张当前界面截图展示了什么内容。"))
            }
        }
    }
}

@Composable
private fun ChatImageButton(
    text: String,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) JiTheme.colors.monetGreen.copy(alpha = 0.1f) else JiTheme.colors.divider)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, color = JiTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChatInputRow(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.chatInput,
            onValueChange = { onEvent(SettingsUiEvent.ChatInputChanged(it)) },
            placeholder = { Text("输入测试内容...", fontSize = 13.sp) },
            colors = settingsTextFieldColors(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).testTag("settings_chat_input"),
            maxLines = 3
        )
        Spacer(modifier = Modifier.width(12.dp))
        val canSend = state.chatInput.isNotBlank() && !state.isChatLoading
        Box(
            modifier = Modifier
                .height(56.dp)
                .width(80.dp)
                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(if (canSend) JiTheme.colors.monetGreen else JiTheme.colors.divider)
                .clickable(
                    enabled = canSend,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEvent(SettingsUiEvent.SendChatMessage) }
                .testTag("settings_send_chat"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "发送",
                color = if (canSend) JiTheme.colors.stroke else JiTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
