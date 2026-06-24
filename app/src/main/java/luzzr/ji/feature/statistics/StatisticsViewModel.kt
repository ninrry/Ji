package luzzr.ji.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import luzzr.ji.domain.model.Transaction
import luzzr.ji.domain.model.TransactionType
import luzzr.ji.domain.usecase.ObserveTransactionsUseCase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class StatisticsViewModel(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _selectedDimension = MutableStateFlow(StatisticsDimension.MONTH)
    val selectedDimension: StateFlow<StatisticsDimension> = _selectedDimension.asStateFlow()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        combine(
            observeTransactionsUseCase(),
            _selectedDimension
        ) { transactions, dimension ->
            val now = LocalDate.now(zoneId)
            val expenses = transactions.filter { !it.isExtra && it.type == TransactionType.EXPENSE }

            calculateState(expenses, dimension, now)
        }
        .onEach { state ->
            _uiState.value = state
        }
        .launchIn(viewModelScope)
    }

    fun selectDimension(dimension: StatisticsDimension) {
        _selectedDimension.value = dimension
    }

    private fun calculateState(
        expenses: List<Transaction>,
        dimension: StatisticsDimension,
        now: LocalDate
    ): StatisticsUiState {
        return when (dimension) {
            StatisticsDimension.WEEK -> calculateWeekState(expenses, now)
            StatisticsDimension.MONTH -> calculateMonthState(expenses, now)
            StatisticsDimension.YEAR -> calculateYearState(expenses, now)
        }
    }

    private fun calculateWeekState(expenses: List<Transaction>, now: LocalDate): StatisticsUiState {
        val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        val startOfLastWeek = startOfWeek.minusWeeks(1)
        val endOfLastWeek = startOfLastWeek.plusDays(6)

        val thisWeekExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }
        val lastWeekExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfLastWeek) && !date.isAfter(endOfLastWeek)
        }

        val totalSpend = thisWeekExpenses.sumOf { it.amount }
        val lastWeekTotal = lastWeekExpenses.sumOf { it.amount }

        val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val dailyMap = thisWeekExpenses.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().dayOfWeek.value
        }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val dailySpends = dayNames.mapIndexed { idx, name ->
            DailySpend(name, dailyMap[idx + 1] ?: 0L)
        }

        val categorySpends = calculateCategorySpends(thisWeekExpenses)
        val spentDays = thisWeekExpenses.map {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
        }.distinct().size
        val averageSpend = if (spentDays > 0) totalSpend.toDouble() / spentDays else 0.0
        val maxSpend = thisWeekExpenses.maxOfOrNull { it.amount } ?: 0L
        val trendComparison = formatTrendComparison(totalSpend, lastWeekTotal, "上周")

        return StatisticsUiState(
            selectedDimension = StatisticsDimension.WEEK,
            dailySpends = dailySpends,
            categorySpends = categorySpends,
            totalSpend = totalSpend,
            averageSpend = averageSpend,
            maxSpend = maxSpend,
            trendComparison = trendComparison,
            dimensionLabel = "本周支出"
        )
    }

    private fun calculateMonthState(expenses: List<Transaction>, now: LocalDate): StatisticsUiState {
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        val startOfLastMonth = startOfMonth.minusMonths(1)
        val endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth())

        val thisMonthExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfMonth) && !date.isAfter(endOfMonth)
        }
        val lastMonthExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfLastMonth) && !date.isAfter(endOfLastMonth)
        }

        val totalSpend = thisMonthExpenses.sumOf { it.amount }
        val lastMonthTotal = lastMonthExpenses.sumOf { it.amount }

        val daysInMonth = now.lengthOfMonth()
        val dailyMap = thisMonthExpenses.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().dayOfMonth
        }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val dailySpends = (1..daysInMonth).map { day ->
            DailySpend("${day}日", dailyMap[day] ?: 0L)
        }

        val categorySpends = calculateCategorySpends(thisMonthExpenses)
        val spentDays = thisMonthExpenses.map {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
        }.distinct().size
        val averageSpend = if (spentDays > 0) totalSpend.toDouble() / spentDays else 0.0
        val maxSpend = thisMonthExpenses.maxOfOrNull { it.amount } ?: 0L
        val trendComparison = formatTrendComparison(totalSpend, lastMonthTotal, "上月")

        return StatisticsUiState(
            selectedDimension = StatisticsDimension.MONTH,
            dailySpends = dailySpends,
            categorySpends = categorySpends,
            totalSpend = totalSpend,
            averageSpend = averageSpend,
            maxSpend = maxSpend,
            trendComparison = trendComparison,
            dimensionLabel = "本月支出"
        )
    }

    private fun calculateYearState(expenses: List<Transaction>, now: LocalDate): StatisticsUiState {
        val startOfYear = now.withDayOfYear(1)
        val endOfYear = now.withDayOfYear(now.lengthOfYear())

        val startOfLastYear = startOfYear.minusYears(1)
        val endOfLastYear = startOfLastYear.withDayOfYear(startOfLastYear.lengthOfYear())

        val thisYearExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfYear) && !date.isAfter(endOfYear)
        }
        val lastYearExpenses = expenses.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            !date.isBefore(startOfLastYear) && !date.isAfter(endOfLastYear)
        }

        val totalSpend = thisYearExpenses.sumOf { it.amount }
        val lastYearTotal = lastYearExpenses.sumOf { it.amount }

        val monthlyMap = thisYearExpenses.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().monthValue
        }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val dailySpends = (1..12).map { month ->
            DailySpend("${month}月", monthlyMap[month] ?: 0L)
        }

        val categorySpends = calculateCategorySpends(thisYearExpenses)
        val spentMonths = thisYearExpenses.map {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().monthValue
        }.distinct().size
        val averageSpend = if (spentMonths > 0) totalSpend.toDouble() / spentMonths else 0.0
        val maxSpend = thisYearExpenses.maxOfOrNull { it.amount } ?: 0L
        val trendComparison = formatTrendComparison(totalSpend, lastYearTotal, "去年")

        return StatisticsUiState(
            selectedDimension = StatisticsDimension.YEAR,
            dailySpends = dailySpends,
            categorySpends = categorySpends,
            totalSpend = totalSpend,
            averageSpend = averageSpend,
            maxSpend = maxSpend,
            trendComparison = trendComparison,
            dimensionLabel = "今年支出"
        )
    }

    private fun calculateCategorySpends(expenses: List<Transaction>): List<CategorySpend> {
        val categoryMap = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        
        val totalForCat = categoryMap.values.sum()
        return categoryMap.map { (cat, amount) ->
            val percent = if (totalForCat > 0) (amount.toDouble() / totalForCat).toFloat() else 0f
            CategorySpend(cat, amount, percent)
        }.sortedByDescending { it.amount }
    }

    private fun formatTrendComparison(current: Long, previous: Long, label: String): String {
        return if (previous <= 0L) {
            if (current > 0L) "上期无消费记录" else "暂无波动数据"
        } else {
            val diffPercent = ((current.toDouble() - previous) / previous) * 100.0
            if (diffPercent >= 0.0) {
                String.format(Locale.getDefault(), "比%s增加 %.1f%%", label, diffPercent)
            } else {
                String.format(Locale.getDefault(), "比%s减少 %.1f%%", label, -diffPercent)
            }
        }
    }
}
