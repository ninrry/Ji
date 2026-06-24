package luzzr.ji.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val yearMonth: String, // format: "yyyy-MM"
    val amount: Long
)
