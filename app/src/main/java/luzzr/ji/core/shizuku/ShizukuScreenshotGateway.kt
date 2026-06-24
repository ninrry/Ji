package luzzr.ji.core.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream

/** Preferred, user-authorized screenshot transport for payment completion pages. */
object ShizukuScreenshotGateway {
    private const val REQUEST_CODE = 4107
    private const val USER_SERVICE_VERSION = 1
    private const val SERVICE_CONNECT_TIMEOUT_MS = 5_000L
    private const val MAX_CAPTURE_BYTES = 12 * 1024 * 1024

    enum class Status {
        UNAVAILABLE,
        PERMISSION_REQUIRED,
        DENIED,
        AUTHORIZED
    }

    private val bindMutex = Mutex()

    @Volatile
    private var remoteService: IShizukuScreenshotService? = null

    fun status(): Status = runCatching {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return Status.UNAVAILABLE
        when {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED -> Status.AUTHORIZED
            Shizuku.shouldShowRequestPermissionRationale() -> Status.DENIED
            else -> Status.PERMISSION_REQUIRED
        }
    }.getOrDefault(Status.UNAVAILABLE)

    /** Requests the Shizuku/Sui grant. Returns false when there is no running provider to request from. */
    fun requestPermission(): Boolean = runCatching {
        when (status()) {
            Status.AUTHORIZED -> true
            Status.PERMISSION_REQUIRED -> {
                Shizuku.requestPermission(REQUEST_CODE)
                true
            }
            Status.UNAVAILABLE,
            Status.DENIED -> false
        }
    }.getOrDefault(false)

    suspend fun capturePng(context: Context): ByteArray? = withContext(Dispatchers.IO) {
        if (status() != Status.AUTHORIZED) return@withContext null
        val service = getService(context.applicationContext) ?: return@withContext null
        runCatching {
            service.capturePng()?.let(::readPng)
        }.getOrElse {
            remoteService = null
            null
        }
    }

    private suspend fun getService(context: Context): IShizukuScreenshotService? = bindMutex.withLock {
        remoteService?.let { return@withLock it }
        val binding = kotlinx.coroutines.CompletableDeferred<IShizukuScreenshotService?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val remote = IShizukuScreenshotService.Stub.asInterface(service)
                remoteService = remote
                binding.complete(remote)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                remoteService = null
                binding.complete(null)
            }
        }
        runCatching {
            Shizuku.bindUserService(userServiceArgs(context), connection)
        }.onFailure {
            binding.complete(null)
        }
        withTimeoutOrNull(SERVICE_CONNECT_TIMEOUT_MS) { binding.await() }
    }

    private fun userServiceArgs(context: Context) = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShizukuScreenshotUserService::class.java.name)
    )
        .tag("ji-payment-screenshot")
        .version(USER_SERVICE_VERSION)
        .daemon(false)

    private fun readPng(descriptor: ParcelFileDescriptor): ByteArray {
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (output.size() + read > MAX_CAPTURE_BYTES) {
                    throw IllegalStateException("Shizuku 截图文件过大")
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}
