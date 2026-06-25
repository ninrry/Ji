package luzzr.ji.core.vlm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import luzzr.ji.domain.model.PaymentKind
import luzzr.ji.domain.model.PaymentPlatform
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit

data class VlmTransactionResult(
    val id: Long = 0,
    val amount: Long,
    val category: String,
    val note: String,
    val platform: PaymentPlatform = PaymentPlatform.MANUAL,
    val paymentKind: PaymentKind = PaymentKind.MERCHANT_PAYMENT,
    val tradeId: String? = null,
    val completedAt: Long? = null,
    val confidence: Double = 0.0,
    val isFallback: Boolean = false
)

class VlmClient(
    private val apiKey: String = "",
    private val modelId: String = "mimo-v2.5",
    private val fallbackRuleEngine: LocalFallbackRuleEngine = LocalFallbackRuleEngine.default(),
    private val apiUrl: String = DEFAULT_API_URL
) {
    companion object {
        const val DEFAULT_API_URL = "https://opencode.ai/zen/go/v1/chat/completions"
        const val PREF_API_URL = "opencode_api_url"
        private const val MAX_AMOUNT_FEN = 9_999_999L
        private const val MAX_NOTE_LENGTH = 100
        private const val MIN_CONFIDENCE = 0.85
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val VALID_CATEGORIES = setOf("餐饮", "交通", "购物", "娱乐", "犒劳", "其它")
        private val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
        private val JSON_PARSER = Json { ignoreUnknownKeys = true }
    }

    suspend fun parsePayment(
        screenText: String,
        imageBytes: ByteArray?,
        expectedPlatform: PaymentPlatform,
        expectedKind: PaymentKind
    ): VlmTransactionResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val prompt = paymentPrompt(screenText, expectedPlatform, expectedKind)
        val messages = JSONArray().put(JSONObject().apply {
            put("role", "user")
            if (imageBytes == null) {
                put("content", prompt)
            } else {
                put("content", JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", prompt))
                    val imageUrl = "data:image/jpeg;base64,${Base64.getEncoder().encodeToString(imageBytes)}"
                    put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageUrl)))
                })
            }
        })
        parsePaymentResponse(executeChat(messages, jsonResponse = true), expectedPlatform, expectedKind)
    }

    /** Kept for connection diagnostics and legacy unit coverage; automatic billing always uses parsePayment. */
    suspend fun parseScreen(screenText: String): VlmTransactionResult? = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            val result = runCatching {
                val messages = JSONArray().put(JSONObject().put("role", "user").put("content", genericBillPrompt(screenText)))
                parseGenericResponse(executeChat(messages, jsonResponse = true))
            }.getOrNull()
            if (result != null) return@withContext result
        }
        parseLocalFallback(screenText)
    }

    suspend fun parseScreenImage(imageBytes: ByteArray, screenText: String): VlmTransactionResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext parseLocalFallback(screenText)
        val messages = JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", genericBillPrompt(screenText)))
                val imageUrl = "data:image/jpeg;base64,${Base64.getEncoder().encodeToString(imageBytes)}"
                put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageUrl)))
            })
        })
        runCatching { parseGenericResponse(executeChat(messages, jsonResponse = true)) }
            .getOrElse { parseLocalFallback(screenText) }
    }

    suspend fun testChat(prompt: String): String = withContext(Dispatchers.IO) {
        executeChat(JSONArray().put(JSONObject().put("role", "user").put("content", prompt)), jsonResponse = false)
    }

    suspend fun testChatWithImage(prompt: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val messages = JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", prompt))
                val imageUrl = "data:image/jpeg;base64,${Base64.getEncoder().encodeToString(imageBytes)}"
                put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageUrl)))
            })
        })
        executeChat(messages, jsonResponse = false)
    }

    private fun executeChat(messages: JSONArray, jsonResponse: Boolean): String {
        val payload = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("temperature", 0)
            put("max_tokens", 256)
            if (jsonResponse) put("response_format", JSONObject().put("type", "json_object"))
        }
        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        HTTP_CLIENT.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error(httpErrorMessage(response.code))
            return JSONObject(body).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        401, 403 -> "云端识别密钥无效或无权限"
        408 -> "云端识别请求超时"
        429 -> "云端识别请求过于频繁，请稍后再试"
        in 500..599 -> "云端识别服务暂时不可用"
        else -> "云端识别请求失败（HTTP $code）"
    }

    private fun paymentPrompt(text: String, platform: PaymentPlatform, kind: PaymentKind): String = """
        你是支付完成页结构化识别器。只分析一次已经完成的交易，绝不猜测。
        预期平台：${platform.name}；预期交易种类：${kind.name}。
        仅返回 JSON：
        {"status":"SUCCESS|NOT_A_COMPLETED_PAYMENT","amount":"15.50","category":"餐饮|交通|购物|娱乐|犒劳|其它","kind":"${kind.name}","platform":"${platform.name}","note":"商户或对手方","trade_id":"交易号或空字符串","completed_at":"ISO-8601 或空字符串","confidence":0.0}
        金额必须是实际扣款金额；如果页面失败、退款、取消、处理中或无法确定，status 必须为 NOT_A_COMPLETED_PAYMENT。
        页面文本：
        ${text.take(6_000)}
    """.trimIndent()

    private fun genericBillPrompt(text: String): String = """
        从支付页面文本中提取账单，只返回 JSON：
        {"amount":"15.50","category":"餐饮|交通|购物|娱乐|犒劳|其它","note":"商户或备注"}
        页面文本：${text.take(6_000)}
    """.trimIndent()

    internal fun parsePaymentResponse(
        content: String,
        expectedPlatform: PaymentPlatform,
        expectedKind: PaymentKind
    ): VlmTransactionResult? {
        val json = jsonObjectFrom(content)
        if (json.string("status") != "SUCCESS") return null
        val platform = runCatching { PaymentPlatform.valueOf(json.string("platform").orEmpty()) }.getOrNull() ?: return null
        val kind = runCatching { PaymentKind.valueOf(json.string("kind").orEmpty()) }.getOrNull() ?: return null
        if (platform != expectedPlatform || kind != expectedKind) return null
        val confidence = json.string("confidence")?.toDoubleOrNull() ?: 0.0
        if (confidence < MIN_CONFIDENCE || confidence > 1.0) return null
        val amount = amountToFen(json.string("amount")) ?: return null
        val category = if (kind == PaymentKind.MERCHANT_PAYMENT) {
            json.string("category").orEmpty().takeIf { it in VALID_CATEGORIES } ?: "其它"
        } else {
            kind.defaultCategory
        }
        return VlmTransactionResult(
            amount = amount,
            category = category,
            note = json.string("note").orEmpty().ifBlank { "自动记账" }.take(MAX_NOTE_LENGTH),
            platform = platform,
            paymentKind = kind,
            tradeId = json.string("trade_id").orEmpty().trim().takeIf { it.isNotEmpty() }?.take(128),
            completedAt = json.string("completed_at")?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            confidence = confidence
        )
    }

    private fun parseGenericResponse(content: String): VlmTransactionResult? {
        val json = jsonObjectFrom(content)
        val amount = amountToFen(json.string("amount")) ?: return null
        val category = json.string("category").orEmpty().takeIf { it in VALID_CATEGORIES } ?: "其它"
        return VlmTransactionResult(
            amount = amount,
            category = category,
            note = json.string("note").orEmpty().ifBlank { "自动记账" }.take(MAX_NOTE_LENGTH),
            confidence = 1.0
        )
    }

    private fun jsonObjectFrom(content: String): JsonObject = try {
        JSON_PARSER.parseToJsonElement(content).jsonObject
    } catch (_: Exception) {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) error("VLM response is not JSON")
        JSON_PARSER.parseToJsonElement(content.substring(start, end + 1)).jsonObject
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun amountToFen(value: Any?): Long? = runCatching {
        val amount = BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP)
        amount.movePointRight(2).longValueExact().takeIf { it in 1..MAX_AMOUNT_FEN }
    }.getOrNull()

    fun parseLocalFallback(screenText: String): VlmTransactionResult? {
        return fallbackRuleEngine.parse(screenText)
    }
}
