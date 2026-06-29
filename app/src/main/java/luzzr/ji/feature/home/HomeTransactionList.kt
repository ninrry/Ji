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
fun TransactionGroupList(
    transactions: List<Transaction>,
    onMigrate: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "本月暂无记账数据\n点击下方 + 开始记账",
                color = JiTheme.colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        return
    }

    // 按日期分组 (例如 "2026-06-23")
    val grouped = remember(transactions) {
        transactions.groupBy {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            date.toString()
        }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        grouped.forEach { (dateStr, list) ->
            // 分割头部
            item {
                val localDate = LocalDate.parse(dateStr)
                val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
                val displayDate = localDate.format(formatter)

                val daySum = list.sumOf { if (it.type == TransactionType.EXPENSE) -it.amount else it.amount }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JiTheme.colors.textPrimary
                    )
                    Text(
                        text = if (daySum >= 0) String.format(Locale.getDefault(), "+¥%.2f", daySum / 100.0) else String.format(Locale.getDefault(), "-¥%.2f", -daySum / 100.0),
                        fontSize = 12.sp,
                        color = if (daySum >= 0) JiTheme.colors.monetGreen else JiTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            items(list, key = { it.id }) { tx ->
                TransactionItem(
                    transaction = tx,
                    onMigrate = { onMigrate(tx) },
                    onEdit = { onEdit(tx) },
                    onDelete = { onDelete(tx) }
                )
            }
        }
    }
}
