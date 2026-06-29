package luzzr.ji.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["source", "platform", "paymentKind", "amount", "occurredAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val note: String,
    val timestamp: Long,
    val isExtra: Boolean,
    val source: String = "MANUAL",
    val platform: String = "MANUAL",
    val paymentKind: String = "MERCHANT_PAYMENT",
    val tradeId: String? = null,
    val occurredAt: Long = timestamp,
    val dedupKey: String? = null
)
