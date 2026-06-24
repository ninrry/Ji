package luzzr.ji.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class JiColors(
    val background: Color = Color(0xFFF4F1EA), // 高级米黄色
    val stroke: Color = Color(0xFF1C1B18),     // 炭黑色线条
    val textPrimary: Color = Color(0xFF1C1B18),
    val textSecondary: Color = Color(0xFF605E59),
    val divider: Color = Color(0xFFD6D3C9),     // 极细分割线
    val monetGreen: Color = Color(0xFF7D9989),  // 莫奈绿
    val monetRed: Color = Color(0xFFC58B8B),    // 莫奈红
    val monetBlue: Color = Color(0xFF8EA4B8),   // 莫奈蓝
    val monetOrange: Color = Color(0xFFC5A28B), // 莫奈橙
    val monetGray: Color = Color(0xFFAFAEA9),
    val monetYellow: Color = Color(0xFFD9C5A0),
    val cardBackground: Color = Color(0xFFFAF9F6) // 稍微浅一些的卡片米白色
)

data class JiShapes(
    val small: Dp = 12.dp,
    val medium: Dp = 24.dp, // 大圆角样式
    val large: Dp = 32.dp   // 大圆角大卡片与弹窗
)

data class JiSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

private val LocalJiColors = staticCompositionLocalOf { JiColors() }
private val LocalJiShapes = staticCompositionLocalOf { JiShapes() }
private val LocalJiSpacing = staticCompositionLocalOf { JiSpacing() }

object JiTheme {
    val colors: JiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalJiColors.current

    val shapes: JiShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalJiShapes.current

    val spacing: JiSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalJiSpacing.current
}

@Composable
fun JiTheme(
    colors: JiColors = JiColors(),
    shapes: JiShapes = JiShapes(),
    spacing: JiSpacing = JiSpacing(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalJiColors provides colors,
        LocalJiShapes provides shapes,
        LocalJiSpacing provides spacing,
        content = content
    )
}
