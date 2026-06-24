package luzzr.ji.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.repository.BudgetRepository
import luzzr.ji.domain.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class ExtraBillOverview(
    val totalPoolAmount: Long,
    val extraSpendAmount: Long,
    val remainingPoolAmount: Long
)

class GetExtraBillOverviewUseCase(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    operator fun invoke(currentMillis: Long = System.currentTimeMillis(), defaultMonthlyBudget: Long = 0L): Flow<ExtraBillOverview> {
        return combine(
            transactionRepository.observeAllTransactions(),
            budgetRepository.observeAllBudgets()
        ) { transactions, budgets ->
            calculateOverview(transactions, budgets, currentMillis, defaultMonthlyBudget)
        }
    }

    fun calculateOverview(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        currentMillis: Long,
        defaultMonthlyBudget: Long
    ): ExtraBillOverview {
        val currentLocalDate = Instant.ofEpochMilli(currentMillis).atZone(zoneId).toLocalDate()
        val currentYearMonth = YearMonth.from(currentLocalDate)

        // 1. 过滤出所有支出普通账单 (isExtra = false, type = EXPENSE)
        val normalExpenses = transactions.filter { !it.isExtra && it.type == TransactionType.EXPENSE }

        // 2. 找到最早有普通支出的月份，若是没有，则为空
        if (normalExpenses.isEmpty()) {
            // 没有记过账，已过历史月份结余为 0，已用额外支出为额外账单的支出总和
            val extraSpend = transactions.filter { it.isExtra && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            return ExtraBillOverview(0L, extraSpend, -extraSpend)
        }

        val earliestDate = normalExpenses.minOf { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
        val earliestYearMonth = YearMonth.from(earliestDate)

        // 预算 Map
        val budgetMap = budgets.associate { it.yearMonth to it.amount }

        var totalPool = 0L

        // 3. 循环计算从最早记账月份开始，到当前月份前一个月的合格历史月份
        var tempMonth = earliestYearMonth
        while (tempMonth.isBefore(currentYearMonth)) {
            val ymString = tempMonth.toString() // "yyyy-MM"
            val monthTransactions = normalExpenses.filter {
                val ym = YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate())
                ym == tempMonth
            }

            // 统计普通账单发生消费的日期去重数
            val uniqueDays = monthTransactions.map {
                Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().dayOfMonth
            }.toSet().size

            if (uniqueDays >= 10) {
                // 消费满十日，计算结余
                val monthlyBudget = budgetMap[ymString] ?: defaultMonthlyBudget
                val totalExpenseInMonth = monthTransactions.sumOf { it.amount }
                val balance = monthlyBudget - totalExpenseInMonth
                totalPool += balance
            } else {
                // 消费不满十日，不计算该月结余
                totalPool += 0L
            }

            tempMonth = tempMonth.plusMonths(1)
        }

        // 4. 计算额外账单的累计支出
        val extraSpend = transactions.filter { it.isExtra && it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return ExtraBillOverview(
            totalPoolAmount = totalPool,
            extraSpendAmount = extraSpend,
            remainingPoolAmount = totalPool - extraSpend
        )
    }
}
