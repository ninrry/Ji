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
    fun `rejects wallet bill detail and payment message screens from screenshots`() {
        assertNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "无人零售 -5.00 自动扣款成功 管理自动扣款 无人零售免密支付 " +
                    "支付时间 2026-06-25 11:55:05 付款方式 余额宝 商品说明 智能货柜消费 " +
                    "收款方全称 武汉轻购云科技有限公司 服务详情 账单管理 账单分类 标签"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "服务消息 支付消息 6月统计支出 本月支出 自动扣款成功 ￥5.00 查看详情 " +
                    "扣款服务 自动扣款 免密支付 付款方式 余额宝 交易对象 无人零售"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "天津工业大学 付款成功 ￥5.70 查看详情 付款方式 余额宝"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "微信支付 阿金烤鸭饭 付款成功 使用零钱支付 ￥10.00 账单详情 我的账单 支付服务 摇优惠"
            )
        )
        assertNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "阿金烤鸭饭 -10.00 当前状态 支付成功 支付时间 2026年6月25日 18:13:24 " +
                    "商品 阿金烤鸭饭 商户全称 商户_许清林 收单机构 拉卡拉支付股份有限公司 " +
                    "支付方式 零钱 交易单号 4200003175202606258228927561 商户单号 可在支持的商户扫码退款 " +
                    "账单服务 对订单有疑惑 发起群收款 在此商户的交易 申请电子凭证"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "支付宝 支付成功 ¥22.00 付款方式 余额宝 完成 返回商家"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.eg.android.AlipayGphone",
                "支付宝 自动扣款成功 ¥5.00 完成 返回商家"
            )
        )
        assertNotNull(
            PaymentCompletionClassifier.from(
                "com.tencent.mm",
                "微信支付 支付成功 ￥12.00"
            )
        )
    }

    @Test
    fun `fallback transaction key deduplicates no-trade-id repeats but preserves different merchants and platforms`() {
        val result = VlmTransactionResult(
            amount = 1880,
            category = "购物",
            note = "便利店",
            platform = PaymentPlatform.WECHAT,
            paymentKind = PaymentKind.MERCHANT_PAYMENT
        )
        val first = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "payment-a", 1_000_000L)
        val repeated = PaymentRecognitionManager.fallbackTransactionDedupKey(result.copy(note = "  便利店  "), "payment-a", 1_010_000L)
        val otherMerchant = PaymentRecognitionManager.fallbackTransactionDedupKey(result.copy(note = "超市"), "payment-b", 1_100_000L)
        val otherPlatform = PaymentRecognitionManager.fallbackTransactionDedupKey(
            result.copy(platform = PaymentPlatform.ALIPAY),
            "payment-c",
            1_100_000L
        )
        val muchLater = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "payment-d", 1_000_000L + 16 * 60_000L)
        assertEquals(first, repeated)
        assertFalse(first == otherMerchant)
        assertFalse(first == otherPlatform)
        assertFalse(first == muchLater)
    }

    @Test
    fun `fallback transaction key deduplicates repeated history captures within completed time window`() {
        val completedAt = 1_772_003_604_000L
        val result = VlmTransactionResult(
            amount = 1000,
            category = "餐饮",
            note = "阿金烤鸭饭",
            platform = PaymentPlatform.WECHAT,
            paymentKind = PaymentKind.MERCHANT_PAYMENT,
            completedAt = completedAt
        )
        val first = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "history-subtree-a", 1_800_000L)
        val repeatedLater = PaymentRecognitionManager.fallbackTransactionDedupKey(result, "history-subtree-b", 1_950_000L)
        val differentPaymentTime = PaymentRecognitionManager.fallbackTransactionDedupKey(
            result.copy(completedAt = completedAt + 16 * 60_000L),
            "history-subtree-c",
            1_950_000L
        )
        assertEquals(first, repeatedLater)
        assertFalse(first == differentPaymentTime)
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
    fun `local fallback rules parse common wallet texts and categories`() {
        val client = VlmClient()
        val food = client.parseLocalFallback("商户全称 阿金烤鸭饭 -10.00 当前状态 支付成功")
        val vending = client.parseLocalFallback("收款方全称 武汉轻购云科技有限公司 自动扣款成功 ￥5.00 智能货柜消费")
        assertNotNull(food)
        assertEquals(1000L, food?.amount)
        assertEquals("餐饮", food?.category)
        assertEquals("阿金烤鸭饭", food?.note)
        assertNotNull(vending)
        assertEquals(500L, vending?.amount)
        assertEquals("购物", vending?.category)
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
