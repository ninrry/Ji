package luzzr.ji.core.payment

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import luzzr.ji.MainActivity
import luzzr.ji.core.vlm.VlmErrorCategory
import luzzr.ji.core.vlm.VlmTransactionResult
import java.util.concurrent.atomic.AtomicInteger

class AutoRecordNotifier(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "auto_record"
        private const val CHANNEL_NAME = "自动记账"
        private const val CHANNEL_API_ERROR = "vlm_api_error"
        private const val CHANNEL_API_ERROR_NAME = "API 异常"
        private val nextNotificationId = AtomicInteger(3000)
    }

    fun showRecorded(result: VlmTransactionResult) {
        show(
            title = "已自动记账",
            text = "¥${"%.2f".format(result.amount / 100.0)} · ${result.category} · ${result.note}",
            icon = android.R.drawable.ic_menu_save,
            channelId = CHANNEL_ID
        )
    }

    fun showFailed(message: String) {
        show(
            title = "自动记账失败",
            text = message.take(96),
            icon = android.R.drawable.ic_dialog_alert,
            channelId = CHANNEL_ID
        )
    }

    /** 402 — 余额不足，高优先级通知引导用户充值 */
    fun showBalanceInsufficient() {
        show(
            title = "API 余额不足",
            text = "云端识别已暂停，请前往设置充值后继续使用",
            icon = android.R.drawable.ic_dialog_alert,
            channelId = CHANNEL_API_ERROR,
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    /** 非402的永久性API错误（401/403/404等），提示用户检查配置 */
    fun showApiError(category: VlmErrorCategory, message: String) {
        val (title, hint) = when (category) {
            VlmErrorCategory.AUTH_FAILED -> "API 密钥无效" to "请在设置中重新配置密钥"
            VlmErrorCategory.ACCESS_DENIED -> "API 访问被拒" to "可能是地区限制或密钥被风控"
            VlmErrorCategory.NOT_FOUND -> "API 接口错误" to "请检查模型名称是否正确"
            VlmErrorCategory.BAD_REQUEST -> "API 请求格式错误" to "请更新应用或联系开发者"
            VlmErrorCategory.CONTENT_BLOCKED -> "内容审核拦截" to "截图内容触发安全审核"
            else -> "API 异常" to message.take(64)
        }
        show(
            title = title,
            text = hint,
            icon = android.R.drawable.ic_dialog_alert,
            channelId = CHANNEL_API_ERROR,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun show(
        title: String,
        text: String,
        icon: Int,
        channelId: String = CHANNEL_ID,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, if (channelId == CHANNEL_API_ERROR) CHANNEL_API_ERROR_NAME else CHANNEL_NAME, importance)
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(nextNotificationId.getAndIncrement(), notification)
    }
}
