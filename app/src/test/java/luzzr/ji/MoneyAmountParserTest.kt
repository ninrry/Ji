package luzzr.ji

import luzzr.ji.core.common.MoneyAmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyAmountParserTest {
    @Test
    fun `converts yuan strings to fen without floating point math`() {
        assertEquals(123L, MoneyAmountParser.yuanToFenOrNull("1.23"))
        assertEquals(1010L, MoneyAmountParser.yuanToFenOrNull("10.10"))
        assertEquals(0L, MoneyAmountParser.yuanToFenOrNull("0"))
        assertEquals(99999999999L, MoneyAmountParser.yuanToFenOrNull("999999999.99"))
    }

    @Test
    fun `rejects invalid money inputs`() {
        assertNull(MoneyAmountParser.yuanToFenOrNull("-1"))
        assertNull(MoneyAmountParser.yuanToFenOrNull("1.234"))
        assertNull(MoneyAmountParser.yuanToFenOrNull("abc"))
    }
}
