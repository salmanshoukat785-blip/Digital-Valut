package com.digitalvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.digitalvault.security.Biometric
import com.digitalvault.security.KeyVault
import com.digitalvault.security.PasswordStrength
import kotlinx.coroutines.delay

private fun fmt(seconds: Long): String =
    if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"

@Composable
private fun ErrorText(text: String?) {
    if (text != null) {
        Text(
            text, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

// ---------------------------------------------------------------- Splash

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoBadge(size = 96)
            Spacer(Modifier.height(16.dp))
            Text(S.appName, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(S.tagline, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

// ---------------------------------------------------------------- Setup

@Composable
fun SetupScreen(vm: AuthViewModel) {
    ScreenColumn {
        Spacer(Modifier.height(8.dp))
        LogoBadge()
        Spacer(Modifier.height(16.dp))
        when (vm.setupStep) {
            0 -> SetupMaster(vm)
            1 -> SetupPin(vm)
            else -> SetupRecovery(vm)
        }
    }
}

@Composable
private fun SetupMaster(vm: AuthViewModel) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val ok = KeyVault.isMasterPasswordAcceptable(pw.toCharArray())
    Text(S.masterTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(S.masterHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    PasswordField(pw, { pw = it }, S.masterLabel)
    if (pw.isNotEmpty()) StrengthBar(PasswordStrength.evaluate(pw))
    Spacer(Modifier.height(10.dp))
    PasswordField(pw2, { pw2 = it }, S.confirmLabel, ImeAction.Done)
    if (pw2.isNotEmpty() && pw != pw2) ErrorText(S.mismatch)
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { vm.setupSetMaster(pw) },
        enabled = ok && pw == pw2,
        modifier = Modifier.fillMaxWidth()
    ) { Text(S.next) }
}

@Composable
private fun SetupPin(vm: AuthViewModel) {
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    val ok = KeyVault.isPinFormatOk(pin.toCharArray()) && pin == pin2
    Text(S.pinTitle, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(S.pinHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    PinField(pin, { pin = it }, S.pinLabel)
    Spacer(Modifier.height(10.dp))
    PinField(pin2, { pin2 = it }, S.pinConfirm)
    if (pin2.isNotEmpty() && pin != pin2) ErrorText(S.mismatch)
    Spacer(Modifier.height(16.dp))
    Button(onClick = { vm.setupSetPin(pin) }, enabled = ok, modifier = Modifier.fillMaxWidth()) { Text(S.next) }
    TextButton(onClick = { vm.setupBack() }) { Text(S.back) }
}

@Composable
private fun SetupRecovery(vm: AuthViewModel) {
    var checked by remember { mutableStateOf(false) }
    Text(S.recoveryTitle, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(S.recoveryHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        vm.recoveryDisplay.split("-").chunked(4).forEach { row ->
            Text(row.joinToString("  "), fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().clickable { checked = !checked },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Text(S.recoveryConfirm, fontSize = 14.sp)
    }
    ErrorText(vm.message)
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { vm.createVault() },
        enabled = checked && !vm.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(S.createVault)
    }
    TextButton(onClick = { vm.setupBack() }, enabled = !vm.busy) { Text(S.back) }
}

// ---------------------------------------------------------------- Login

private enum class LoginMode { Pin, Master, Recovery }

@Composable
fun LoginScreen(vm: AuthViewModel, activity: FragmentActivity) {
    var mode by remember { mutableStateOf(LoginMode.Pin) }
    var forgot by remember { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vm.lockUntil) {
        while (System.currentTimeMillis() < vm.lockUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val remaining = ((vm.lockUntil - now + 999) / 1000).coerceAtLeast(0)
    val locked = remaining > 0
    val status = if (locked) S.locked(fmt(remaining)) else vm.message

    fun launchFingerprint() {
        val cipher = vm.biometricCipher() ?: return
        Biometric.prompt(
            activity, cipher, S.fingerprintUnlockTitle, S.usePin,
            onSuccess = { vm.biometricUnlocked(it) },
            onError = { vm.showMessage(it) }
        )
    }

    LaunchedEffect(Unit) {
        if (vm.biometricEnabled) launchFingerprint()
    }

    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        LogoBadge()
        Spacer(Modifier.height(12.dp))
        Text(S.appName, style = MaterialTheme.typography.headlineSmall)

        when (mode) {
            LoginMode.Pin -> {
                var pin by remember { mutableStateOf("") }
                fun submit() {
                    val p = pin
                    pin = ""
                    vm.submitPin(p)
                }
                Text(S.enterPin, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                PinDots(pin.length)
                ErrorText(status)
                Spacer(Modifier.height(6.dp))
                Keypad(
                    onDigit = { d ->
                        if (!locked && !vm.busy && pin.length < 6) {
                            pin += d
                            if (pin.length == 6) submit()
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) },
                    showFingerprint = vm.biometricEnabled,
                    onFingerprint = { launchFingerprint() }
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submit() },
                    enabled = pin.length >= 4 && !locked && !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(S.unlock)
                }
                TextButton(onClick = { mode = LoginMode.Master; forgot = false; vm.clearMessage() }) {
                    Text(S.useMaster)
                }
                TextButton(onClick = { mode = LoginMode.Master; forgot = true; vm.clearMessage() }) {
                    Text(S.forgotPin)
                }
            }

            LoginMode.Master -> {
                var pw by remember { mutableStateOf("") }
                Spacer(Modifier.height(8.dp))
                Text(S.enterMaster, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                PasswordField(pw, { pw = it }, S.masterLabel, ImeAction.Done)
                ErrorText(status)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { val p = pw; pw = ""; vm.submitMaster(p, forgot) },
                    enabled = pw.isNotEmpty() && !locked && !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(S.unlock)
                }
                TextButton(onClick = { mode = LoginMode.Pin; forgot = false; vm.clearMessage() }) {
                    Text(S.usePin)
                }
                TextButton(onClick = { mode = LoginMode.Recovery; vm.clearMessage() }) {
                    Text(S.forgotMaster)
                }
            }

            LoginMode.Recovery -> {
                var code by remember { mutableStateOf("") }
                var newPw by remember { mutableStateOf("") }
                var newPw2 by remember { mutableStateOf("") }
                Spacer(Modifier.height(8.dp))
                Text(S.recoveryEnterTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(S.recoveryCodeLabel) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(10.dp))
                PasswordField(newPw, { newPw = it }, S.newMasterLabel)
                if (newPw.isNotEmpty()) StrengthBar(PasswordStrength.evaluate(newPw))
                Spacer(Modifier.height(10.dp))
                PasswordField(newPw2, { newPw2 = it }, S.confirmLabel, ImeAction.Done)
                if (newPw2.isNotEmpty() && newPw != newPw2) ErrorText(S.mismatch)
                ErrorText(status)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { val c = code; val p = newPw; newPw = ""; newPw2 = ""; vm.submitRecovery(c, p) },
                    enabled = code.isNotBlank() && newPw.isNotEmpty() && newPw == newPw2 && !locked && !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(S.resetUnlock)
                }
                TextButton(onClick = { mode = LoginMode.Master; vm.clearMessage() }) { Text(S.back) }
            }
        }
    }
}

// ---------------------------------------------------------------- Naya PIN / Fingerprint offer

@Composable
fun NewPinScreen(vm: AuthViewModel) {
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    val ok = KeyVault.isPinFormatOk(pin.toCharArray()) && pin == pin2
    ScreenColumn {
        Spacer(Modifier.height(32.dp))
        LogoBadge()
        Spacer(Modifier.height(16.dp))
        Text(S.newPinTitle, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(S.pinHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        PinField(pin, { pin = it }, S.pinLabel)
        Spacer(Modifier.height(10.dp))
        PinField(pin2, { pin2 = it }, S.pinConfirm)
        if (pin2.isNotEmpty() && pin != pin2) ErrorText(S.mismatch)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.setNewPin(pin) },
            enabled = ok && !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(S.savePin) }
    }
}

@Composable
fun FingerprintOfferScreen(vm: AuthViewModel, activity: FragmentActivity) {
    ScreenColumn {
        Spacer(Modifier.height(48.dp))
        LogoBadge()
        Spacer(Modifier.height(16.dp))
        Text(S.fingerprintOfferTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            S.fingerprintOfferBody, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp, textAlign = TextAlign.Center
        )
        ErrorText(vm.message)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val cipher = vm.enrollCipher()
                if (cipher == null) {
                    vm.skipFingerprint()
                } else {
                    Biometric.prompt(
                        activity, cipher, S.fingerprintEnrollTitle, S.cancel,
                        onSuccess = { vm.biometricEnrolled(it) },
                        onError = { vm.showMessage(it) }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(S.enable) }
        OutlinedButton(onClick = { vm.skipFingerprint() }, modifier = Modifier.fillMaxWidth()) { Text(S.notNow) }
    }
}
