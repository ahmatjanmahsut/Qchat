package com.qchat.app.data.remote.config

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.qchat.app.data.remote.config.ServerConfigManager.Companion.DEFAULT_BASE_URL

class ServerConfigManager(private val context: Context) {

    private val preferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "server_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getBaseUrl(): String {
        return preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        preferences.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getProtocol(): String {
        val url = getBaseUrl()
        return if (url.startsWith("https://")) "https" else "http"
    }

    fun getHost(): String {
        val url = getBaseUrl()
        val protocol = getProtocol()
        val startIndex = protocol.length + 3 // "http://" 或 "https://"
        val endIndex = url.indexOf(':', startIndex)
        return if (endIndex == -1) url.substring(startIndex) else url.substring(startIndex, endIndex)
    }

    fun getPort(): Int {
        val url = getBaseUrl()
        val protocol = getProtocol()
        val startIndex = protocol.length + 3
        val colonIndex = url.indexOf(':', startIndex)
        return if (colonIndex == -1) {
            if (protocol == "https") 443 else 80
        } else {
            url.substring(colonIndex + 1).takeWhile { it != '/' }.toIntOrNull() ?: 
                if (protocol == "https") 443 else 80
        }
    }

    fun resetToDefault() {
        setBaseUrl(DEFAULT_BASE_URL)
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"
    }
}
