package com.digitalvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.digitalvault.settings.AppSettings
import com.digitalvault.ui.AppRoot
import com.digitalvault.ui.AuthViewModel
import com.digitalvault.ui.S
import com.digitalvault.ui.theme.VaultTheme

// BiometricPrompt ke liye FragmentActivity zaroori hai
class MainActivity : FragmentActivity() {

    private val vm: AuthViewModel by viewModels()
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = (application as DigitalVaultApp).settings
        applyScreenSecurity(settings.blockScreenshots)
        setContent {
            val block = settings.blockScreenshots
            SideEffect { applyScreenSecurity(block) }
            val dark = when (settings.themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_SYSTEM -> isSystemInDarkTheme()
                else -> true
            }
            val direction = if (S.lang == 1) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                VaultTheme(dark = dark) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.safeDrawingPadding()) {
                            AppRoot(vm, this@MainActivity)
                        }
                    }
                }
            }
        }
    }

    /** Screenshot, screen recording aur Recent Apps preview block (setting se on/off). */
    private fun applyScreenSecurity(block: Boolean) {
        if (block) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStart() {
        super.onStart()
        vm.onAppForegrounded(settings.autoLockSeconds * 1000L)
    }

    override fun onStop() {
        super.onStop()
        // Screen rotate par lock nahi, sirf asli background par
        if (!isChangingConfigurations) vm.onAppBackgrounded(settings.autoLockSeconds * 1000L)
    }
}
