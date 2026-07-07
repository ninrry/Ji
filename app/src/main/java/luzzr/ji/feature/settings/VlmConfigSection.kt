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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.ji.core.design.JiTheme
import luzzr.ji.core.vlm.VlmProvider

@Composable
fun VlmConfigSection(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = "云端识别服务",
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
            .testTag("settings_vlm_config_section")
    ) {
        Text(
            text = "配置云端识别服务。自动识别时，支付完成页截图和文本会发送到下方服务地址；请只使用你信任的 HTTPS 服务。",
            fontSize = 12.sp,
            color = JiTheme.colors.textSecondary,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Provider selector
        Text(
            text = "服务供应商",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JiTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VlmProvider.entries.forEach { provider ->
                val isSelected = state.vlmProvider == provider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .border(
                            1.dp,
                            if (isSelected) JiTheme.colors.monetGreen else JiTheme.colors.stroke,
                            RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) JiTheme.colors.monetGreen.copy(alpha = 0.15f)
                            else JiTheme.colors.cardBackground
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onEvent(SettingsUiEvent.ProviderChanged(provider)) }
                        .testTag("settings_provider_${provider.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) JiTheme.colors.monetGreen else JiTheme.colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // API URL
        OutlinedTextField(
            value = state.opencodeApiUrl,
            onValueChange = { onEvent(SettingsUiEvent.ApiUrlChanged(it)) },
            label = { Text("云端服务地址（HTTPS）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = settingsTextFieldColors(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("settings_api_url_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // API Key
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.opencodeApiKey,
                onValueChange = { onEvent(SettingsUiEvent.ApiKeyChanged(it)) },
                label = { Text("API 密钥") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = settingsTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("settings_api_key_input"),
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
                    ) { onEvent(SettingsUiEvent.SaveApiKey) }
                    .testTag("settings_save_api_key"),
                contentAlignment = Alignment.Center
            ) {
                Text("保存", color = JiTheme.colors.stroke, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model display
        Text(
            text = "已选模型",
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
                text = "${state.opencodeModel} via ${state.vlmProvider.displayName}",
                fontSize = 12.sp,
                color = JiTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Cost hint for Xiaomi pay-as-you-go
        if (state.vlmProvider == VlmProvider.XIAOMI) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "按量计费: mimo-v2.5 输入 1/MTok, 输出 2/MTok",
                fontSize = 11.sp,
                color = JiTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(if (state.isTestingConnection) JiTheme.colors.divider else JiTheme.colors.cardBackground)
                .clickable(
                    enabled = !state.isTestingConnection,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEvent(SettingsUiEvent.TestConnection) }
                .testTag("settings_test_connection"),
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
}
