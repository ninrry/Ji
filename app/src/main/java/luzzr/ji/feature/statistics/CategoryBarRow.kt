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
