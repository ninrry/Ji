package luzzr.ji.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.85f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun StatisticsIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // 绘制三根极简柱体轮廓 (只是折线)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.9f), end = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.9f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        
        // 柱子 1
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.55f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.35f), style = Stroke(width = 2.dp.toPx()))
        // 柱子 2
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.6f), style = Stroke(width = 2.dp.toPx()))
        // 柱子 3
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.45f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.45f), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun ExtraIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // 绘制一个小钱袋的极简线条，或者两个同心大弧线表达额度池
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.3f)
            quadraticTo(w * 0.5f, h * 0.15f, w * 0.7f, h * 0.3f)
            quadraticTo(w * 0.85f, h * 0.6f, w * 0.75f, h * 0.85f)
            lineTo(w * 0.25f, h * 0.85f)
            quadraticTo(w * 0.15f, h * 0.6f, w * 0.3f, h * 0.3f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        
        // 钱袋中间的一横
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.55f), end = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.55f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.65f), end = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.65f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun SettingsIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // 绘制三个错落的线条同心圆圈，类似于控制板滑块
        drawCircle(color = color, radius = w * 0.12f, center = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.3f), style = Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.3f), end = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.3f), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.3f), end = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.3f), strokeWidth = 2.dp.toPx())

        drawCircle(color = color, radius = w * 0.12f, center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.7f), style = Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.7f), end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.7f), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.7f), end = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.7f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun PlusIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.5f), end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.5f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f), end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.8f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.6f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.75f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CalendarIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // 绘制一个小日历边框
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.6f), style = Stroke(width = 2.dp.toPx()))
        // 日历上面的小提耳
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.15f), end = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.25f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.15f), end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.25f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        // 日历内分割横线
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.45f), end = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.45f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun ArrowRightIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.4f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.4f, h * 0.75f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun TickIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.7f)
            lineTo(w * 0.8f, h * 0.3f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CrossIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.25f), end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.75f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.25f), end = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.75f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun CameraIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        
        // 机身与镜头抽象外壳
        val bodyPath = Path().apply {
            moveTo(w * 0.15f, h * 0.4f)
            lineTo(w * 0.35f, h * 0.4f)
            lineTo(w * 0.4f, h * 0.23f)
            lineTo(w * 0.6f, h * 0.23f)
            lineTo(w * 0.65f, h * 0.4f)
            lineTo(w * 0.85f, h * 0.4f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.85f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
        
        // 中间镜头
        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.62f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun PhoneIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF1C1B18)) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        
        // 手机外壳
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.7f),
            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // 顶部听筒
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.24f),
            end = androidx.compose.ui.geometry.Offset(w * 0.56f, h * 0.24f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // 底部指示条/键
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.76f),
            end = androidx.compose.ui.geometry.Offset(w * 0.56f, h * 0.76f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

