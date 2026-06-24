package luzzr.ji.core.shizuku

import android.os.ParcelFileDescriptor
import android.os.RemoteException
import java.io.File
import java.util.concurrent.TimeUnit

/** Runs as the user-authorized Shizuku shell/root identity, never in the app process. */
class ShizukuScreenshotUserService : IShizukuScreenshotService.Stub() {
    override fun capturePng(): ParcelFileDescriptor {
        val output = File("/data/local/tmp/ji-screen-${System.nanoTime()}.png")
        var process: Process? = null
        try {
            process = ProcessBuilder("/system/bin/screencap", "-p", output.absolutePath)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(4, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw RemoteException("Shizuku 截图超时")
            }
            if (process.exitValue() != 0 || !output.isFile || output.length() == 0L) {
                throw RemoteException("Shizuku 截图命令执行失败")
            }
            // Binder duplicates this descriptor for the app; deleting the path then leaves no file behind.
            return ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (error: Exception) {
            if (error is RemoteException) throw error
            throw RemoteException("Shizuku 截图失败: ${error.message}").apply { initCause(error) }
        } finally {
            process?.inputStream?.close()
            process?.errorStream?.close()
            output.delete()
        }
    }
}
