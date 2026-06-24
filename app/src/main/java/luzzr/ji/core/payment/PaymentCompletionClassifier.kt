package luzzr.ji.core.payment

import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform

data class PaymentCompletionSignal(
    val platform: PaymentPlatform,
    val kind: PaymentKind
)

object PaymentCompletionClassifier {
    private val blockedWords = listOf("退款", "失败", "取消", "关闭", "处理中", "待支付", "支付中", "已撤销")
    private val wechatBillListMarkers = listOf(
        "微信支付账单", "全部账单", "账单列表", "账单筛选", "账单统计",
        "收支统计", "搜索账单", "交易记录", "历史账单"
    )
    private val wechatBillDetailMarkers = listOf("账单详情", "交易单号", "支付时间", "收款方")

    fun from(packageName: String, rawText: String): PaymentCompletionSignal? {
        val text = rawText.replace(Regex("\\s+"), "")
        if (text.length < 4 || blockedWords.any(text::contains)) return null
        if (packageName == "com.tencent.mm" && isWechatBillHistory(text)) return null

        return when (packageName) {
            "com.tencent.mm" -> when {
                text.contains("红包已发出") || text.contains("红包发送成功") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.RED_PACKET)
                text.contains("转账成功") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.TRANSFER)
                text.contains("提现成功") || text.contains("提现申请已提交") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.WITHDRAWAL)
                isWechatMerchantCompletion(text) -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.MERCHANT_PAYMENT)
                else -> null
            }

            "com.eg.android.AlipayGphone" -> when {
                text.contains("花呗") && (text.contains("还款成功") || text.contains("已还清")) -> PaymentCompletionSignal(PaymentPlatform.ALIPAY, PaymentKind.HUABEI_REPAYMENT)
                text.contains("转账成功") -> PaymentCompletionSignal(PaymentPlatform.ALIPAY, PaymentKind.TRANSFER)
                text.contains("支付成功") || text.contains("付款成功") || text.contains("交易成功") -> PaymentCompletionSignal(PaymentPlatform.ALIPAY, PaymentKind.MERCHANT_PAYMENT)
                else -> null
            }

            "com.jingdong.app.mall" -> when {
                (text.contains("白条") && text.contains("还款成功")) || text.contains("白条还款成功") -> PaymentCompletionSignal(PaymentPlatform.JD, PaymentKind.BAITIAO_REPAYMENT)
                text.contains("支付成功") || text.contains("付款成功") || text.contains("交易成功") -> PaymentCompletionSignal(PaymentPlatform.JD, PaymentKind.MERCHANT_PAYMENT)
                else -> null
            }

            else -> null
        }
    }

    /** Revalidates persisted work before a cloud request, so pre-upgrade history tasks are harmless. */
    fun isStillEligible(platform: PaymentPlatform, kind: PaymentKind, rawText: String): Boolean {
        val packageName = when (platform) {
            PaymentPlatform.WECHAT -> "com.tencent.mm"
            PaymentPlatform.ALIPAY -> "com.eg.android.AlipayGphone"
            PaymentPlatform.JD -> "com.jingdong.app.mall"
            PaymentPlatform.MANUAL -> return false
        }
        return from(packageName, rawText)?.let { it.platform == platform && it.kind == kind } == true
    }

    private fun isWechatMerchantCompletion(text: String): Boolean {
        if (isWechatBillHistory(text)) return false
        // A historic detail view normally contains several of these fields. A completion page may
        // link to details, but it does not present the complete historic-detail field set.
        if (wechatBillDetailMarkers.count(text::contains) >= 2) return false
        // "付款成功" is the transient merchant-payment result. "支付成功" by itself is also
        // shown in history pages, so only accept it with an immediate completion affordance.
        return text.contains("付款成功") ||
            (text.contains("支付成功") && (text.contains("完成") || text.contains("继续付款")))
    }

    private fun isWechatBillHistory(text: String): Boolean =
        wechatBillListMarkers.any(text::contains) || wechatBillDetailMarkers.count(text::contains) >= 2
}
