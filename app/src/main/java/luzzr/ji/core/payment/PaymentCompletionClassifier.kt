package luzzr.ji.core.payment

import android.content.Context
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform

/** Compatibility facade for the data-driven payment completion rule engine. */
data class PaymentCompletionSignal(
    val platform: PaymentPlatform,
    val kind: PaymentKind
)

object PaymentCompletionClassifier {
    @Volatile
    private var assetEngine: PaymentCompletionRuleEngine? = null

    private val classpathEngine: PaymentCompletionRuleEngine by lazy {
        PaymentCompletionRuleEngine.fromClasspathResource()
            ?: error("payment_completion_rules.json is missing from test/runtime classpath")
    }

    fun classify(packageName: String, rawText: String): PaymentClassification =
        classpathEngine.classify(packageName, rawText)

    fun classify(context: Context, packageName: String, rawText: String): PaymentClassification =
        engine(context).classify(packageName, rawText)

    fun from(packageName: String, rawText: String): PaymentCompletionSignal? =
        classify(packageName, rawText).signal

    fun from(context: Context, packageName: String, rawText: String): PaymentCompletionSignal? =
        classify(context, packageName, rawText).signal

    /** Revalidates persisted work before a cloud request, so pre-upgrade history tasks are harmless. */
    fun isStillEligible(platform: PaymentPlatform, kind: PaymentKind, rawText: String): Boolean {
        val packageName = platform.packageNameOrNull() ?: return false
        return from(packageName, rawText)?.let { it.platform == platform && it.kind == kind } == true
    }

    fun isStillEligible(context: Context, platform: PaymentPlatform, kind: PaymentKind, rawText: String): Boolean {
        val packageName = platform.packageNameOrNull() ?: return false
        return from(context, packageName, rawText)?.let { it.platform == platform && it.kind == kind } == true
    }

    private fun engine(context: Context): PaymentCompletionRuleEngine {
        assetEngine?.let { return it }
        return synchronized(this) {
            assetEngine ?: PaymentCompletionRuleEngine.fromAssets(context.applicationContext).also { assetEngine = it }
        }
    }

    private fun PaymentPlatform.packageNameOrNull(): String? = when (this) {
        PaymentPlatform.WECHAT -> "com.tencent.mm"
        PaymentPlatform.ALIPAY -> "com.eg.android.AlipayGphone"
        PaymentPlatform.JD -> "com.jingdong.app.mall"
        PaymentPlatform.MANUAL -> null
    }
}
