package com.digitalvault.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.DigitalVaultApp
import com.digitalvault.security.AttemptLimiter
import com.digitalvault.security.Biometric
import com.digitalvault.security.Crypto
import com.digitalvault.security.RecoveryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    enum class Stage { Splash, Setup, Login, NewPin, FingerprintOffer, Unlocked }

    private val appCtx = app as DigitalVaultApp
    private val vault = appCtx.vault
    private val repo = appCtx.repository
    private val limiter = AttemptLimiter(app)

    var stage by mutableStateOf(Stage.Splash); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var lockUntil by mutableLongStateOf(0L); private set
    var attemptsLeft by mutableIntStateOf(limiter.attemptsLeft()); private set
    var biometricEnabled by mutableStateOf(vault.isBiometricEnabled); private set

    // Setup ki halat (sirf memory mein, kahin save nahi hoti)
    var setupStep by mutableIntStateOf(0); private set
    var recoveryDisplay by mutableStateOf(""); private set
    private var setupMaster: CharArray? = null
    private var setupPin: CharArray? = null
    private var recoveryBytes: ByteArray? = null

    init { refreshLock() }

    fun showMessage(m: String) { message = m }
    fun clearMessage() { message = null }

    fun onSplashDone() {
        stage = when {
            vault.isUnlocked -> Stage.Unlocked
            vault.isSetUp -> Stage.Login
            else -> Stage.Setup
        }
    }

    // ---------------- Setup ----------------

    fun setupSetMaster(pw: String) {
        setupMaster = pw.toCharArray()
        setupStep = 1
    }

    fun setupSetPin(pin: String) {
        setupPin = pin.toCharArray()
        val bytes = Crypto.randomBytes(32)
        recoveryBytes = bytes
        recoveryDisplay = RecoveryCode.encode(bytes)
        setupStep = 2
    }

    fun setupBack() {
        if (setupStep > 0) setupStep -= 1
        message = null
    }

    fun createVault() {
        val master = setupMaster ?: return
        val pin = setupPin ?: return
        val rec = recoveryBytes ?: return
        if (busy) return
        viewModelScope.launch {
            busy = true
            val ok = withContext(Dispatchers.Default) {
                runCatching { vault.setup(master, pin, rec) }.isSuccess
            }
            busy = false
            if (ok) {
                Crypto.wipe(master); Crypto.wipe(pin); Crypto.wipe(rec)
                setupMaster = null; setupPin = null; recoveryBytes = null
                recoveryDisplay = ""
                setupStep = 0
                afterUnlock(if (Biometric.isAvailable(getApplication())) Stage.FingerprintOffer else Stage.Unlocked)
            } else {
                message = S.setupFailed
            }
        }
    }

    // ---------------- Login ----------------

    fun submitPin(pin: String) {
        if (busy || isLocked()) return
        viewModelScope.launch {
            busy = true
            val ok = withContext(Dispatchers.Default) { vault.unlockWithPin(pin.toCharArray()) }
            busy = false
            handleResult(ok, Stage.Unlocked)
        }
    }

    /** forgotPin = true ho to kamyabi ke baad naya PIN set karwaya jata hai. */
    fun submitMaster(password: String, forgotPin: Boolean) {
        if (busy || isLocked()) return
        viewModelScope.launch {
            busy = true
            val ok = withContext(Dispatchers.Default) { vault.unlockWithMaster(password.toCharArray()) }
            busy = false
            handleResult(ok, if (forgotPin) Stage.NewPin else Stage.Unlocked)
        }
    }

    fun submitRecovery(code: String, newMaster: String) {
        if (busy || isLocked()) return
        if (!com.digitalvault.security.KeyVault.isMasterPasswordAcceptable(newMaster.toCharArray())) {
            message = S.weakMaster
            return
        }
        viewModelScope.launch {
            busy = true
            val ok = withContext(Dispatchers.Default) { vault.unlockWithRecovery(code, newMaster.toCharArray()) }
            busy = false
            handleResult(ok, Stage.NewPin, S.failedRecovery)
        }
    }

    fun setNewPin(pin: String) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            withContext(Dispatchers.Default) { vault.changePin(pin.toCharArray()) }
            busy = false
            stage = Stage.Unlocked
        }
    }

    // ---------------- Fingerprint ----------------

    fun biometricCipher(): Cipher? {
        val c = vault.biometricUnlockCipher()
        if (c == null) biometricEnabled = vault.isBiometricEnabled
        return c
    }

    fun biometricUnlocked(cipher: Cipher) {
        if (vault.finishBiometricUnlock(cipher)) {
            limiter.recordSuccess()
            refreshLock()
            viewModelScope.launch { afterUnlock(Stage.Unlocked) }
        } else {
            message = S.wrong(attemptsLeft)
        }
    }

    fun enrollCipher(): Cipher? = runCatching { vault.biometricEnrollCipher() }.getOrNull()

    fun biometricEnrolled(cipher: Cipher) {
        runCatching { vault.finishBiometricEnroll(cipher) }
        biometricEnabled = vault.isBiometricEnabled
        stage = Stage.Unlocked
    }

    fun skipFingerprint() { stage = Stage.Unlocked }

    // ---------------- Lock ----------------

    /** Activity ke onStop par: background mein jate hi dobara lock. */
    fun onAppBackgrounded() {
        when (stage) {
            Stage.Unlocked, Stage.NewPin, Stage.FingerprintOffer -> lockNow()
            else -> Unit
        }
    }

    fun lockNow() {
        vault.lock()
        message = null
        stage = if (vault.isSetUp) Stage.Login else Stage.Setup
    }

    // ---------------- Andar ----------------

    private fun isLocked(): Boolean {
        refreshLock()
        return lockUntil > System.currentTimeMillis()
    }

    private fun refreshLock() {
        val rem = limiter.lockRemainingMs()
        lockUntil = if (rem > 0) System.currentTimeMillis() + rem else 0L
        attemptsLeft = limiter.attemptsLeft()
    }

    private suspend fun handleResult(ok: Boolean, next: Stage, failText: String? = null) {
        if (ok) {
            limiter.recordSuccess()
            refreshLock()
            afterUnlock(next)
        } else {
            limiter.recordFailure()
            refreshLock()
            message = if (lockUntil > 0) null else (failText?.let { "$it ${S.wrong(attemptsLeft)}" } ?: S.wrong(attemptsLeft))
        }
    }

    private suspend fun afterUnlock(next: Stage) {
        withContext(Dispatchers.IO) { repo.seedDefaultCategories() }
        message = null
        stage = next
    }
}
