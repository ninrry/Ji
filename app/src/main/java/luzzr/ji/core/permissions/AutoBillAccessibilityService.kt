package luzzr.ji.core.permissions

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import luzzr.ji.JiApplication
import luzzr.ji.core.payment.PaymentCandidate
import luzzr.ji.core.payment.PaymentCaptureIdentity
import luzzr.ji.core.payment.PaymentCompletionClassifier
import luzzr.ji.core.payment.PaymentFingerprint
import luzzr.ji.core.shizuku.ShizukuScreenshotGateway
import java.io.ByteArrayOutputStream

/** Captures only completed payment pages from the supported wallet applications. */
class AutoBillAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "AutoBill"
        private const val MAX_DEDUP_ENTRIES = 64
        private const val MAX_NODE_COUNT = 300
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(
        serviceJob + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "Automatic payment recognition failed", error)
        }
    )
    private data class RecentFingerprint(val capturedAt: Long, val dedupWindowMs: Long)

    private val recentFingerprints = LinkedHashMap<String, RecentFingerprint>(MAX_DEDUP_ENTRIES, 0.75f, true)

    override fun onServiceConnected() {
        super.onServiceConnected()
        // The framework rebinds an enabled accessibility service after boot or process death.
        // Keep the companion foreground service visible and restartable at the same time.
        AutoRecordKeepAliveService.startIfAccessibilityEnabled(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in setOf(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            )
        ) return

        val packageName = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return
        try {
            val texts = ArrayList<String>()
            collectNodeTexts(root, texts, intArrayOf(0))
            val fullText = PaymentFingerprint.normalizedText(texts.joinToString("\n"))
            val signal = PaymentCompletionClassifier.from(this, packageName, fullText) ?: return
            val identity = PaymentFingerprint.captureIdentity(signal.platform, signal.kind, fullText)
            if (!shouldProcess(identity)) return
            captureAndQueue(signal.platform, signal.kind, fullText, identity)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun shouldProcess(identity: PaymentCaptureIdentity): Boolean {
        val now = System.currentTimeMillis()
        synchronized(recentFingerprints) {
            recentFingerprints.entries.removeAll { (_, recent) ->
                now - recent.capturedAt > recent.dedupWindowMs
            }
            if (recentFingerprints.containsKey(identity.fingerprint)) return false
            if (recentFingerprints.size >= MAX_DEDUP_ENTRIES) {
                recentFingerprints.entries.firstOrNull()?.let { recentFingerprints.remove(it.key) }
            }
            recentFingerprints[identity.fingerprint] = RecentFingerprint(now, identity.dedupWindowMs)
            return true
        }
    }

    private fun captureAndQueue(
        platform: luzzr.ji.domain.model.PaymentPlatform,
        kind: luzzr.ji.domain.model.PaymentKind,
        screenText: String,
        identity: PaymentCaptureIdentity
    ) {
        val capturedAt = System.currentTimeMillis()
        serviceScope.launch {
            // Shizuku/Sui capture is silent and includes content that ordinary accessibility
            // screenshots can miss. The same 720px/JPEG budget is retained for VLM cost control.
            val shizukuBitmap = ShizukuScreenshotGateway.capturePng(applicationContext)
                ?.let(::decodeSampledBitmap)
            if (shizukuBitmap != null) {
                try {
                    queue(platform, kind, screenText, compress(shizukuBitmap), capturedAt, identity)
                } finally {
                    shizukuBitmap.recycle()
                }
                return@launch
            }
            captureAccessibilityAndQueue(platform, kind, screenText, capturedAt, identity)
        }
    }

    private fun captureAccessibilityAndQueue(
        platform: luzzr.ji.domain.model.PaymentPlatform,
        kind: luzzr.ji.domain.model.PaymentKind,
        screenText: String,
        capturedAt: Long,
        identity: PaymentCaptureIdentity
    ) {
        try {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                applicationContext.mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val buffer = screenshot.hardwareBuffer
                        val bitmap = try {
                            Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        } catch (error: Exception) {
                            Log.w(TAG, "Cannot wrap payment screenshot", error)
                            null
                        }
                        if (bitmap == null) {
                            buffer.close()
                            queue(platform, kind, screenText, null, capturedAt, identity)
                            return
                        }
                        serviceScope.launch {
                            try {
                                queue(platform, kind, screenText, compress(bitmap), capturedAt, identity)
                            } finally {
                                bitmap.recycle()
                                buffer.close()
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.w(TAG, "Payment screenshot failed: $errorCode")
                        queue(platform, kind, screenText, null, capturedAt, identity)
                    }
                }
            )
        } catch (error: Exception) {
            Log.w(TAG, "Payment screenshot request failed", error)
            queue(platform, kind, screenText, null, capturedAt, identity)
        }
    }

    private fun queue(
        platform: luzzr.ji.domain.model.PaymentPlatform,
        kind: luzzr.ji.domain.model.PaymentKind,
        screenText: String,
        imageBytes: ByteArray?,
        capturedAt: Long,
        identity: PaymentCaptureIdentity
    ) {
        serviceScope.launch {
            val app = applicationContext as JiApplication
            app.container.paymentRecognitionManager.enqueue(
                PaymentCandidate(
                    platform = platform,
                    kindHint = kind,
                    screenText = screenText,
                    screenshotBytes = imageBytes,
                    capturedAt = capturedAt,
                    eventFingerprint = identity.fingerprint,
                    dedupWindowMs = identity.dedupWindowMs
                )
            )
        }
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        val scaled = scaleBitmapDown(bitmap, 720)
        return try {
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, output)
                output.toByteArray()
            }
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        return bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(width, height, 720)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrElse { error ->
        Log.w(TAG, "Cannot decode Shizuku payment screenshot safely", error)
        null
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val longest = maxOf(width, height)
        while (longest / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, target: MutableList<String>, count: IntArray) {
        if (node == null || count[0] >= MAX_NODE_COUNT) return
        count[0]++
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(target::add)
        for (index in 0 until node.childCount) {
            if (count[0] >= MAX_NODE_COUNT) return
            val child = node.getChild(index) ?: continue
            try {
                collectNodeTexts(child, target, count)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
