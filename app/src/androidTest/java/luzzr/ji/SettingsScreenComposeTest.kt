package luzzr.ji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import luzzr.ji.core.design.JiTheme
import luzzr.ji.feature.settings.SettingsScreen
import luzzr.ji.feature.settings.SettingsUiEvent
import luzzr.ji.feature.settings.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreenRendersStableSectionsAndDispatchesBudgetEvents() {
        val events = mutableListOf<SettingsUiEvent>()

        composeRule.setContent {
            JiTheme {
                SettingsScreen(
                    state = SettingsUiState(budgetInput = "3000"),
                    onEvent = events::add
                )
            }
        }

        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_budget_section").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_vlm_config_section").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_vlm_chat_section").assertIsDisplayed()
        // 权限 section 在长页面下方，断言存在即可（不一定首屏可见）
        composeRule.onNodeWithTag("settings_permission_section").assertExists()
        // 滚动到底再断言 send_chat，避免首屏不可见
        composeRule.onNodeWithTag("settings_vlm_chat_section")
            .assertExists()
            .performScrollTo()
        composeRule.onNodeWithTag("settings_send_chat").assertExists()

        composeRule.onNodeWithTag("settings_budget_input").performTextInput("4500")
        composeRule.onNodeWithTag("settings_save_budget").performClick()

        // 数字输入框会按 IME 行为产生 onValueChange，断言 SaveBudget 派发并验证输入事件至少包含 4500
        assertEquals(SettingsUiEvent.SaveBudget, events.last())
        val inputEvents = events.filterIsInstance<SettingsUiEvent.BudgetInputChanged>()
        assertTrue("expected budget input events, was=${events}", inputEvents.isNotEmpty())
        assertTrue(
            "expected at least one budget input event with '4500' in value, was=${inputEvents.map { it.value }}",
            inputEvents.any { it.value.contains("4500") }
        )
    }

    @Test
    fun disabledChatSendDoesNotDispatchSendEvent() {
        val events = mutableListOf<SettingsUiEvent>()

        composeRule.setContent {
            JiTheme {
                SettingsScreen(
                    state = SettingsUiState(chatInput = "", isChatLoading = false),
                    onEvent = events::add
                )
            }
        }

        composeRule.onNodeWithTag("settings_send_chat").performClick()

        assertEquals(emptyList<SettingsUiEvent>(), events)
    }
}
