package com.digitalvault.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.digitalvault.DigitalVaultApp
import com.digitalvault.ui.main.MainScreen

@Composable
fun AppRoot(vm: AuthViewModel, activity: FragmentActivity) {
    when (vm.stage) {
        AuthViewModel.Stage.Splash -> SplashScreen(onDone = { vm.onSplashDone() })
        AuthViewModel.Stage.Setup -> SetupScreen(vm)
        AuthViewModel.Stage.Login -> LoginScreen(vm, activity)
        AuthViewModel.Stage.NewPin -> NewPinScreen(vm)
        AuthViewModel.Stage.FingerprintOffer -> FingerprintOfferScreen(vm, activity)
        AuthViewModel.Stage.Unlocked -> {
            val repo = remember { (activity.application as DigitalVaultApp).repository }
            MainScreen(repo = repo, vm = vm, activity = activity)
        }
    }
}
