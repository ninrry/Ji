package luzzr.ji.core.payment

import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform

data class PaymentCompletionSignal(
    val platform: PaymentPlatform,
    val kind: PaymentKind
)

object PaymentCompletionClassifier {
    private val blockedWords = listOf("退款", "失败", "取消", "关闭", "处理中", "待支付", "支付中", "已撤销")

    fun from(packageName: String, rawText: String): PaymentCompletionSignal? {
        val text = rawText.replace(Regex("\\s+"), "")
        if (text.length < 4 || blockedWords.any(text::contains)) return null

        return when (packageName) {
            "com.tencent.mm" -> when {
                text.contains("红包已发出") || text.contains("红包发送成功") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.RED_PACKET)
                text.contains("转账成功") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.TRANSFER)
                text.contains("提现成功") || text.contains("提现申请已提交") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.WITHDRAWAL)
                text.contains("支付成功") || text.contains("付款成功") -> PaymentCompletionSignal(PaymentPlatform.WECHAT, PaymentKind.MERCHANT_PAYMENT)
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
}
