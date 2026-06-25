package luzzr.ji.core.vlm

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode

class LocalFallbackRuleEngine private constructor(
    private val amountPatterns: List<Regex>,
    private val categoryRules: List<CategoryRule>
) {
    companion object {
        const val CUSTOM_RULES_PREF_KEY = "local_fallback_rules_json"
        private const val MAX_AMOUNT_FEN = 9_999_999L
        private const val MAX_NOTE_LENGTH = 100
        private val JSON = Json { ignoreUnknownKeys = true }
        private const val DEFAULT_RULES_JSON = """
            {
              "amount_patterns": [
                "(?:¥|￥)\\s*-?\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
                "(?:支付|付款|扣款|交易金额|共计|金额)\\s*(?:¥|￥)?\\s*-?\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
                "(?<!\\d)-\\s*([0-9]+(?:\\.[0-9]{1,2})?)(?!\\d)",
                "-?\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*元"
              ],
              "note_patterns": [
                "(?:商户全称|收款方全称|商品说明|收款方|交易对象|商品|商户|对方)\\s*[:：]?\\s*([^\\s¥￥]{2,40})"
              ],
              "category_rules": [
                {
                  "category": "餐饮",
                  "patterns": ["麦当劳|肯德基|星巴克|瑞幸|罗森|全家|美团|饿了么|外卖|餐饮|食堂|饭|面|粉|咖啡|奶茶|烤鸭|大学"]
                },
                {
                  "category": "交通",
                  "patterns": ["滴滴|地铁|公交|高铁|铁路|火车|出行|打车|停车|加油|充电|高速|机场"]
                },
                {
                  "category": "购物",
                  "patterns": ["淘宝|天猫|京东|拼多多|超市|便利店|购物|零售|百货|服饰|衣服|鞋|数码|无人零售|货柜|轻购云"]
                },
                {
                  "category": "娱乐",
                  "patterns": ["电影|影院|游戏|会员|音乐|娱乐|剧本|KTV|演出|直播"]
                },
                {
                  "category": "犒劳",
                  "patterns": ["红包|打赏|犒劳"]
                }
              ]
            }
        """

        fun default(): LocalFallbackRuleEngine = fromJson(DEFAULT_RULES_JSON)

        fun from(context: Context, sharedPreferences: SharedPreferences): LocalFallbackRuleEngine {
            val custom = sharedPreferences.getString(CUSTOM_RULES_PREF_KEY, null)
            if (!custom.isNullOrBlank()) {
                runCatching { return fromJson(custom) }
            }
            val assetJson = runCatching {
                context.assets.open("local_fallback_rules.json").bufferedReader().use { it.readText() }
            }.getOrNull()
            if (!assetJson.isNullOrBlank()) {
                runCatching { return fromJson(assetJson) }
            }
            return default()
        }

        fun fromJson(jsonText: String): LocalFallbackRuleEngine {
            val json = JSON.parseToJsonElement(jsonText).jsonObject
            val amountPatterns = json.array("amount_patterns").toRegexList()
            val notePatterns = json.array("note_patterns").toRegexList()
            val categoryRules = json.array("category_rules").toCategoryRules()
            return LocalFallbackRuleEngine(
                amountPatterns = amountPatterns.ifEmpty { default().amountPatterns },
                categoryRules = categoryRules.ifEmpty {
                    listOf(CategoryRule("其它", notePatterns))
                }
            )
        }

        private fun JsonObject.array(key: String): JsonArray? =
            this[key]?.let { runCatching { it.jsonArray }.getOrNull() }

        private fun JsonArray?.toRegexList(): List<Regex> {
            if (this == null) return emptyList()
            return mapNotNull { element ->
                val pattern = element.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() }
                pattern?.let { Regex(it, RegexOption.IGNORE_CASE) }
            }
        }

        private fun JsonArray?.toCategoryRules(): List<CategoryRule> {
            if (this == null) return emptyList()
            return mapNotNull { element ->
                val item = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                val category = item["category"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val patterns = item.array("patterns").toRegexList()
                if (patterns.isNotEmpty()) {
                    CategoryRule(category, patterns)
                } else {
                    null
                }
            }
        }
    }

    private data class CategoryRule(
        val category: String,
        val patterns: List<Regex>
    )

    fun parse(screenText: String): VlmTransactionResult? {
        val text = screenText.replace(Regex("\\s+"), " ").trim()
        val amountText = amountPatterns.firstNotNullOfOrNull { regex ->
            regex.find(text)?.firstCapturedGroup()
        } ?: return null
        val amount = amountToFen(amountText) ?: return null
        val category = categoryRules.firstOrNull { rule ->
            rule.patterns.any { it.containsMatchIn(text) }
        }?.category ?: "其它"
        val note = extractNote(text).ifBlank { "自动提取" }.take(MAX_NOTE_LENGTH)
        return VlmTransactionResult(
            amount = amount,
            category = category,
            note = note,
            confidence = 1.0,
            isFallback = true
        )
    }

    private fun extractNote(text: String): String {
        val fieldPattern = Regex(
            "(?:商户全称|收款方全称|商品说明|收款方|交易对象|商品|商户|对方)\\s*[:：]?\\s*([^\\s¥￥]{2,40})"
        )
        fieldPattern.find(text)?.firstCapturedGroup()?.let { return it }
        categoryRules.asSequence()
            .flatMap { rule -> rule.patterns.asSequence() }
            .mapNotNull { regex -> regex.find(text)?.value }
            .firstOrNull { it.isNotBlank() }
            ?.let { return it }
        return ""
    }

    private fun MatchResult.firstCapturedGroup(): String? =
        groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()

    private fun amountToFen(value: String): Long? = runCatching {
        val normalized = value.replace(",", "").trim()
        val amount = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
        amount.movePointRight(2).longValueExact().takeIf { it in 1..MAX_AMOUNT_FEN }
    }.getOrNull()
}
