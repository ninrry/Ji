package luzzr.ji.core.common

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyAmountParser {
    private val validAmountPattern = Regex("^\\d{1,9}(?:\\.\\d{1,2})?$")

    fun yuanToFenOrNull(input: String): Long? {
        val normalized = input.trim().replace(",", "")
        if (!validAmountPattern.matches(normalized)) return null
        return runCatching {
            BigDecimal(normalized)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
                .takeIf { it >= 0L }
        }.getOrNull()
    }
}
