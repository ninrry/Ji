package luzzr.ji.feature.extrabill

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import luzzr.ji.core.design.*
import luzzr.ji.domain.model.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExtraPoolOverviewWidget(totalSaved: Long, remaining: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .size(160.dp)
                .border(1.dp, JiTheme.colors.stroke, CircleShape)
                .clip(CircleShape)
                .background(JiTheme.colors.monetYellow.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "省下可用钱数",
                    fontSize = 11.sp,
                    color = JiTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format(Locale.getDefault(), "¥%.2f", remaining / 100.0),
                    fontSize = 24.sp,
                    color = JiTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "历次结余累计 ¥%.0f", totalSaved / 100.0),
                    fontSize = 10.sp,
                    color = JiTheme.colors.textSecondary
                )
            }
        }
    }
}
