package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.utils.SecureStorageManager

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fleet_prefs", Context.MODE_PRIVATE)
    private val secureStorage: SecureStorageManager? by lazy {
        try {
            SecureStorageManager(context)
        } catch (e: Exception) {
            null
        }
    }

    var isGoogleSheetsMode: Boolean
        get() = prefs.getBoolean("is_google_sheets_mode", true)
        set(value) = prefs.edit().putBoolean("is_google_sheets_mode", value).apply()

    var appsScriptUrl: String
        get() = prefs.getString("apps_script_url", "https://script.google.com/macros/s/AKfycbxt5PDpBtv9vwtzj6Md273D61IU1Z2eYvdcGPGWZ6lG2KJxArcZIhz9Q3H-3l007VI/exec") ?: "https://script.google.com/macros/s/AKfycbxt5PDpBtv9vwtzj6Md273D61IU1Z2eYvdcGPGWZ6lG2KJxArcZIhz9Q3H-3l007VI/exec"
        set(value) = prefs.edit().putString("apps_script_url", value).apply()

    var googleSheetId: String
        get() = prefs.getString("google_sheet_id", "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw") ?: "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"
        set(value) = prefs.edit().putString("google_sheet_id", value).apply()

    var loggedInDriverName: String
        get() = prefs.getString("logged_in_driver_name", "") ?: ""
        set(value) = prefs.edit().putString("logged_in_driver_name", value).apply()

    var loggedInDriverId: String
        get() = prefs.getString("logged_in_driver_id", "") ?: ""
        set(value) = prefs.edit().putString("logged_in_driver_id", value).apply()

    var geminiApiKey: String
        get() {
            val secureKey = try { secureStorage?.getString("encrypted_gemini_key", "") ?: "" } catch (e: Exception) { "" }
            if (secureKey.isNotEmpty()) return secureKey
            return prefs.getString("gemini_api_key", "") ?: ""
        }
        set(value) {
            try {
                secureStorage?.saveString("encrypted_gemini_key", value)
            } catch (e: Exception) {
                // Fallback to standard prefs if KeyStore fails on some custom ROMs
                prefs.edit().putString("gemini_api_key", value).apply()
            }
        }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    fun clearLogin() {
        prefs.edit()
            .remove("logged_in_driver_name")
            .remove("logged_in_driver_id")
            .apply()
    }
}
