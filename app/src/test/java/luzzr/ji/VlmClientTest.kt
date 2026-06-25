package luzzr.ji

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import luzzr.ji.core.vlm.VlmClient
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VlmClientTest {

    @Test
    fun testLocalFallbackParsing() = runTest {
        val client = VlmClient()

        // 1. 测试标准微信支付成功解析
        val wechatText = """
            微信支付
            付款成功
            ￥15.50
            商户：罗森便利店
        """.trimIndent()
        val result1 = client.parseScreen(wechatText)
        assertNotNull(result1)
        assertEquals(1550L, result1!!.amount)
        assertEquals("罗森便利店", result1.note)
        assertEquals("餐饮", result1.category)
        assertTrue(result1.isFallback)

        // 2. 测试支付宝账单解析
        val alipayText = """
            支付宝
            付款详情
            商品说明：美团外卖
            支付金额 35.80 元
        """.trimIndent()
        val result2 = client.parseScreen(alipayText)
        assertNotNull(result2)
        assertEquals(3580L, result2!!.amount)
        assertEquals("美团外卖", result2.note)
        assertEquals("餐饮", result2.category)
        assertTrue(result2.isFallback)

        // 3. 测试无金额解析失败返回 null
        val noAmountText = """
            我的账号
            支付成功
            这里没有钱数
        """.trimIndent()
        val result3 = client.parseScreen(noAmountText)
        assertNull(result3)
    }

    @Test
    fun testParseScreenImageFallback() = runTest {
        val client = VlmClient()
        val imageBytes = byteArrayOf(1, 2, 3)
        val text = "微信支付 ￥100.00 罗森"
        val result = client.parseScreenImage(imageBytes, text)
        assertNotNull(result)
        assertEquals(10000L, result!!.amount)
        assertEquals("罗森", result.note)
        assertTrue(result.isFallback)
    }
}
