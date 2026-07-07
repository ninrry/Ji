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

// ── Structured API error ──────────────────────────────────────────────────────

/** Error categories that map to distinct user-facing behaviors. */
enum class VlmErrorCategory {
    /** Bad request / format / model name — developer error, not retryable. */
    BAD_REQUEST,
    /** 401 — wrong key or mixed Token-Plan/paygo keys. */
    AUTH_FAILED,
    /** 402 — account balance insufficient. Must alert user to recharge. */
    BALANCE_INSUFFICIENT,
    /** 403 — region block or key flagged by risk-control. */
    ACCESS_DENIED,
    /** 404 — model or endpoint doesn't support the requested feature. */
    NOT_FOUND,
    /** 421 — content moderation block. */
    CONTENT_BLOCKED,
    /** 429 — rate-limited. Retryable with backoff. */
    RATE_LIMITED,
    /** 5xx — server-side issue. Retryable. */
    SERVER_ERROR,
    /** Timeout / network. Retryable. */
    NETWORK_ERROR,
    /** Anything else. */
    UNKNOWN;

    val isRetryable: Boolean
        get() = this in setOf(RATE_LIMITED, SERVER_ERROR, NETWORK_ERROR)
}

class VlmApiException(
    val category: VlmErrorCategory,
    val httpStatus: Int,
    val rawBody: String,
    message: String
) : Exception(message)

// ── Result model ─────────────────────────────────────────────────────────────

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

// ── Provider enum ─────────────────────────────────────────────────────────────

enum class VlmProvider(
    val displayName: String,
    val defaultApiUrl: String,
    val defaultModel: String,
    val apiKeyPrefKey: String
) {
    XIAOMI(
        displayName = "小米 MiMo",
        defaultApiUrl = "https://api.xiaomimimo.com/v1/chat/completions",
        defaultModel = "mimo-v2.5",
        apiKeyPrefKey = "xiaomi_api_key"
    ),
    OPENCODE(
        displayName = "OpenCode Go",
        defaultApiUrl = "https://opencode.ai/zen/go/v1/chat/completions",
        defaultModel = "mimo-v2.5",
        apiKeyPrefKey = "opencode_api_key"
    )
}

// ── Client ───────────────────────────────────────────────────────────────────

class VlmClient(
    private val apiKey: String = "",
    private val modelId: String = "mimo-v2.5",
    private val fallbackRuleEngine: LocalFallbackRuleEngine = LocalFallbackRuleEngine.default(),
    private val apiUrl: String = VlmProvider.XIAOMI.defaultApiUrl,
    private val provider: VlmProvider = VlmProvider.XIAOMI
) {
    companion object {
        const val PREF_API_URL = "opencode_api_url"
        const val PREF_PROVIDER = "vlm_provider"
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

        fun resolveEndpoint(provider: VlmProvider, prefs: android.content.SharedPreferences): Pair<String, String> {
            val savedUrl = prefs.getString(PREF_API_URL, null)
            val savedModel = prefs.getString("opencode_model_id", null)
            val url = savedUrl?.takeIf { it.isNotBlank() } ?: provider.defaultApiUrl
            val model = savedModel?.takeIf { it.isNotBlank() } ?: provider.defaultModel
            return url to model
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

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

    /** Check account balance for Xiaomi pay-as-you-go (call on settings page). */
    suspend fun checkBalance(): String = withContext(Dispatchers.IO) {
        if (provider != VlmProvider.XIAOMI)
            return@withContext "余额查询仅支持小米直连供应商"
        val request = Request.Builder()
            .url("https://api.xiaomimimo.com/v1/dashboard/billing/credit_grants")
            .header("Authorization", "Bearer $apiKey")
            .header("api-key", apiKey)
            .get()
            .build()
        try {
            HTTP_CLIENT.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val cat = categorizeHttpError(resp.code)
                    throw VlmApiException(cat, resp.code, body, describeCategory(cat, resp.code))
                }
                body
            }
        } catch (e: VlmApiException) { throw e }
          catch (e: Exception) { throw VlmApiException(VlmErrorCategory.NETWORK_ERROR, 0, "", e.message ?: "网络异常") }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun executeChat(messages: JSONArray, jsonResponse: Boolean): String {
        val payload = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("temperature", 0)
            put("max_tokens", 200)
            if (jsonResponse) put("response_format", JSONObject().put("type", "json_object"))
        }
        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .apply { if (provider == VlmProvider.XIAOMI) header("api-key", apiKey) }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        try {
            HTTP_CLIENT.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val cat = categorizeHttpError(response.code)
                    throw VlmApiException(cat, response.code, body, describeCategory(cat, response.code))
                }
                return JSONObject(body).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content")
            }
        } catch (e: VlmApiException) { throw e }
          catch (e: java.net.SocketTimeoutException) {
              throw VlmApiException(VlmErrorCategory.NETWORK_ERROR, 0, "", "云端识别请求超时")
          }
          catch (e: java.net.ConnectException) {
              throw VlmApiException(VlmErrorCategory.NETWORK_ERROR, 0, "", "无法连接云端识别服务")
          }
          catch (e: Exception) {
              if (e is VlmApiException) throw e
              throw VlmApiException(VlmErrorCategory.NETWORK_ERROR, 0, "", e.message ?: "网络异常")
          }
    }

    private fun categorizeHttpError(code: Int): VlmErrorCategory = when (code) {
        400 -> VlmErrorCategory.BAD_REQUEST
        401 -> VlmErrorCategory.AUTH_FAILED
        402 -> VlmErrorCategory.BALANCE_INSUFFICIENT
        403 -> VlmErrorCategory.ACCESS_DENIED
        404 -> VlmErrorCategory.NOT_FOUND
        421 -> VlmErrorCategory.CONTENT_BLOCKED
        429 -> VlmErrorCategory.RATE_LIMITED
        in 500..599 -> VlmErrorCategory.SERVER_ERROR
        else -> VlmErrorCategory.UNKNOWN
    }

    private fun describeCategory(cat: VlmErrorCategory, httpCode: Int): String = when (cat) {
        VlmErrorCategory.BAD_REQUEST ->
            "请求格式错误（$httpCode），请检查模型名称或联系开发者"
        VlmErrorCategory.AUTH_FAILED ->
            "API 密钥无效或类型不匹配（$httpCode），请在设置中重新配置"
        VlmErrorCategory.BALANCE_INSUFFICIENT ->
            "账户余额不足（$httpCode），请前往小米开放平台充值"
        VlmErrorCategory.ACCESS_DENIED ->
            "访问被拒绝（$httpCode），可能是地区限制或密钥被风控"
        VlmErrorCategory.NOT_FOUND ->
            "接口或模型不存在（$httpCode），请确认模型名称"
        VlmErrorCategory.CONTENT_BLOCKED ->
            "内容被安全审核拦截（$httpCode）"
        VlmErrorCategory.RATE_LIMITED ->
            "请求过于频繁（$httpCode），请稍后再试"
        VlmErrorCategory.SERVER_ERROR ->
            "云端服务暂时不可用（$httpCode），稍后自动重试"
        VlmErrorCategory.NETWORK_ERROR ->
            "网络连接异常，请检查网络设置"
        VlmErrorCategory.UNKNOWN ->
            "云端识别请求失败（HTTP $httpCode）"
    }

    private fun paymentPrompt(text: String, platform: PaymentPlatform, kind: PaymentKind): String = """
        支付识别器。平台:${platform.name} 类型:${kind.name}
        非完成页/退款/取消/处理中→status:0
        JSON:{"s":1,"a":"12.50","c":"餐饮|交通|购物|娱乐|犒劳|其它","n":"商户","t":"交易号"}
        文本:${text.take(3_000)}
    """.trimIndent()

    private fun genericBillPrompt(text: String): String = """
        提取账单。JSON:{"a":"12.50","c":"餐饮|交通|购物|娱乐|犒劳|其它","n":"备注"}
        文本:${text.take(3_000)}
    """.trimIndent()

    internal fun parsePaymentResponse(
        content: String,
        expectedPlatform: PaymentPlatform,
        expectedKind: PaymentKind
    ): VlmTransactionResult? {
        val json = jsonObjectFrom(content)
        val isSuccess = json.int("s") == 1 || json.string("status") == "SUCCESS"
        if (!isSuccess) return null

        if (json.string("status") == "SUCCESS") {
            val platform = runCatching { PaymentPlatform.valueOf(json.string("platform").orEmpty()) }.getOrNull() ?: return null
            val kind = runCatching { PaymentKind.valueOf(json.string("kind").orEmpty()) }.getOrNull() ?: return null
            if (platform != expectedPlatform || kind != expectedKind) return null
        }

        val confidence = json.string("confidence")?.toDoubleOrNull() ?: 0.95
        if (confidence < MIN_CONFIDENCE || confidence > 1.0) return null
        val amount = amountToFen(json.string("a") ?: json.string("amount")) ?: return null
        val category = if (expectedKind == PaymentKind.MERCHANT_PAYMENT) {
            (json.string("c") ?: json.string("category")).orEmpty().takeIf { it in VALID_CATEGORIES } ?: "其它"
        } else {
            expectedKind.defaultCategory
        }
        val note = (json.string("n") ?: json.string("note")).orEmpty().ifBlank { "自动记账" }.take(MAX_NOTE_LENGTH)
        val tradeId = (json.string("t") ?: json.string("trade_id")).orEmpty().trim().takeIf { it.isNotEmpty() }?.take(128)
        val completedAt = json.string("completed_at")?.takeIf { it.isNotBlank() }?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        }
        return VlmTransactionResult(
            amount = amount,
            category = category,
            note = note,
            platform = expectedPlatform,
            paymentKind = expectedKind,
            tradeId = tradeId,
            completedAt = completedAt,
            confidence = confidence
        )
    }

    private fun parseGenericResponse(content: String): VlmTransactionResult? {
        val json = jsonObjectFrom(content)
        val amount = amountToFen(json.string("a") ?: json.string("amount")) ?: return null
        val category = (json.string("c") ?: json.string("category")).orEmpty().takeIf { it in VALID_CATEGORIES } ?: "其它"
        return VlmTransactionResult(
            amount = amount,
            category = category,
            note = (json.string("n") ?: json.string("note")).orEmpty().ifBlank { "自动记账" }.take(MAX_NOTE_LENGTH),
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

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun amountToFen(value: Any?): Long? = runCatching {
        val amount = BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP)
        amount.movePointRight(2).longValueExact().takeIf { it in 1..MAX_AMOUNT_FEN }
    }.getOrNull()

    fun parseLocalFallback(screenText: String): VlmTransactionResult? {
        return fallbackRuleEngine.parse(screenText)
    }
}
