package luzzr.ji.domain.model

enum class TransactionType {
    INCOME, EXPENSE
}

enum class PaymentPlatform {
    MANUAL,
    WECHAT,
    ALIPAY,
    JD
}

enum class PaymentKind(val defaultCategory: String) {
    MERCHANT_PAYMENT("其它"),
    TRANSFER("转账"),
    RED_PACKET("红包"),
    WITHDRAWAL("提现"),
    HUABEI_REPAYMENT("花呗还款"),
    BAITIAO_REPAYMENT("白条还款")
}

data class Transaction(
    val id: Long = 0,
    val amount: Long,
    val type: TransactionType,
    val category: String,
    val note: String,
    val timestamp: Long,
    val isExtra: Boolean,
    val source: String = "MANUAL",
    val platform: PaymentPlatform = PaymentPlatform.MANUAL,
    val paymentKind: PaymentKind = PaymentKind.MERCHANT_PAYMENT,
    val tradeId: String? = null,
    val occurredAt: Long = timestamp,
    val dedupKey: String? = null
)
