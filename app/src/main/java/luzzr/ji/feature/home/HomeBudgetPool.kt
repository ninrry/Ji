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
fun BudgetPoolWidget(budget: Long, expense: Long) {
    val remaining = (budget - expense).coerceAtLeast(0L)
    val remainingPercent = if (budget > 0) (remaining.toDouble() / budget).coerceIn(0.0, 1.0) else 0.0

    // 水波波浪动画，利用 infiniteTransition 产生流动的相位
    val infiniteTransition = rememberInfiniteTransition(label = "water_flow")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val strokeColor = JiTheme.colors.stroke
    val waterColor = if (remainingPercent > 0.2f) JiTheme.colors.monetGreen else JiTheme.colors.monetRed
    val poolBgColor = JiTheme.colors.cardBackground

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(1.dp, strokeColor, CircleShape)
                .clip(CircleShape)
                .background(poolBgColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val circlePath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
                }

                // 剩余额度百分比对应水位高度 (0 代表满，1 代表空)
                val targetWaterY = height * (1f - remainingPercent.toFloat())

                clipPath(circlePath) {
                    val wavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, targetWaterY)
                        
                        // 正弦波形生成，平滑波动
                        val steps = 100
                        val waveLength = width
                        val amplitude = 8.dp.toPx()
                        for (i in 0..steps) {
                            val x = (i.toFloat() / steps) * width
                            val y = targetWaterY + amplitude * sin((2 * Math.PI * x / waveLength) + wavePhase).toFloat()
                            lineTo(x, y)
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(path = wavePath, color = waterColor.copy(alpha = 0.5f))
                }
            }

            // 额度数值信息
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "剩余额度池",
                    fontSize = 11.sp,
                    color = JiTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.getDefault(), "¥%.0f", remaining / 100.0),
                    fontSize = 24.sp,
                    color = JiTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "总预算 ¥%.0f", budget / 100.0),
                    fontSize = 10.sp,
                    color = JiTheme.colors.textSecondary
                )
            }
        }
    }
}
