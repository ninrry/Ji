package luzzr.ji.feature.settings

data class ChatMessage(
    val sender: String, // "user", "ai"
    val text: String,
    val imageLabel: String? = null
)

data class SettingsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isShizukuAuthorized: Boolean = false,
    val shizukuStatusLabel: String = "未检测到 Shizuku/Sui",
    val isBatteryOptimizationIgnored: Boolean = false,
    val budgetInput: String = "",
    val isBudgetSaved: Boolean = false,
    val opencodeApiKey: String = "",
    val opencodeApiUrl: String = "https://opencode.ai/zen/go/v1/chat/completions",
    val isApiKeySaved: Boolean = false,
    val opencodeModel: String = "mimo-v2.5",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val chatInput: String = "",
    val chatHistory: List<ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SettingsUiEvent {
    data class BudgetInputChanged(val value: String) : SettingsUiEvent
    data object SaveBudget : SettingsUiEvent
    data class ApiKeyChanged(val value: String) : SettingsUiEvent
    data class ApiUrlChanged(val value: String) : SettingsUiEvent
    data class ModelChanged(val value: String) : SettingsUiEvent
    data object SaveApiKey : SettingsUiEvent
    data object RefreshPermissions : SettingsUiEvent
    data object RequestShizukuPermission : SettingsUiEvent
    data object RequestIgnoreBatteryOptimization : SettingsUiEvent
    data object TestConnection : SettingsUiEvent
    data class ChatInputChanged(val value: String) : SettingsUiEvent
    data object SendChatMessage : SettingsUiEvent
    data class SendChatImageMessage(val imageBytes: ByteArray, val text: String) : SettingsUiEvent
    data object ClearChat : SettingsUiEvent
}

sealed interface SettingsUiEffect {
    data class ShowToast(val message: String) : SettingsUiEffect
}
