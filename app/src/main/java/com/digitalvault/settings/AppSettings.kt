package com.digitalvault.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App ki saaf-suthri settings. Is mein koi secret nahi. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("vault_settings", Context.MODE_PRIVATE)

    var themeMode by mutableIntStateOf(prefs.getInt(KEY_THEME, THEME_DARK))
        private set
    /** 0 = English, 1 = Urdu */
    var language by mutableIntStateOf(prefs.getInt(KEY_LANG, 0))
        private set
    /** App chhorne ke kitne second baad lock (0 = foran) */
    var autoLockSeconds by mutableIntStateOf(prefs.getInt(KEY_AUTOLOCK, 0))
        private set
    var blockScreenshots by mutableStateOf(prefs.getBoolean(KEY_BLOCK, true))
        private set

    fun updateTheme(mode: Int) { themeMode = mode; prefs.edit().putInt(KEY_THEME, mode).apply() }
    fun updateLanguage(l: Int) { language = l; prefs.edit().putInt(KEY_LANG, l).apply() }
    fun updateAutoLock(seconds: Int) { autoLockSeconds = seconds; prefs.edit().putInt(KEY_AUTOLOCK, seconds).apply() }
    fun updateBlockScreenshots(b: Boolean) { blockScreenshots = b; prefs.edit().putBoolean(KEY_BLOCK, b).apply() }

    companion object {
        const val THEME_DARK = 0
        const val THEME_LIGHT = 1
        const val THEME_SYSTEM = 2
        private const val KEY_THEME = "theme"
        private const val KEY_LANG = "lang"
        private const val KEY_AUTOLOCK = "autolock"
        private const val KEY_BLOCK = "block_screens"
    }
}
