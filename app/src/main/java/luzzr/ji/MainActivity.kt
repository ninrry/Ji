package luzzr.ji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import luzzr.ji.core.design.JiTheme
import luzzr.ji.core.permissions.AutoRecordAccessibilityWatchdog

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      JiTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(JiTheme.colors.background)
        ) {
          MainNavigation()
        }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
    }
  }

  override fun onResume() {
    super.onResume()
    // The user may have enabled the accessibility service in system settings and returned here.
    AutoRecordAccessibilityWatchdog.keepAliveIfEnabled(this)
  }
}
