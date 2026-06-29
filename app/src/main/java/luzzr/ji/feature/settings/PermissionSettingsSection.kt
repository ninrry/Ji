package luzzr.ji.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.ji.core.design.CrossIcon
import luzzr.ji.core.design.JiTheme
import luzzr.ji.core.design.TickIcon
import luzzr.ji.core.permissions.PermissionManager

@Composable
fun PermissionSettingsSection(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Text(
        text = "权限获取状态与引导",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = JiTheme.colors.textPrimary
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(JiTheme.colors.cardBackground)
            .padding(vertical = 8.dp)
            .testTag("settings_permission_section")
    ) {
        PermissionItem(
            title = "无障碍服务",
            description = "用于监听微信、支付宝和京东的支付完成页，并自动调用云端 VLM 记账。",
            isEnabled = state.isAccessibilityEnabled,
            onClick = { PermissionManager.openAccessibilitySettings(context) },
            modifier = Modifier.testTag("settings_permission_accessibility")
        )
        PermissionItem(
            title = "Shizuku 静默截图",
            description = "${state.shizukuStatusLabel}。授权后优先静默截取支付完成页；不可用时自动使用无障碍截图兜底。",
            isEnabled = state.isShizukuAuthorized,
            onClick = { onEvent(SettingsUiEvent.RequestShizukuPermission) },
            modifier = Modifier.testTag("settings_permission_shizuku")
        )
        PermissionItem(
            title = "忽略电池优化",
            description = "前台保活服务已启用。允许忽略电池优化可降低系统省电策略回收服务的概率。",
            isEnabled = state.isBatteryOptimizationIgnored,
            onClick = {
                PermissionManager.requestIgnoreBatteryOptimizations(context)
                onEvent(SettingsUiEvent.RequestIgnoreBatteryOptimization)
            },
            modifier = Modifier.testTag("settings_permission_battery")
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
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
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = JiTheme.colors.textSecondary,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (isEnabled) {
                Text(text = "已开启", fontSize = 12.sp, color = JiTheme.colors.monetGreen, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                TickIcon(color = JiTheme.colors.monetGreen, modifier = Modifier.size(16.dp))
            } else {
                Text(text = "去开启", fontSize = 12.sp, color = JiTheme.colors.monetRed, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                CrossIcon(color = JiTheme.colors.monetRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
