package luzzr.ji.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import java.io.ByteArrayOutputStream

object SettingsDebugImageFactory {
    fun createTestBillJpeg(quality: Int = 85): ByteArray {
        val bitmap = createTestBillBitmap()
        return bitmap.toJpegBytes(quality).also { bitmap.recycle() }
    }

    fun createViewScreenshotJpeg(view: View, maxDimension: Int = 720, quality: Int = 75): ByteArray {
        val screenshot = createViewScreenshot(view)
        val scaled = scaleBitmapDown(screenshot, maxDimension)
        return try {
            scaled.toJpegBytes(quality)
        } finally {
            if (scaled != screenshot) scaled.recycle()
            screenshot.recycle()
        }
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray = ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }

    private fun createTestBillBitmap(): Bitmap {
        val bitmap = createBitmap(400, 300)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = "#F4F1EA".toColorInt()
        canvas.drawRect(0f, 0f, 400f, 300f, paint)

        paint.color = "#D5CFC1".toColorInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(10f, 10f, 390f, 290f, paint)

        paint.color = "#433D35".toColorInt()
        paint.style = Paint.Style.FILL
        paint.textSize = 24f
        paint.isAntiAlias = true
        canvas.drawText("Lush 莫奈咖啡馆", 40f, 60f, paint)

        paint.color = "#D5CFC1".toColorInt()
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 85f, 360f, 85f, paint)

        paint.color = "#433D35".toColorInt()
        paint.textSize = 16f
        canvas.drawText("手冲莫奈咖啡     x 2    ￥76.00", 40f, 120f, paint)
        canvas.drawText("法式拿破仑酥     x 1    ￥52.00", 40f, 160f, paint)

        paint.color = "#8E887E".toColorInt()
        paint.textSize = 13f
        canvas.drawText("支付方式: 微信免密支付", 40f, 200f, paint)

        paint.color = "#433D35".toColorInt()
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("￥128.00", 40f, 260f, paint)

        return bitmap
    }

    private fun createViewScreenshot(view: View): Bitmap {
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var newWidth = originalWidth
        var newHeight = originalHeight

        if (originalWidth > originalHeight) {
            if (originalWidth > maxDimension) {
                newWidth = maxDimension
                newHeight = (newWidth * originalHeight) / originalWidth
            }
        } else if (originalHeight > maxDimension) {
            newHeight = maxDimension
            newWidth = (newHeight * originalWidth) / originalHeight
        }

        return if (newWidth == originalWidth && newHeight == originalHeight) {
            bitmap
        } else {
            bitmap.scale(newWidth, newHeight, true)
        }
    }
}
