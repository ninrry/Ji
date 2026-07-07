package luzzr.ji.core.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import luzzr.ji.core.vlm.VlmProvider

interface SecureStorage {
    /** Legacy single-key access — returns the key for the current provider. */
    fun getApiKey(): String
    fun saveApiKey(key: String)

    /** Provider-isolated key access. */
    fun getApiKey(provider: VlmProvider): String
    fun saveApiKey(provider: VlmProvider, key: String)
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
        sharedPrefs.edit {
            putString("opencode_api_key", key)
        }
    }

    override fun getApiKey(provider: VlmProvider): String {
        return sharedPrefs.getString(provider.apiKeyPrefKey, "") ?: ""
    }

    override fun saveApiKey(provider: VlmProvider, key: String) {
        sharedPrefs.edit {
            putString(provider.apiKeyPrefKey, key)
        }
    }
}
