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
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var backgroundedAt = 0L
    private var suppressBackground = false
    private var lockJob: Job? = null

    /** Bahar ki screen (file picker) kholne se pehle bulayein, taake us waqt vault lock na ho. */
    fun suppressLockOnce() { suppressBackground = true }

    private fun isUnlockedStage() =
        stage == Stage.Unlocked || stage == Stage.NewPin || stage == Stage.FingerprintOffer

    /** Activity ke onStop par. timeoutMs = 0 ho to foran lock, warna itne waqt baad. */
    fun onAppBackgrounded(timeoutMs: Long) {
        if (!isUnlockedStage() || suppressBackground) return
        if (timeoutMs <= 0) { lockNow(); return }
        backgroundedAt = SystemClock.elapsedRealtime()
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            delay(timeoutMs)
            if (backgroundedAt > 0) lockNow()
        }
    }

    /** Activity ke onStart par: agar waqt guzar gaya ho to lock. */
    fun onAppForegrounded(timeoutMs: Long) {
        suppressBackground = false
        lockJob?.cancel()
        val since = backgroundedAt
        backgroundedAt = 0
        if (since > 0 && isUnlockedStage() && SystemClock.elapsedRealtime() - since >= timeoutMs) lockNow()
    }

    // ---------------- Settings ke hassas kaam ----------------

    class OpResult(val error: String?, val value: String? = null)

    private fun lockText(): String {
        val s = ((lockUntil - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(1)
        return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
    }

    /** Hassas kaam se pehle Master Password dobara. null = theek, warna error text. */
    suspend fun reauth(password: String): String? {
        if (isLocked()) return S.locked(lockText())
        val ok = withContext(Dispatchers.Default) { vault.verifyMaster(password.toCharArray()) }
        if (ok) {
            limiter.recordSuccess()
            refreshLock()
            return null
        }
        limiter.recordFailure()
        refreshLock()
        return if (lockUntil > 0) S.locked(lockText()) else S.wrong(attemptsLeft)
    }

    suspend fun changePin(master: String, newPin: String): String? {
        reauth(master)?.let { return it }
        withContext(Dispatchers.Default) { vault.changePin(newPin.toCharArray()) }
        return null
    }

    suspend fun changeMaster(old: String, new: String): String? {
        if (!com.digitalvault.security.KeyVault.isMasterPasswordAcceptable(new.toCharArray())) return S.weakMaster
        reauth(old)?.let { return it }
        val ok = withContext(Dispatchers.Default) {
            vault.changeMasterPassword(old.toCharArray(), new.toCharArray())
        }
        return if (ok) null else S.wrong(attemptsLeft)
    }

    suspend fun newRecoveryKey(master: String): OpResult {
        reauth(master)?.let { return OpResult(it) }
        return OpResult(null, withContext(Dispatchers.Default) { vault.regenerateRecoveryKey() })
    }

    fun disableBiometric() {
        vault.disableBiometric()
        biometricEnabled = false
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
