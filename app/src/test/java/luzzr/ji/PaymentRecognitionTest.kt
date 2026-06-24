package luzzr.ji

import luzzr.ji.core.payment.PaymentCompletionClassifier
import luzzr.ji.core.payment.PaymentFingerprint
import luzzr.ji.core.payment.PaymentRecognitionManager
import luzzr.ji.core.vlm.VlmClient
import luzzr.ji.core.vlm.VlmTransactionResult
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentRecognitionTest {
    @Test
    fun `classifies all supported completion scenarios`() {
        val cases = listOf(
            Triple("com.tencent.mm", "微信支付 付款成功 ￥18.80", PaymentKind.MERCHANT_PAYMENT),
            Triple("com.eg.android.AlipayGphone", "支付宝 交易成功 ¥22.00", PaymentKind.MERCHANT_PAYMENT),
            Triple("com.tencent.mm", "转账成功 ￥100.00", PaymentKind.TRANSFER),
            Triple("com.tencent.mm", "红包已发出 ￥66.00", PaymentKind.RED_PACKET),
            Triple("com.tencent.mm", "提现申请已提交 ￥200.00", PaymentKind.WITHDRAWAL),
            Triple("com.eg.android.AlipayGphone", "花呗还款成功 ￥88.00", PaymentKind.HUABEI_REPAYMENT),
            Triple("com.jingdong.app.mall", "白条还款成功 ￥128.00", PaymentKind.BAITIAO_REPAYMENT)
        )
        cases.forEach { (packageName, text, expectedKind) ->
            val signal = PaymentCompletionClassifier.from(packageName, text)
            assertNotNull(text, signal)
            assertEquals(text, expectedKind, signal?.kind)
        }
    }

    @Test
    fun `rejects failed and refunded pages`() {
        assertNull(PaymentCompletionClassifier.from("com.tencent.mm", "付款失败 ￥18.80"))
        assertNull(PaymentCompletionClassifier.from("com.eg.android.AlipayGphone", "退款成功 ￥22.00"))
        assertNull(PaymentCompletionClassifier.from("com.jingdong.app.mall", "支付处理中"))
    }

    @Test
    fun `rejects wechat bill history and accepts an immediate merchant completion`() {
        assertNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "微信支付账单 全部账单 便利店 支付成功 ￥18.80"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "账单详情 支付成功 交易单号 2026062410012345 支付时间 10:01 收款方 便利店"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "微信支付 付款成功 ￥18.80 完成"
            )
        )
    }

    @Test
    fun `rejects Alipay JD and bank statement history pages`() {
        assertNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "支付宝账单 账单详情 交易成功 交易号 2026062410012345 付款时间 10:01"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "支付消息 消息详情 您有一笔交易成功 ￥22.00"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.jingdong.app.mall",
                "白条账单 账单明细 交易成功 订单编号 123456789 付款时间 10:01"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.icbc",
                "交易明细 支付成功 ￥18.80"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "支付宝 交易成功 ￥22.00"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.jingdong.app.mall",
                "京东支付 付款成功 ￥36.00"
            )
        )
    }

    @Test
    fun `fallback transaction key preserves duplicate candidates but permits a later same amount payment`() {
        val result = VlmTransactionResult(
            amount = 1880,
            category = "购物",
            note = "便利店",
            platform = PaymentPlatform.WECHAT,
            paymentKind = PaymentKind.MERCHANT_PAYMENT
        )
        val first = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "payment-a", 1_000_000L)
        val repeated = PaymentRecognitionManager.fallbackTransactionDedupKey(result.copy(note = "  便利店  "), "payment-a", 1_010_000L)
        val secondPayment = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "payment-b", 1_100_000L)
        assertEquals(first, repeated)
        assertFalse(first == secondPayment)
    }

    @Test
    fun `parses and validates strict cloud response`() {
        val client = VlmClient()
        val response = """{"status":"SUCCESS","amount":"18.80","category":"餐饮","kind":"MERCHANT_PAYMENT","platform":"WECHAT","note":"便利店","trade_id":"wx-1","completed_at":"2026-06-23T12:00:00Z","confidence":0.96}"""
        val parsed = client.parsePaymentResponse(response, PaymentPlatform.WECHAT, PaymentKind.MERCHANT_PAYMENT)
        assertNotNull(parsed)
        assertEquals(1880L, parsed?.amount)
        assertEquals("wx-1", parsed?.tradeId)
        assertEquals(PaymentPlatform.WECHAT, parsed?.platform)
        assertEquals(PaymentKind.MERCHANT_PAYMENT, parsed?.paymentKind)
        assertNull(client.parsePaymentResponse(response, PaymentPlatform.ALIPAY, PaymentKind.MERCHANT_PAYMENT))
    }

    @Test
    fun `fingerprint normalizes whitespace and retains platform scope`() {
        val first = PaymentFingerprint.create(PaymentPlatform.WECHAT, PaymentKind.TRANSFER, "转账成功   ￥10.00")
        val same = PaymentFingerprint.create(PaymentPlatform.WECHAT, PaymentKind.TRANSFER, "转账成功 ￥10.00")
        val otherPlatform = PaymentFingerprint.create(PaymentPlatform.ALIPAY, PaymentKind.TRANSFER, "转账成功 ￥10.00")
        assertEquals(first, same)
        assertFalse(first == otherPlatform)
    }

    @Test
    fun `fingerprint ignores volatile clocks but retains payment identifiers`() {
        val first = PaymentFingerprint.create(
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT,
            "微信支付 付款成功 ￥18.80 2026年6月24日 10:01 交易单号 2026062410012345"
        )
        val refreshed = PaymentFingerprint.create(
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT,
            "微信支付 付款成功 ￥18.80 2026年6月24日 10:02 交易单号 2026062410012345"
        )
        val secondPayment = PaymentFingerprint.create(
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT,
            "微信支付 付款成功 ￥18.80 2026年6月24日 10:02 交易单号 2026062410019999"
        )
        assertEquals(first, refreshed)
        assertFalse(first == secondPayment)
    }

    @Test
    fun `unidentified payment identity is platform scoped and uses a short callback window`() {
        val alipay = PaymentFingerprint.captureIdentity(
            PaymentPlatform.ALIPAY,
            PaymentKind.MERCHANT_PAYMENT,
            "支付宝 支付成功 ¥18.80"
        )
        val wechat = PaymentFingerprint.captureIdentity(
            PaymentPlatform.WECHAT,
            PaymentKind.MERCHANT_PAYMENT,
            "微信支付 支付成功 ￥18.80"
        )
        assertEquals(PaymentFingerprint.UNIDENTIFIED_PAYMENT_WINDOW_MS, alipay.dedupWindowMs)
        assertFalse(alipay.fingerprint == wechat.fingerprint)
    }

    @Test
    fun `accepts WeChat success page without a completion button but rejects payment messages`() {
        assertNotNull(PaymentCompletionClassifier.from("com.tencent.mm", "微信支付 支付成功 ￥12.00"))
        assertNull(PaymentCompletionClassifier.from("com.tencent.mm", "支付消息 微信支付通知 支付成功 ￥12.00"))
    }
}
