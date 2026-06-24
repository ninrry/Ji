package luzzr.ji.core.payment

import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import java.security.MessageDigest

data class PaymentCandidate(
    val platform: PaymentPlatform,
    val kindHint: PaymentKind,
    val screenText: String,
    val screenshotBytes: ByteArray?,
    val capturedAt: Long,
    /**
     * A semantic payment identity. It is scoped to the wallet and payment kind, and prefers the
     * transaction/order identifier over the changing accessibility-node tree.
     */
    val eventFingerprint: String,
    /** How long callbacks with this identity represent the same completed payment page. */
    val dedupWindowMs: Long
)

data class PaymentCaptureIdentity(
    val fingerprint: String,
    val dedupWindowMs: Long
)

object PaymentFingerprint {
    const val MAX_TEXT_LENGTH = 6_000
    const val IDENTIFIED_PAYMENT_WINDOW_MS = 5 * 60_000L
    const val UNIDENTIFIED_PAYMENT_WINDOW_MS = 20_000L

    private val clockPattern = Regex("(?<!\\d)\\d{1,2}:\\d{2}(?::\\d{2})?(?!\\d)")
    private val datePattern = Regex("(?<!\\d)\\d{4}[年/-]\\d{1,2}(?:[月/-]\\d{1,2}日?)?(?!\\d)")
    private val paymentIdentifierPattern = Regex(
        "(?:交易单号|交易号|订单号|商户单号|商家订单号|支付单号|支付流水号|流水号)\\s*[:：]?\\s*([A-Za-z0-9][A-Za-z0-9_-]{5,})",
        RegexOption.IGNORE_CASE
    )
    private val currencyAmountPattern = Regex("(?:￥|¥)\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
    private val labelledAmountPattern = Regex("(?:支付金额|付款金额|交易金额|金额)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*元?")

    fun normalizedText(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_TEXT_LENGTH)

    fun captureIdentity(platform: PaymentPlatform, kind: PaymentKind, text: String): PaymentCaptureIdentity {
        // Accessibility callbacks for one page can contain different subsets of nodes. Prefer a
        // wallet transaction identifier; otherwise use a short, platform-scoped amount window.
        // This makes one Alipay completion page idempotent without suppressing a WeChat payment
        // made seconds later.
        val stableText = normalizedText(text)
            .replace(clockPattern, "<time>")
            .replace(datePattern, "<date>")
        val identifier = paymentIdentifierPattern.find(stableText)?.groupValues?.getOrNull(1)?.lowercase()
        val identity = when {
            !identifier.isNullOrBlank() -> "id:$identifier"
            else -> {
                val amount = currencyAmountPattern.find(stableText)?.groupValues?.getOrNull(1)
                    ?: labelledAmountPattern.find(stableText)?.groupValues?.getOrNull(1)
                if (!amount.isNullOrBlank()) "amount:$amount" else "page:$stableText"
            }
        }
        return PaymentCaptureIdentity(
            fingerprint = sha256("${platform.name}|${kind.name}|$identity"),
            dedupWindowMs = if (identifier == null) UNIDENTIFIED_PAYMENT_WINDOW_MS else IDENTIFIED_PAYMENT_WINDOW_MS
        )
    }

    fun create(platform: PaymentPlatform, kind: PaymentKind, text: String): String =
        captureIdentity(platform, kind, text).fingerprint

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
