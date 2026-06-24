package luzzr.ji

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import luzzr.ji.core.ui.FloatingNavigationBar
import luzzr.ji.core.ui.ScreenTab
import luzzr.ji.feature.extrabill.ExtraBillRoute
import luzzr.ji.feature.extrabill.ExtraBillViewModel
import luzzr.ji.feature.home.HomeRoute
import luzzr.ji.feature.home.HomeViewModel
import luzzr.ji.feature.settings.SettingsRoute
import luzzr.ji.feature.settings.SettingsViewModel
import luzzr.ji.feature.statistics.StatisticsRoute
import luzzr.ji.feature.statistics.StatisticsViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider { entry<Main> { MainScreenFrame() }
        }
    )
}

@Composable
fun MainScreenFrame() {
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    val context = LocalContext.current
    val app = context.applicationContext as JiApplication
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            app.container.observeTransactionsUseCase,
            app.container.createTransactionUseCase,
            app.container.deleteTransactionUseCase,
            app.container.migrateTransactionUseCase,
            app.container.observeBudgetUseCase,
            app.container.paymentRecognitionManager
        )
    }
    val statisticsViewModel: StatisticsViewModel = viewModel { StatisticsViewModel(app.container.observeTransactionsUseCase) }
    val extraBillViewModel: ExtraBillViewModel = viewModel {
        ExtraBillViewModel(
            app.container.observeTransactionsUseCase,
            app.container.createTransactionUseCase,
            app.container.deleteTransactionUseCase,
            app.container.migrateTransactionUseCase,
            app.container.getExtraBillOverviewUseCase
        )
    }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            app.container.observeBudgetUseCase,
            app.container.saveBudgetUseCase,
            app.container.secureStorage,
            app.getSharedPreferences("app_config", android.content.Context.MODE_PRIVATE)
        )
    }
    val homeState by homeViewModel.uiState.collectAsState()
    val extraState by extraBillViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.uiEffect.collect { effect ->
            when (effect) {
                is luzzr.ji.feature.home.HomeUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Crossfade(currentTab, Modifier.fillMaxSize(), label = "tab_crossfade") { tab ->
            when (tab) {
                ScreenTab.HOME -> HomeRoute(homeViewModel, Modifier.fillMaxSize())
                ScreenTab.STATISTICS -> StatisticsRoute(statisticsViewModel, Modifier.fillMaxSize())
                ScreenTab.EXTRA -> ExtraBillRoute(extraBillViewModel, Modifier.fillMaxSize())
                ScreenTab.SETTINGS -> SettingsRoute(settingsViewModel, Modifier.fillMaxSize())
            }
        }
        AnimatedVisibility(
            visible = !homeState.showAddDialog && !extraState.showAddDialog,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingNavigationBar(currentTab = currentTab, onTabSelected = { currentTab = it })
        }
    }
}
