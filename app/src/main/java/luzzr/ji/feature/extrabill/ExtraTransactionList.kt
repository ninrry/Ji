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
fun ExtraTransactionList(
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
                text = "还没有额外消费记录\n普通账单长按菜单可迁入此处",
                color = JiTheme.colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        item {
            Text(
                text = "额外账单明细",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(transactions, key = { it.id }) { tx ->
            ExtraTransactionItem(
                transaction = tx,
                onMigrate = { onMigrate(tx) },
                onEdit = { onEdit(tx) },
                onDelete = { onDelete(tx) }
            )
        }
    }
}
