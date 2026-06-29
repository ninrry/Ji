package luzzr.ji

import luzzr.ji.core.payment.PaymentCompletionClassifier
import luzzr.ji.core.payment.PaymentRuleDecision
import luzzr.ji.domain.model.PaymentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCompletionClassifierFixtureTest {
    @Test
    fun `fixture pages expose stable accept and reject rule traces`() {
        val cases = listOf(
            // === WeChat: accept ===
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/immediate_merchant_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.MERCHANT_PAYMENT,
                ruleId = "wechat.merchant_payment"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/transfer_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.TRANSFER,
                ruleId = "wechat.transfer"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/red_packet_sent.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.RED_PACKET,
                ruleId = "wechat.red_packet"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/withdrawal_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.WITHDRAWAL,
                ruleId = "wechat.withdrawal"
            ),
            // === WeChat: reject ===
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/bill_detail_history.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "wechat.bill_detail_field_cluster"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/payment_message.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "wechat.bill_list_or_message"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/bill_list_reject.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "wechat.bill_list_or_message"
            ),
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/wechat/bill_detail_with_actions.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "wechat.bill_list_or_message"
            ),
            // === Alipay: accept ===
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/immediate_auto_debit_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.MERCHANT_PAYMENT,
                ruleId = "alipay.merchant_payment"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/payment_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.MERCHANT_PAYMENT,
                ruleId = "alipay.merchant_payment"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/huabei_repayment.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.HUABEI_REPAYMENT,
                ruleId = "alipay.huabei_repayment"
            ),
            // === Alipay: reject ===
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/bill_detail_history.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "alipay.bill_list_or_message"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/message_card.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "alipay.payment_message_card"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/payment_message_reject.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "alipay.bill_list_or_message"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/service_message_reject.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "alipay.bill_list_or_message"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/alipay/auto_debit_message_reject.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "alipay.auto_debit_message_card"
            ),
            // === JD: accept ===
            FixtureCase(
                packageName = "com.jingdong.app.mall",
                fixture = "/payment-fixtures/jd/immediate_success.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.MERCHANT_PAYMENT,
                ruleId = "jd.merchant_payment"
            ),
            FixtureCase(
                packageName = "com.jingdong.app.mall",
                fixture = "/payment-fixtures/jd/baitiao_repayment.txt",
                decision = PaymentRuleDecision.ACCEPT,
                kind = PaymentKind.BAITIAO_REPAYMENT,
                ruleId = "jd.baitiao_repayment"
            ),
            // === JD: reject ===
            FixtureCase(
                packageName = "com.jingdong.app.mall",
                fixture = "/payment-fixtures/jd/bill_history.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "jd.bill_list"
            ),
            // === Common reject ===
            FixtureCase(
                packageName = "com.tencent.mm",
                fixture = "/payment-fixtures/common/failed_payment.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "common.not_completed"
            ),
            FixtureCase(
                packageName = "com.eg.android.AlipayGphone",
                fixture = "/payment-fixtures/common/refund_success.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "common.refund"
            ),
            FixtureCase(
                packageName = "com.jingdong.app.mall",
                fixture = "/payment-fixtures/common/processing.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "common.not_completed"
            ),
            // === Unsupported package ===
            FixtureCase(
                packageName = "com.icbc",
                fixture = "/payment-fixtures/common/unsupported_package.txt",
                decision = PaymentRuleDecision.REJECT,
                kind = null,
                ruleId = "common.unsupported_package"
            )
        )

        cases.forEach { case ->
            val result = PaymentCompletionClassifier.classify(case.packageName, readFixture(case.fixture))
            assertEquals(case.fixture, case.decision, result.trace.decision)
            assertEquals(case.fixture, case.ruleId, result.trace.ruleId)
            if (case.decision == PaymentRuleDecision.ACCEPT) {
                assertNotNull(case.fixture, result.signal)
                assertEquals(case.fixture, case.kind, result.signal?.kind)
                assertTrue(case.fixture, result.trace.matchedKeywords.isNotEmpty())
            } else {
                assertNull(case.fixture, result.signal)
            }
        }
    }

    private fun readFixture(path: String): String =
        requireNotNull(javaClass.getResource(path)) { "missing fixture: $path" }
            .readText()

    private data class FixtureCase(
        val packageName: String,
        val fixture: String,
        val decision: PaymentRuleDecision,
        val kind: PaymentKind?,
        val ruleId: String
    )
}
