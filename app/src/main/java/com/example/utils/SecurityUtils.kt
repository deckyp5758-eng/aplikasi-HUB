package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted Storage Helper using EncryptedSharedPreferences (Android KeyStore).
 * Safely stores sensitive data like auth tokens, driver IDs, and PINs.
 */
class SecureStorageManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "hub_kediri_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return encryptedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    fun saveAuthToken(token: String) {
        saveString(KEY_AUTH_TOKEN, token)
    }

    fun getAuthToken(): String {
        return getString(KEY_AUTH_TOKEN, "")
    }

    fun saveDriverPin(pin: String) {
        saveString(KEY_DRIVER_PIN, pin)
    }

    fun getDriverPin(): String {
        return getString(KEY_DRIVER_PIN, "")
    }

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "secure_auth_token"
        private const val KEY_DRIVER_PIN = "secure_driver_pin"
    }
}

/**
 * Helper object for Input Sanitization before sending HTTP Requests or storing inputs.
 * Prevents script injection, HTML/XSS, and invalid payload data.
 */
object InputSanitizer {

    /**
     * Cleans general string input by stripping dangerous HTML tags,
     * control characters, and excess whitespace.
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("[\\r\\n\\t]"), " ") // Replace line breaks & tabs with spaces
            .replace(Regex("[\"'\\\\]"), "") // Remove dangerous quotes & backslashes
            .trim()
    }

    /**
     * Ensures input contains only alphanumeric characters (e.g., Driver ID, Unit ID).
     */
    fun sanitizeAlphanumeric(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input.replace(Regex("[^a-zA-Z0-9]"), "").trim()
    }

    /**
     * Ensures numeric string input (e.g., Odometer KM, PIN) only contains digits.
     */
    fun sanitizeNumeric(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input.replace(Regex("[^0-9]"), "").trim()
    }

    /**
     * Computes SHA-256 hash of a string for secure comparison.
     */
    fun sha256(input: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }
}
