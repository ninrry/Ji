package luzzr.ji.domain.usecase

import luzzr.ji.domain.model.Budget
import java.time.YearMonth

internal fun List<Budget>.effectiveBudgetFor(yearMonth: YearMonth): Budget? {
    return asSequence()
        .mapNotNull { budget ->
            runCatching { YearMonth.parse(budget.yearMonth) }
                .getOrNull()
                ?.takeIf { !it.isAfter(yearMonth) }
                ?.let { it to budget }
        }
        .maxByOrNull { it.first }
        ?.second
}
