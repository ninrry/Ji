package luzzr.ji.core.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import luzzr.ji.core.shizuku.ShizukuScreenshotGateway

object PermissionManager {
    private const val TAG = "PermissionManager"

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AutoBillAccessibilityService::class.java).flattenToString()
        val fullExpected = ComponentName(
            context.packageName,
            AutoBillAccessibilityService::class.java.name
        ).flattenToString()

        // 1) Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES (Android 7.0+)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (!enabled.isNullOrEmpty()) {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                val item = splitter.next()
                if (item.equals(expected, ignoreCase = true) ||
                    item.equals(fullExpected, ignoreCase = true)
                ) return true
            }
        }

        // 2) AccessibilityManager.getEnabledAccessibilityServiceList fallback
        // Some OEM ROMs (Huawei EMUI, Xiaomi MIUI) only return true here.
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am != null) {
            val infos: List<AccessibilityServiceInfo> = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            for (info in infos) {
                val name = info.resolveInfo.serviceInfo
                val component = ComponentName(name.packageName, name.name).flattenToString()
                if (component.equals(expected, ignoreCase = true) ||
                    component.equals(fullExpected, ignoreCase = true)
                ) return true
            }
        }

        Log.w(TAG, "Accessibility not recognised: expected=$expected / $fullExpected")
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun shizukuStatus(): ShizukuScreenshotGateway.Status = ShizukuScreenshotGateway.status()

    fun requestShizukuScreenshotPermission(): Boolean = ShizukuScreenshotGateway.requestPermission()

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
