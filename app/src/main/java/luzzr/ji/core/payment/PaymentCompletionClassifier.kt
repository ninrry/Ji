package luzzr.ji.core.payment

import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform

data class PaymentCompletionSignal(
    val platform: PaymentPlatform,
    val kind: PaymentKind
)

object PaymentCompletionClassifier {
    private val blockedWords = listOf("失败", "取消", "关闭", "处理中", "待支付", "支付中", "已撤销")
    private val refundBlockedWords = listOf(
        "退款成功", "退款到账", "退款金额", "退款通知", "退款原因", "退款时间",
        "已退款", "原路退回", "退回零钱", "退回余额"
    )
    private val wechatBillListMarkers = listOf(
        "微信支付账单", "全部账单", "账单列表", "账单筛选", "账单统计",
        "收支统计", "搜索账单", "交易记录", "历史账单", "我的账单",
        "支付服务", "摇优惠"
    )
    private val wechatBillDetailMarkers = listOf(
        "账单详情", "当前状态", "交易单号", "商户单号", "支付时间", "付款时间",
        "支付方式", "付款方式", "商品", "商户全称", "收款方", "收单机构",
        "账单服务", "申请电子凭证", "发起群收款", "在此商户的交易", "对订单有疑惑"
    )
    private val wechatMessageMarkers = listOf(
        "支付消息", "服务通知", "消息列表", "消息详情", "微信支付通知",
        "微信支付凭证"
    )
    private val wechatMessageCardMarkers = listOf(
        "使用零钱支付", "使用零钱通支付", "使用银行卡支付"
    )
    private val alipayBillListMarkers = listOf(
        "支付宝账单", "全部账单", "账单列表", "账单筛选", "账单搜索",
        "收支分析", "月度账单", "交易记录", "历史账单", "花呗账单",
        "账单管理", "账单分类", "账单服务"
    )
    private val alipayBillDetailMarkers = listOf(
        "账单详情", "当前状态", "交易号", "交易单号", "订单号", "商户单号",
        "创建时间", "付款时间", "支付时间", "付款方式", "支付方式", "商品",
        "商品说明", "商户全称", "收款方全称", "收单机构", "服务商", "服务详情",
        "标签", "申请电子凭证", "在此商户的交易", "对订单有疑惑"
    )
    private val alipayMessageMarkers = listOf(
        "支付消息", "消息中心", "消息通知", "通知消息", "消息详情",
        "服务提醒", "消息盒子", "站内消息", "系统消息", "服务消息",
        "本月支出", "统计支出", "大额消费", "先用后付", "分期付款"
    )
    private val alipayMessageCardActionMarkers = listOf("查看详情", "管理当前自动扣款服务")
    private val alipayMessageCardFieldMarkers = listOf("付款方式", "支付方式", "交易对象", "扣款服务")
    private val jdBillListMarkers = listOf(
        "我的账单", "全部账单", "账单明细", "账单查询", "交易记录",
        "消费记录", "订单列表", "订单详情", "白条账单", "还款记录"
    )
    private val jdBillDetailMarkers = listOf("订单编号", "交易单号", "下单时间", "付款时间", "支付时间", "支付方式")

    fun from(packageName: String, rawText: String): PaymentCompletionSignal? {
        val text = rawText.replace(Regex("\\s+"), "")
        if (text.length < 4 || blockedWords.any(text::contains) || refundBlockedWords.any(text::contains)) return null
        if (isHistoricalBillPage(packageName, text)) return null

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
                isAlipayMerchantCompletion(text) -> PaymentCompletionSignal(PaymentPlatform.ALIPAY, PaymentKind.MERCHANT_PAYMENT)
                else -> null
            }

            "com.jingdong.app.mall" -> when {
                (text.contains("白条") && text.contains("还款成功")) || text.contains("白条还款成功") -> PaymentCompletionSignal(PaymentPlatform.JD, PaymentKind.BAITIAO_REPAYMENT)
                isJdMerchantCompletion(text) -> PaymentCompletionSignal(PaymentPlatform.JD, PaymentKind.MERCHANT_PAYMENT)
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
        if (looksLikeWechatDetailOrMessageCard(text)) return false
        // The actual WeChat result UI can expose only "支付成功". History and message views are
        // blocked above, so do not require an additional button that may be absent from the node tree.
        return text.contains("付款成功") || text.contains("支付成功")
    }

    private fun isWechatBillHistory(text: String): Boolean =
        wechatBillListMarkers.any(text::contains) ||
            wechatMessageMarkers.any(text::contains) ||
            looksLikeWechatDetailOrMessageCard(text)

    private fun isAlipayMerchantCompletion(text: String): Boolean =
        !isAlipayHistoricalPage(text) &&
            (text.contains("付款成功") || text.contains("支付成功") || text.contains("交易成功") || text.contains("自动扣款成功"))

    private fun isJdMerchantCompletion(text: String): Boolean =
        !isJdBillHistory(text) && (text.contains("付款成功") || text.contains("支付成功") || text.contains("交易成功"))

    private fun isHistoricalBillPage(packageName: String, text: String): Boolean = when (packageName) {
        "com.tencent.mm" -> isWechatBillHistory(text)
        "com.eg.android.AlipayGphone" -> isAlipayHistoricalPage(text)
        "com.jingdong.app.mall" -> isJdBillHistory(text)
        // Bank apps are intentionally not listed in accessibility_service_config. Unknown packages
        // are never treated as payment sources, which prevents bank-statement browsing from billing.
        else -> true
    }

    private fun isAlipayHistoricalPage(text: String): Boolean =
        alipayBillListMarkers.any(text::contains) ||
            looksLikeAlipayDetailPage(text) ||
            looksLikeAlipayPaymentMessageCard(text)

    private fun isJdBillHistory(text: String): Boolean =
        jdBillListMarkers.any(text::contains) || jdBillDetailMarkers.count(text::contains) >= 2

    private fun looksLikeWechatDetailOrMessageCard(text: String): Boolean {
        if (wechatBillDetailMarkers.count(text::contains) >= 3) return true
        if (text.contains("账单详情") && (wechatMessageMarkers.any(text::contains) || wechatMessageCardMarkers.any(text::contains))) return true
        return text.contains("账单服务") ||
            text.contains("申请电子凭证") ||
            text.contains("在此商户的交易") ||
            text.contains("对订单有疑惑")
    }

    private fun looksLikeAlipayDetailPage(text: String): Boolean {
        if (alipayBillDetailMarkers.count(text::contains) >= 3) return true
        return text.contains("账单详情") ||
            text.contains("账单服务") ||
            text.contains("账单管理") ||
            text.contains("账单分类") ||
            text.contains("收款方全称") ||
            text.contains("申请电子凭证") ||
            text.contains("在此商户的交易") ||
            text.contains("对订单有疑惑") ||
            text.contains("服务详情")
    }

    private fun looksLikeAlipayPaymentMessageCard(text: String): Boolean {
        if (alipayMessageMarkers.any(text::contains)) return true
        val hasSuccess = text.contains("付款成功") || text.contains("支付成功") || text.contains("交易成功") || text.contains("自动扣款成功")
        val hasCardAction = alipayMessageCardActionMarkers.any(text::contains)
        val hasMessageFields = alipayMessageCardFieldMarkers.any(text::contains)
        return hasSuccess && hasCardAction && hasMessageFields
    }
}
