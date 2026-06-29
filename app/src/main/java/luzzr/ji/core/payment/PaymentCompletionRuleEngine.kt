package luzzr.ji.core.payment

import android.content.Context
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data-driven payment completion rule evaluator.
 *
 * The JSON rule file is intentionally kept outside PaymentCompletionClassifier so real-world
 * wallet page patches become versioned rule data with stable rule ids and traceable decisions.
 */
class PaymentCompletionRuleEngine private constructor(
    private val ruleSet: RuleSet
) {
    fun classify(packageName: String, rawText: String): PaymentClassification {
        val text = normalize(rawText)
        if (text.length < MIN_TEXT_LENGTH) {
            return PaymentClassification.reject(
                packageName = packageName,
                textLength = text.length,
                ruleId = "common.too_short",
                reason = "normalized screen text is too short"
            )
        }

        ruleSet.commonReject.firstMatching(text)?.let { match ->
            return PaymentClassification.reject(
                packageName = packageName,
                textLength = text.length,
                ruleId = match.rule.id,
                reason = match.rule.description,
                matchedKeywords = match.keywords
            )
        }

        val platform = ruleSet.platforms.firstOrNull { packageName in it.packages }
            ?: return PaymentClassification.reject(
                packageName = packageName,
                textLength = text.length,
                ruleId = "common.unsupported_package",
                reason = "unsupported package: $packageName"
            )

        platform.reject.firstMatching(text)?.let { match ->
            return PaymentClassification.reject(
                packageName = packageName,
                textLength = text.length,
                platform = platform.platform,
                ruleId = match.rule.id,
                reason = match.rule.description,
                matchedKeywords = match.keywords
            )
        }

        platform.accept.firstMatching(text)?.let { match ->
            val signal = PaymentCompletionSignal(platform.platform, match.kind ?: PaymentKind.MERCHANT_PAYMENT)
            return PaymentClassification.accept(
                packageName = packageName,
                textLength = text.length,
                signal = signal,
                ruleId = match.rule.id,
                reason = match.rule.description,
                matchedKeywords = match.keywords
            )
        }

        return PaymentClassification.unknown(
            packageName = packageName,
            textLength = text.length,
            platform = platform.platform,
            ruleId = "${platform.id}.no_completion_signal",
            reason = "no completion rule matched"
        )
    }

    private fun List<TextRule>.firstMatching(text: String): RuleMatch? {
        for (rule in this) {
            val match = rule.match(text)
            if (match != null) return match
        }
        return null
    }

    private data class RuleSet(
        val version: Int,
        val commonReject: List<TextRule>,
        val platforms: List<PlatformRule>
    )

    private data class PlatformRule(
        val id: String,
        val platform: PaymentPlatform,
        val packages: List<String>,
        val reject: List<TextRule>,
        val accept: List<TextRule>
    )

    private data class TextRule(
        val id: String,
        val description: String,
        val all: List<String>,
        val any: List<String>,
        val minMatch: Int,
        val kind: PaymentKind? = null
    ) {
        fun match(text: String): RuleMatch? {
            val allMatched = all.all(text::contains)
            if (!allMatched) return null
            val matchedAny = any.filter(text::contains)
            val requiredAnyCount = when {
                any.isEmpty() -> 0
                minMatch <= 0 -> 1
                else -> minMatch
            }
            if (matchedAny.size < requiredAnyCount) return null
            return RuleMatch(this, kind, all + matchedAny)
        }
    }

    private data class RuleMatch(
        val rule: TextRule,
        val kind: PaymentKind?,
        val keywords: List<String>
    )

    companion object {
        private const val MIN_TEXT_LENGTH = 4
        private const val ASSET_NAME = "payment_completion_rules.json"

        fun fromAssets(context: Context): PaymentCompletionRuleEngine {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            return fromJson(json)
        }

        fun fromClasspathResource(resourceName: String = ASSET_NAME): PaymentCompletionRuleEngine? {
            val stream = PaymentCompletionRuleEngine::class.java.classLoader
                ?.getResourceAsStream(resourceName)
                ?: return null
            return stream.bufferedReader().use { fromJson(it.readText()) }
        }

        fun fromJson(jsonText: String): PaymentCompletionRuleEngine {
            val root = JSONObject(jsonText)
            val ruleSet = RuleSet(
                version = root.optInt("version", 1),
                commonReject = root.optJSONArray("common_reject").toTextRules(),
                platforms = root.optJSONArray("platforms").toPlatformRules()
            )
            require(ruleSet.platforms.isNotEmpty()) { "payment completion rules must define platforms" }
            return PaymentCompletionRuleEngine(ruleSet)
        }

        fun normalize(rawText: String): String = rawText.replace(Regex("\\s+"), "")

        private fun JSONArray?.toPlatformRules(): List<PlatformRule> {
            if (this == null) return emptyList()
            return (0 until length()).map { index ->
                val item = getJSONObject(index)
                PlatformRule(
                    id = item.getString("id"),
                    platform = PaymentPlatform.valueOf(item.getString("platform")),
                    packages = item.getJSONArray("packages").toStringList(),
                    reject = item.optJSONArray("reject").toTextRules(),
                    accept = item.optJSONArray("accept").toTextRules()
                )
            }
        }

        private fun JSONArray?.toTextRules(): List<TextRule> {
            if (this == null) return emptyList()
            return (0 until length()).map { index ->
                val item = getJSONObject(index)
                TextRule(
                    id = item.getString("id"),
                    description = item.optString("description", item.getString("id")),
                    all = item.optJSONArray("all").toStringList(),
                    any = item.optJSONArray("any").toStringList(),
                    minMatch = item.optInt("min_match", 0),
                    kind = item.optString("kind").takeIf { it.isNotBlank() }?.let(PaymentKind::valueOf)
                )
            }
        }

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { index ->
                optString(index).takeIf { it.isNotBlank() }
            }
        }
    }
}

data class PaymentClassification(
    val signal: PaymentCompletionSignal?,
    val trace: PaymentRuleTrace
) {
    companion object {
        fun accept(
            packageName: String,
            textLength: Int,
            signal: PaymentCompletionSignal,
            ruleId: String,
            reason: String,
            matchedKeywords: List<String>
        ): PaymentClassification = PaymentClassification(
            signal = signal,
            trace = PaymentRuleTrace(
                packageName = packageName,
                normalizedTextLength = textLength,
                platform = signal.platform,
                kind = signal.kind,
                decision = PaymentRuleDecision.ACCEPT,
                ruleId = ruleId,
                reason = reason,
                matchedKeywords = matchedKeywords
            )
        )

        fun reject(
            packageName: String,
            textLength: Int,
            platform: PaymentPlatform? = null,
            ruleId: String,
            reason: String,
            matchedKeywords: List<String> = emptyList()
        ): PaymentClassification = PaymentClassification(
            signal = null,
            trace = PaymentRuleTrace(
                packageName = packageName,
                normalizedTextLength = textLength,
                platform = platform,
                kind = null,
                decision = PaymentRuleDecision.REJECT,
                ruleId = ruleId,
                reason = reason,
                matchedKeywords = matchedKeywords
            )
        )

        fun unknown(
            packageName: String,
            textLength: Int,
            platform: PaymentPlatform,
            ruleId: String,
            reason: String
        ): PaymentClassification = PaymentClassification(
            signal = null,
            trace = PaymentRuleTrace(
                packageName = packageName,
                normalizedTextLength = textLength,
                platform = platform,
                kind = null,
                decision = PaymentRuleDecision.UNKNOWN,
                ruleId = ruleId,
                reason = reason,
                matchedKeywords = emptyList()
            )
        )
    }
}

data class PaymentRuleTrace(
    val packageName: String,
    val normalizedTextLength: Int,
    val platform: PaymentPlatform?,
    val kind: PaymentKind?,
    val decision: PaymentRuleDecision,
    val ruleId: String,
    val reason: String,
    val matchedKeywords: List<String>
)

enum class PaymentRuleDecision {
    ACCEPT,
    REJECT,
    UNKNOWN
}
