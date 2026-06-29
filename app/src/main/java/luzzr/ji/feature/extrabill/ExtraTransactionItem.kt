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
fun ExtraTransactionItem(
    transaction: Transaction,
    onMigrate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val localDate = Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val displayDate = localDate.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 0.5.dp,
                color = JiTheme.colors.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(JiTheme.colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showMenu = !showMenu
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JiTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayDate,
                            fontSize = 10.sp,
                            color = JiTheme.colors.textSecondary
                        )
                    }
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            fontSize = 12.sp,
                            color = JiTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Text(
                    text = String.format(Locale.getDefault(), "-¥%.2f", transaction.amount / 100.0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JiTheme.colors.textPrimary
                )
            }

            AnimatedVisibility(
                visible = showMenu,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(JiTheme.colors.divider)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "迁回普通账单",
                            color = JiTheme.colors.monetGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onMigrate()
                                }
                                .border(0.5.dp, JiTheme.colors.monetGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "编辑",
                            color = JiTheme.colors.monetBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onEdit()
                                }
                                .border(0.5.dp, JiTheme.colors.monetBlue, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "删除",
                            color = JiTheme.colors.monetRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMenu = false
                                    onDelete()
                                }
                                .border(0.5.dp, JiTheme.colors.monetRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
