package com.digitalvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun AppRoot(vm: AuthViewModel, activity: FragmentActivity) {
    when (vm.stage) {
        AuthViewModel.Stage.Splash -> SplashScreen(onDone = { vm.onSplashDone() })
        AuthViewModel.Stage.Setup -> SetupScreen(vm)
        AuthViewModel.Stage.Login -> LoginScreen(vm, activity)
        AuthViewModel.Stage.NewPin -> NewPinScreen(vm)
        AuthViewModel.Stage.FingerprintOffer -> FingerprintOfferScreen(vm, activity)
        AuthViewModel.Stage.Unlocked -> UnlockedPlaceholder(vm)
    }
}

/** Part 4 mein Dashboard, All Passwords wagaira isi ki jagah aayenge. */
@Composable
private fun UnlockedPlaceholder(vm: AuthViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoBadge()
        Spacer(Modifier.height(16.dp))
        Text(S.unlocked, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(S.unlockedBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.lockNow() }) { Text(S.lockNow) }
    }
}
