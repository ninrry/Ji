package luzzr.ji.core.common

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SecureStorage {
    fun getApiKey(): String
    fun saveApiKey(key: String)
}

class SecureStorageImpl(context: Context) : SecureStorage {
    private val sharedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "ji_secure_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getApiKey(): String {
        return sharedPrefs.getString("opencode_api_key", "") ?: ""
    }

    override fun saveApiKey(key: String) {
        sharedPrefs.edit()
            .putString("opencode_api_key", key)
            .apply()
    }
}
