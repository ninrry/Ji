package luzzr.ji.core.payment

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedScreenshotStore(private val directory: File) {
    companion object {
        private const val KEY_ALIAS = "ji_recognition_screenshot_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private val MAGIC = byteArrayOf('J'.code.toByte(), 'I'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte())
    }

    fun save(id: String, bytes: ByteArray): String {
        directory.mkdirs()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(bytes)
        val payload = ByteBuffer.allocate(MAGIC.size + 1 + iv.size + encrypted.size)
            .put(MAGIC)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
        return File(directory, "$id.jpg.enc").apply { writeBytes(payload) }.absolutePath
    }

    fun read(path: String): ByteArray? {
        val file = File(path).takeIf(File::exists) ?: return null
        val bytes = file.readBytes()
        if (!bytes.startsWith(MAGIC)) {
            // Compatibility for tasks captured by older builds before encrypted temp screenshots.
            return bytes
        }
        val buffer = ByteBuffer.wrap(bytes)
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        val ivSize = buffer.get().toInt() and 0xFF
        if (ivSize <= 0 || ivSize > 32 || buffer.remaining() <= ivSize) return null
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return runCatching { cipher.doFinal(encrypted) }.getOrNull()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
