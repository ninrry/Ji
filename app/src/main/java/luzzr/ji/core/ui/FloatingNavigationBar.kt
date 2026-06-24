package luzzr.ji.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import luzzr.ji.core.design.*

enum class ScreenTab {
    HOME, STATISTICS, EXTRA, SETTINGS
}

@Composable
fun FloatingNavigationBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(64.dp)
            .border(
                width = 1.dp,
                color = JiTheme.colors.stroke,
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(JiTheme.colors.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertCenter
        ) {
            ScreenTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1.0f, label = "tab_scale")
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 彻底关闭系统灰色矩形波纹
                        ) {
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 自定义的选择指示胶囊：大圆角，极细炭黑 Border，明度低莫奈黄色背景
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                                .border(
                                    width = 1.dp,
                                    color = JiTheme.colors.stroke,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(
                                    color = JiTheme.colors.monetYellow.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    }

                    Box(
                        modifier = Modifier.scale(scale),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconColor = if (isSelected) JiTheme.colors.stroke else JiTheme.colors.textSecondary
                        when (tab) {
                            ScreenTab.HOME -> HomeIcon(color = iconColor)
                            ScreenTab.STATISTICS -> StatisticsIcon(color = iconColor)
                            ScreenTab.EXTRA -> ExtraIcon(color = iconColor)
                            ScreenTab.SETTINGS -> SettingsIcon(color = iconColor)
                        }
                    }
                }
            }
        }
    }
}

// 辅助布局对齐
private val Alignment.Companion.CenterVertCenter: Alignment.Vertical
    get() = Alignment.CenterVertically
