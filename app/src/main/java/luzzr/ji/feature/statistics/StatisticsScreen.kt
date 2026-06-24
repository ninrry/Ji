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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun StatisticsRoute(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    StatisticsScreen(
        state = state,
        onSelectDimension = viewModel::selectDimension,
        modifier = modifier
    )
}

@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onSelectDimension: (StatisticsDimension) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(key1 = state.selectedDimension, key2 = state.dailySpends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp) // 避开悬浮底部栏
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. 维度选择 Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(JiTheme.colors.cardBackground)
            ) {
                val dimensions = listOf(
                    StatisticsDimension.WEEK to "周",
                    StatisticsDimension.MONTH to "月",
                    StatisticsDimension.YEAR to "年"
                )
                dimensions.forEach { (dim, label) ->
                    val isSelected = state.selectedDimension == dim
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isSelected) JiTheme.colors.stroke else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSelectDimension(dim)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) JiTheme.colors.background else JiTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 指标分析面板 (小米风格)
            Text(
                text = "${state.dimensionLabel}概览",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "总支出", fontSize = 11.sp, color = JiTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "¥%.1f", state.totalSpend / 100.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.trendComparison, 
                        fontSize = 9.sp, 
                        color = if (state.trendComparison.contains("减少")) JiTheme.colors.monetGreen else JiTheme.colors.monetRed,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(40.dp)
                        .background(JiTheme.colors.divider)
                        .align(Alignment.CenterVertically)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val avgLabel = when (state.selectedDimension) {
                        StatisticsDimension.WEEK -> "日均支出"
                        StatisticsDimension.MONTH -> "日均支出"
                        StatisticsDimension.YEAR -> "月均支出"
                    }
                    Text(text = avgLabel, fontSize = 11.sp, color = JiTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "¥%.1f", state.averageSpend / 100.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(40.dp)
                        .background(JiTheme.colors.divider)
                        .align(Alignment.CenterVertically)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "单笔最高", fontSize = 11.sp, color = JiTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "¥%.1f", state.maxSpend / 100.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 走势折线图
            Text(
                text = "支出走势图",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(JiTheme.colors.cardBackground)
                    .padding(16.dp)
            ) {
                DailySpendLineChart(
                    dailySpends = state.dailySpends,
                    progress = animationProgress.value
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 环形占比图
            if (state.categorySpends.isNotEmpty()) {
                Text(
                    text = "消费类别占比",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = JiTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(JiTheme.colors.cardBackground)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryPieChart(
                            categorySpends = state.categorySpends,
                            progress = animationProgress.value
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    val colors = JiTheme.colors
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        state.categorySpends.take(4).forEachIndexed { index, item ->
                            val color = getMonetColor(index, colors)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category,
                                        fontSize = 12.sp,
                                        color = JiTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", item.percentage * 100),
                                    fontSize = 12.sp,
                                    color = JiTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. 柱状排行榜
                Text(
                    text = "分类支出排行榜",
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
                    val colors = JiTheme.colors
                    state.categorySpends.forEachIndexed { index, item ->
                        CategoryBarRow(
                            categorySpend = item,
                            color = getMonetColor(index, colors),
                            progress = animationProgress.value
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(1.dp, JiTheme.colors.stroke, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(JiTheme.colors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前维度暂无支出数据\n无法生成统计图表",
                        color = JiTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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

@Composable
fun CategoryPieChart(categorySpends: List<CategorySpend>, progress: Float) {
    val colors = JiTheme.colors
    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        categorySpends.forEachIndexed { index, item ->
            val sweepAngle = item.percentage * 360f * progress
            val color = getMonetColor(index, colors)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
            startAngle += item.percentage * 360f
        }
    }
}

@Composable
fun CategoryBarRow(categorySpend: CategorySpend, color: Color, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categorySpend.category,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary
            )
            Text(
                text = String.format(Locale.getDefault(), "¥%.2f (%.1f%%)", categorySpend.amount / 100.0, categorySpend.percentage * 100),
                fontSize = 12.sp,
                color = JiTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .border(0.5.dp, JiTheme.colors.stroke, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(JiTheme.colors.background)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barWidth = width * categorySpend.percentage * progress
                drawRoundRect(
                    color = color,
                    size = Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
                )
            }
        }
    }
}

fun getMonetColor(index: Int, colors: luzzr.ji.core.design.JiColors): Color {
    val list = listOf(
        colors.monetGreen,
        colors.monetBlue,
        colors.monetOrange,
        colors.monetYellow,
        colors.monetRed,
        colors.monetGray
    )
    return list[index % list.size]
}
