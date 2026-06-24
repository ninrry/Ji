package luzzr.ji.feature.statistics

enum class StatisticsDimension {
    WEEK, MONTH, YEAR
}

data class DailySpend(
    val label: String,  // 横坐标文本标签，如 "周一"、"1日"、"1月"
    val amount: Long
)

data class CategorySpend(
    val category: String,
    val amount: Long,
    val percentage: Float
)

data class StatisticsUiState(
    val selectedDimension: StatisticsDimension = StatisticsDimension.MONTH,
    val dailySpends: List<DailySpend> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val totalSpend: Long = 0L,
    val averageSpend: Double = 0.0,
    val maxSpend: Long = 0L,
    val trendComparison: String = "",
    val dimensionLabel: String = ""
)
