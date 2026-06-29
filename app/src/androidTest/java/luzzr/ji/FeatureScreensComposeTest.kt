package luzzr.ji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import luzzr.ji.core.design.JiTheme
import luzzr.ji.feature.extrabill.ExtraBillScreen
import luzzr.ji.feature.extrabill.ExtraBillUiState
import luzzr.ji.feature.home.HomeScreen
import luzzr.ji.feature.home.HomeUiState
import luzzr.ji.feature.statistics.StatisticsScreen
import luzzr.ji.feature.statistics.StatisticsUiState
import org.junit.Rule
import org.junit.Test

class HomeScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenExposesStableAutomationAnchors() {
        composeRule.setContent {
            JiTheme {
                HomeScreen(state = HomeUiState(monthlyBudget = 300000L), onEvent = {})
            }
        }
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_button").assertIsDisplayed()
    }
}

class ExtraBillScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun extraBillScreenExposesStableAutomationAnchors() {
        composeRule.setContent {
            JiTheme {
                ExtraBillScreen(state = ExtraBillUiState(), onEvent = {})
            }
        }
        composeRule.onNodeWithTag("extra_bill_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("extra_add_button").assertIsDisplayed()
    }
}

class StatisticsScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statisticsScreenExposesStableAutomationAnchor() {
        composeRule.setContent {
            JiTheme {
                StatisticsScreen(state = StatisticsUiState(), onSelectDimension = {})
            }
        }
        composeRule.onNodeWithTag("statistics_screen").assertIsDisplayed()
    }
}
