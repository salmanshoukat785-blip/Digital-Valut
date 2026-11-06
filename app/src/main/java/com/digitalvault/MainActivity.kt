package com.digitalvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.digitalvault.ui.AppRoot
import com.digitalvault.ui.AuthViewModel
import com.digitalvault.ui.theme.VaultTheme

// BiometricPrompt ke liye FragmentActivity zaroori hai
class MainActivity : FragmentActivity() {

    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screenshot, screen recording aur Recent Apps preview block
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            VaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.safeDrawingPadding()) {
                        AppRoot(vm, this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Screen rotate par lock nahi, sirf asli background par
        if (!isChangingConfigurations) vm.onAppBackgrounded()
    }
}
