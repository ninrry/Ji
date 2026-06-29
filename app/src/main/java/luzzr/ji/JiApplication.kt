package luzzr.ji

import android.app.Application
import androidx.core.content.edit

class JiApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // The test-era implementation stored this key in plain SharedPreferences.
        getSharedPreferences("app_config", MODE_PRIVATE).edit { remove("opencode_api_key") }
        container = AppContainerImpl(this)
    }
}
