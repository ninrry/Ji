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
    val eventFingerprint: String
)

object PaymentFingerprint {
    const val MAX_TEXT_LENGTH = 6_000

    private val clockPattern = Regex("(?<!\\d)\\d{1,2}:\\d{2}(?::\\d{2})?(?!\\d)")
    private val datePattern = Regex("(?<!\\d)\\d{4}[年/-]\\d{1,2}(?:[月/-]\\d{1,2}日?)?(?!\\d)")
    private val longIdPattern = Regex("(?<!\\d)\\d{6,}(?!\\d)")

    fun normalizedText(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_TEXT_LENGTH)

    fun create(platform: PaymentPlatform, kind: PaymentKind, text: String): String {
        // Status-bar clocks, dates and long transaction identifiers often change while the same
        // completion page is still displayed. They must not create a second cloud task.
        val stableText = normalizedText(text)
            .replace(clockPattern, "<time>")
            .replace(datePattern, "<date>")
            .replace(longIdPattern, "<id>")
        val value = "${platform.name}|${kind.name}|$stableText"
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
