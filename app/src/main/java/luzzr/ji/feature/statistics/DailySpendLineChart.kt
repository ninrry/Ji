package luzzr.ji.feature.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luzzr.ji.core.design.JiTheme
import java.util.Locale

@Composable
fun DailySpendLineChart(dailySpends: List<DailySpend>, progress: Float) {
    val strokeColor = JiTheme.colors.stroke
    val fillColor = JiTheme.colors.monetGreen.copy(alpha = 0.15f)
    val bgColor = JiTheme.colors.background

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (dailySpends.isEmpty()) return@Canvas

        val maxAmount = dailySpends.maxOf { it.amount }.coerceAtLeast(1000L) // 最小 10元

        drawLine(
            color = strokeColor.copy(alpha = 0.1f),
            start = Offset(0f, height * 0.9f),
            end = Offset(width, height * 0.9f),
            strokeWidth = 1.dp.toPx()
        )

        val chartWidth = width
        val chartHeight = height * 0.8f
        val startY = height * 0.9f

        val stepX = if (dailySpends.size > 1) chartWidth / (dailySpends.size - 1) else chartWidth

        val path = Path()
        val fillPath = Path()

        val drawCount = (dailySpends.size * progress).toInt().coerceIn(1, dailySpends.size)

        dailySpends.take(drawCount).forEachIndexed { index, daily ->
            val x = index * stepX
            val y = startY - ((daily.amount.toDouble() / maxAmount) * chartHeight).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, startY)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = startY - ((dailySpends[index - 1].amount.toDouble() / maxAmount) * chartHeight).toFloat()
                val controlX1 = prevX + stepX / 2f
                val controlY1 = prevY
                val controlX2 = prevX + stepX / 2f
                val controlY2 = y
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }

            if (index == drawCount - 1) {
                fillPath.lineTo(x, startY)
                fillPath.close()
            }
        }

        if (drawCount > 1) {
            drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 绘制节点小圆圈
        dailySpends.take(drawCount).forEachIndexed { index, daily ->
            if (daily.amount > 0L) {
                val x = index * stepX
                val y = startY - ((daily.amount.toDouble() / maxAmount) * chartHeight).toFloat()
                drawCircle(
                    color = strokeColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = bgColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
