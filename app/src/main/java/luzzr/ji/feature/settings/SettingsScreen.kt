package luzzr.ji.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import luzzr.ji.core.design.JiTheme
import androidx.compose.material3.Text

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is SettingsUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "系统设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JiTheme.colors.textPrimary,
                modifier = Modifier.testTag("settings_title")
            )
            Spacer(modifier = Modifier.height(24.dp))

            BudgetSettingsSection(state = state, onEvent = onEvent)
            Spacer(modifier = Modifier.height(24.dp))

            VlmConfigSection(state = state, onEvent = onEvent)
            Spacer(modifier = Modifier.height(24.dp))

            VlmChatDebugSection(state = state, onEvent = onEvent)
            Spacer(modifier = Modifier.height(24.dp))

            PermissionSettingsSection(state = state, onEvent = onEvent)
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}
