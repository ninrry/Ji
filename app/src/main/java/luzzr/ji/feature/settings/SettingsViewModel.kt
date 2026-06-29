package luzzr.ji.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import luzzr.ji.core.common.MoneyAmountParser
import luzzr.ji.core.permissions.PermissionManager
import luzzr.ji.core.vlm.VlmClient
import luzzr.ji.domain.model.Budget
import luzzr.ji.domain.usecase.ObserveBudgetUseCase
import luzzr.ji.domain.usecase.SaveBudgetUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class SettingsViewModel(
    private val observeBudgetUseCase: ObserveBudgetUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val secureStorage: luzzr.ji.core.common.SecureStorage,
    private val sharedPreferences: android.content.SharedPreferences,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsUiEffect>()
    val uiEffect: SharedFlow<SettingsUiEffect> = _uiEffect.asSharedFlow()

    private val currentYearMonth: String

    init {
        val currentMillis = System.currentTimeMillis()
        currentYearMonth = YearMonth.from(Instant.ofEpochMilli(currentMillis).atZone(zoneId).toLocalDate()).toString()

        // 观察当月预算
        observeBudgetUseCase(currentYearMonth)
            .onEach { budget ->
                if (budget != null) {
                    val inputVal = if (budget.amount % 100 == 0L) (budget.amount / 100).toString() else String.format(Locale.US, "%.2f", budget.amount / 100.0)
                    _uiState.update { it.copy(budgetInput = inputVal) }
                } else {
                    _uiState.update { it.copy(budgetInput = "0") } // 默认 0
                }
            }
            .launchIn(viewModelScope)

        // 初始化加载已保存的 API Key 和 Model (强制为 mimo-v2.5)
        val savedApiKey = secureStorage.getApiKey()
        val savedModel = "mimo-v2.5"
        val savedApiUrl = sharedPreferences.getString(VlmClient.PREF_API_URL, VlmClient.DEFAULT_API_URL)
            ?: VlmClient.DEFAULT_API_URL
        sharedPreferences.edit().putString("opencode_model_id", savedModel).apply()
        _uiState.update { it.copy(opencodeApiKey = savedApiKey, opencodeApiUrl = savedApiUrl, opencodeModel = savedModel) }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.BudgetInputChanged -> {
                _uiState.update { it.copy(budgetInput = event.value, isBudgetSaved = false) }
            }
            SettingsUiEvent.SaveBudget -> {
                saveBudget()
            }
            is SettingsUiEvent.ApiKeyChanged -> {
                _uiState.update { it.copy(opencodeApiKey = event.value, isApiKeySaved = false) }
            }
            is SettingsUiEvent.ApiUrlChanged -> {
                _uiState.update { it.copy(opencodeApiUrl = event.value, isApiKeySaved = false) }
            }
            is SettingsUiEvent.ModelChanged -> {
                _uiState.update { it.copy(opencodeModel = "mimo-v2.5", isApiKeySaved = false) }
            }
            SettingsUiEvent.SaveApiKey -> {
                saveApiKey()
            }
            SettingsUiEvent.RefreshPermissions -> {
                Unit
            }
            SettingsUiEvent.RequestShizukuPermission -> {
                val requested = PermissionManager.requestShizukuScreenshotPermission()
                viewModelScope.launch {
                    _uiEffect.emit(SettingsUiEffect.ShowToast(if (requested) "已请求 Shizuku 截图授权" else "Shizuku/Sui 未运行或已拒绝授权"))
                }
            }
            SettingsUiEvent.RequestIgnoreBatteryOptimization -> {
                viewModelScope.launch {
                    _uiEffect.emit(SettingsUiEffect.ShowToast("请在系统页面允许 Ji 忽略电池优化"))
                }
            }
            SettingsUiEvent.TestConnection -> {
                testConnection()
            }
            is SettingsUiEvent.ChatInputChanged -> {
                _uiState.update { it.copy(chatInput = event.value) }
            }
            SettingsUiEvent.SendChatMessage -> {
                sendChatMessage()
            }
            is SettingsUiEvent.SendChatImageMessage -> {
                sendChatImageMessage(event.imageBytes, event.text)
            }
            SettingsUiEvent.ClearChat -> {
                _uiState.update { it.copy(chatHistory = emptyList(), chatInput = "") }
            }
        }
    }

    fun refreshPermissions(context: android.content.Context) {
        val accessibilityEnabled = PermissionManager.isAccessibilityServiceEnabled(context)
        val shizukuStatus = PermissionManager.shizukuStatus()
        val shizukuAuthorized = shizukuStatus == luzzr.ji.core.shizuku.ShizukuScreenshotGateway.Status.AUTHORIZED
        val shizukuLabel = when (shizukuStatus) {
            luzzr.ji.core.shizuku.ShizukuScreenshotGateway.Status.AUTHORIZED -> "Shizuku/Sui 已授权"
            luzzr.ji.core.shizuku.ShizukuScreenshotGateway.Status.PERMISSION_REQUIRED -> "需要授予 Shizuku 截图权限"
            luzzr.ji.core.shizuku.ShizukuScreenshotGateway.Status.DENIED -> "Shizuku 截图权限已拒绝"
            luzzr.ji.core.shizuku.ShizukuScreenshotGateway.Status.UNAVAILABLE -> "未检测到正在运行的 Shizuku/Sui"
        }
        val ignoringBatteryOptimizations = PermissionManager.isIgnoringBatteryOptimizations(context)
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = accessibilityEnabled,
                isShizukuAuthorized = shizukuAuthorized,
                shizukuStatusLabel = shizukuLabel,
                isBatteryOptimizationIgnored = ignoringBatteryOptimizations
            )
        }
    }

    private fun saveBudget() {
        val amount = MoneyAmountParser.yuanToFenOrNull(_uiState.value.budgetInput)
        if (amount == null) {
            _uiState.update { it.copy(errorMessage = "预算金额无效，最多保留两位小数") }
            return
        }

        viewModelScope.launch {
            saveBudgetUseCase(Budget(currentYearMonth, amount))
                .onSuccess {
                    _uiState.update { it.copy(isBudgetSaved = true, errorMessage = null) }
                    _uiEffect.emit(SettingsUiEffect.ShowToast("默认预算设置已保存"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    private fun saveApiKey() {
        val key = _uiState.value.opencodeApiKey.trim()
        val apiUrl = normalizedApiUrlOrNull(_uiState.value.opencodeApiUrl)
        if (apiUrl == null) {
            _uiState.update { it.copy(errorMessage = "云端服务地址必须是 HTTPS 链接") }
            return
        }
        val model = "mimo-v2.5"
        secureStorage.saveApiKey(key)
        sharedPreferences.edit()
            .putString("opencode_model_id", model)
            .putString(VlmClient.PREF_API_URL, apiUrl)
            .apply()
        _uiState.update { it.copy(isApiKeySaved = true, opencodeModel = model, opencodeApiUrl = apiUrl, errorMessage = null) }
        viewModelScope.launch {
            _uiEffect.emit(SettingsUiEffect.ShowToast("API 密钥与模型配置已保存"))
        }
    }

    private fun testConnection() {
        val apiKey = _uiState.value.opencodeApiKey.trim()
        val model = _uiState.value.opencodeModel.trim()
        val apiUrl = normalizedApiUrlOrNull(_uiState.value.opencodeApiUrl)
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(connectionTestResult = "Failed: 密钥为空") }
            return
        }
        if (apiUrl == null) {
            _uiState.update { it.copy(connectionTestResult = "Failed: 云端服务地址必须是 HTTPS 链接") }
            return
        }

        _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
        viewModelScope.launch {
            try {
                val client = VlmClient(apiKey = apiKey, modelId = model, apiUrl = apiUrl)
                val reply = client.testChat("Ping")
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionTestResult = "Success: 连接正常，模型回复: $reply"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionTestResult = "Failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun sendChatMessage() {
        val apiKey = _uiState.value.opencodeApiKey.trim()
        val model = _uiState.value.opencodeModel.trim()
        val apiUrl = normalizedApiUrlOrNull(_uiState.value.opencodeApiUrl)
        val input = _uiState.value.chatInput.trim()
        if (input.isBlank()) return
        if (apiKey.isBlank()) {
            val newHistory = _uiState.value.chatHistory + ChatMessage("user", input) + ChatMessage("ai", "错误：未配置 API 密钥")
            _uiState.update { it.copy(chatHistory = newHistory, chatInput = "") }
            return
        }
        if (apiUrl == null) {
            val newHistory = _uiState.value.chatHistory + ChatMessage("user", input) + ChatMessage("ai", "错误：云端服务地址必须是 HTTPS 链接")
            _uiState.update { it.copy(chatHistory = newHistory, chatInput = "") }
            return
        }

        val updatedHistory = _uiState.value.chatHistory + ChatMessage("user", input)
        _uiState.update {
            it.copy(
                chatHistory = updatedHistory,
                chatInput = "",
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val client = VlmClient(apiKey = apiKey, modelId = model, apiUrl = apiUrl)
                val reply = client.testChat(input)
                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + ChatMessage("ai", reply),
                        isChatLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + ChatMessage("ai", "请求失败：${e.message}"),
                        isChatLoading = false
                    )
                }
            }
        }
    }

    private fun sendChatImageMessage(imageBytes: ByteArray, prompt: String) {
        val apiKey = _uiState.value.opencodeApiKey.trim()
        val model = _uiState.value.opencodeModel.trim()
        val apiUrl = normalizedApiUrlOrNull(_uiState.value.opencodeApiUrl)
        if (apiKey.isBlank()) {
            val newHistory = _uiState.value.chatHistory + ChatMessage("user", prompt, imageLabel = "图片消息") + ChatMessage("ai", "错误：未配置 API 密钥")
            _uiState.update { it.copy(chatHistory = newHistory) }
            return
        }
        if (apiUrl == null) {
            val newHistory = _uiState.value.chatHistory + ChatMessage("user", prompt, imageLabel = "图片消息") + ChatMessage("ai", "错误：云端服务地址必须是 HTTPS 链接")
            _uiState.update { it.copy(chatHistory = newHistory) }
            return
        }

        val updatedHistory = _uiState.value.chatHistory + ChatMessage("user", prompt, imageLabel = "图片消息")
        _uiState.update {
            it.copy(
                chatHistory = updatedHistory,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val client = VlmClient(apiKey = apiKey, modelId = model, apiUrl = apiUrl)
                val reply = client.testChatWithImage(prompt, imageBytes)
                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + ChatMessage("ai", reply),
                        isChatLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + ChatMessage("ai", "请求失败：${e.message}"),
                        isChatLoading = false
                    )
                }
            }
        }
    }

    private fun normalizedApiUrlOrNull(value: String): String? {
        val trimmed = value.trim().ifBlank { VlmClient.DEFAULT_API_URL }
        return trimmed.takeIf { it.startsWith("https://", ignoreCase = true) }
    }
}
