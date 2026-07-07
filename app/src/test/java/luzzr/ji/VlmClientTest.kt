package luzzr.ji

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import luzzr.ji.core.vlm.VlmClient
import luzzr.ji.core.vlm.VlmErrorCategory
import luzzr.ji.core.vlm.VlmProvider
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VlmClientTest {

    @Test
    fun testLocalFallbackParsing() = runTest {
        val client = VlmClient()

        // 1. 标准微信支付成功解析
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

        // 2. 支付宝账单解析
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

        // 3. 无金额解析失败返回 null
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

    @Test
    fun testPaymentResponseNewFormat() {
        val client = VlmClient()
        // New compact format (s:1, a, c, n, t)
        val result = client.parsePaymentResponse(
            """{"s":1,"a":"28.50","c":"餐饮","n":"星巴克","t":"WX202607070001"}""",
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT
        )
        assertNotNull(result)
        assertEquals(2850L, result!!.amount)
        assertEquals("餐饮", result.category)
        assertEquals("星巴克", result.note)
        assertEquals("WX202607070001", result.tradeId)
        assertEquals(PaymentPlatform.WECHAT, result.platform)
        assertEquals(PaymentKind.MERCHANT_PAYMENT, result.paymentKind)
    }

    @Test
    fun testPaymentResponseOldFormat() {
        val client = VlmClient()
        // Old format backward compatibility (status:SUCCESS, amount, category, etc.)
        val result = client.parsePaymentResponse(
            """{"status":"SUCCESS","amount":"15.50","category":"交通","kind":"MERCHANT_PAYMENT","platform":"ALIPAY","note":"滴滴","trade_id":"AP20260707001","confidence":0.95}""",
            PaymentPlatform.ALIPAY,
            PaymentKind.MERCHANT_PAYMENT
        )
        assertNotNull(result)
        assertEquals(1550L, result!!.amount)
        assertEquals("交通", result.category)
        assertEquals("滴滴", result.note)
        assertEquals("AP20260707001", result.tradeId)
        assertEquals(PaymentPlatform.ALIPAY, result.platform)
    }

    @Test
    fun testPaymentResponseNotPayment() {
        val client = VlmClient()
        val result = client.parsePaymentResponse(
            """{"s":0}""",
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT
        )
        assertNull(result)
    }

    @Test
    fun testPaymentResponseOldFormatRejected() {
        val client = VlmClient()
        val result = client.parsePaymentResponse(
            """{"status":"NOT_A_COMPLETED_PAYMENT"}""",
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT
        )
        assertNull(result)
    }

    @Test
    fun testProviderDefaults() {
        assertEquals("https://api.xiaomimimo.com/v1/chat/completions", VlmProvider.XIAOMI.defaultApiUrl)
        assertEquals("mimo-v2.5", VlmProvider.XIAOMI.defaultModel)
        assertEquals("https://opencode.ai/zen/go/v1/chat/completions", VlmProvider.OPENCODE.defaultApiUrl)
        assertEquals("mimo-v2.5", VlmProvider.OPENCODE.defaultModel)
        assertEquals("小米 MiMo", VlmProvider.XIAOMI.displayName)
        assertEquals("OpenCode Go", VlmProvider.OPENCODE.displayName)
    }

    @Test
    fun testProviderApiKeyIsolation() {
        // Each provider has its own isolated pref key
        assertNotEquals(VlmProvider.XIAOMI.apiKeyPrefKey, VlmProvider.OPENCODE.apiKeyPrefKey)
        assertEquals("xiaomi_api_key", VlmProvider.XIAOMI.apiKeyPrefKey)
        assertEquals("opencode_api_key", VlmProvider.OPENCODE.apiKeyPrefKey)
    }

    @Test
    fun testErrorCategoryRetryability() {
        // Retryable errors
        assertTrue(VlmErrorCategory.RATE_LIMITED.isRetryable)
        assertTrue(VlmErrorCategory.SERVER_ERROR.isRetryable)
        assertTrue(VlmErrorCategory.NETWORK_ERROR.isRetryable)

        // Non-retryable errors
        assertFalse(VlmErrorCategory.BAD_REQUEST.isRetryable)
        assertFalse(VlmErrorCategory.AUTH_FAILED.isRetryable)
        assertFalse(VlmErrorCategory.BALANCE_INSUFFICIENT.isRetryable)
        assertFalse(VlmErrorCategory.ACCESS_DENIED.isRetryable)
        assertFalse(VlmErrorCategory.NOT_FOUND.isRetryable)
        assertFalse(VlmErrorCategory.CONTENT_BLOCKED.isRetryable)
        assertFalse(VlmErrorCategory.UNKNOWN.isRetryable)
    }
}
