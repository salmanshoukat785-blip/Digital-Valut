package com.digitalvault.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** App ki saaf-suthri settings (abhi sirf theme; baqi Part 7 mein). Is mein koi secret nahi. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("vault_settings", Context.MODE_PRIVATE)

    var themeMode by mutableIntStateOf(prefs.getInt(KEY_THEME, THEME_DARK))
        private set

    fun updateTheme(mode: Int) {
        themeMode = mode
        prefs.edit().putInt(KEY_THEME, mode).apply()
    }

    companion object {
        const val THEME_DARK = 0
        const val THEME_LIGHT = 1
        const val THEME_SYSTEM = 2
        private const val KEY_THEME = "theme"
    }
}
