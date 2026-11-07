package com.digitalvault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Key ka nizam:
 *  - Data Key: 32 random bytes. Poora data isi se AES-256-GCM encrypt hota hai.
 *  - Data Key teen jagah alag alag lapeti (wrap) hoti hai:
 *      1) Master Password (PBKDF2-SHA256, 210k iterations)
 *      2) Recovery Key (256-bit random)
 *      3) Android Keystore key (PIN ke liye) aur auth-wali Keystore key (fingerprint ke liye)
 *  - 4 digit PIN kabhi encryption key nahi banta, sirf Keystore wali key tak rasai ka darwaza hai.
 *    Ghalat PIN ki hadd Part 3 mein lagegi.
 */
class KeyVault(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vault_meta", Context.MODE_PRIVATE)

    @Volatile private var dataKey: ByteArray? = null

    val isSetUp: Boolean get() = prefs.contains(K_PIN_BLOB)
    val isUnlocked: Boolean get() = dataKey != null
    val isBiometricEnabled: Boolean get() = prefs.contains(K_BIO_BLOB)

    /** Unlock hone ke baad hi key milti hai, warna IllegalStateException. */
    fun requireKey(): ByteArray = dataKey ?: throw IllegalStateException("Vault locked")

    fun lock() {
        Crypto.wipe(dataKey)
        dataKey = null
    }

    // ---------- Pehli baar setup ----------

    /**
     * Pehli baar setup. Recovery Key ke bytes pehle bana kar user ko dikhaye ja chuke hote hain,
     * aur user ki tasdeeq ke baad hi ye call hota hai, taake adhura setup kabhi na bache.
     */
    fun setup(master: CharArray, pin: CharArray, recoveryBytes: ByteArray) {
        require(isMasterPasswordAcceptable(master)) { "Master Password kamzor hai" }
        require(isPinFormatOk(pin)) { "PIN 4 se 6 hindsay ka ho" }
        require(recoveryBytes.size == 32) { "Recovery Key ghalat size ki hai" }
        val dk = Crypto.randomBytes(32)
        wrapWithMaster(dk, master)
        prefs.edit()
            .putString(K_RECOVERY_BLOB, b64(Crypto.encrypt(recoveryKek(recoveryBytes), dk)))
            .commit()
        storePin(dk, pin)   // aakhir mein: isSetUp isi par depend karta hai
        dataKey = dk
    }

    /** Naya Recovery Key (purana kaam karna band kar deta hai). Vault unlocked hona zaroori hai. */
    fun regenerateRecoveryKey(): String {
        val dk = requireKey()
        val bytes = Crypto.randomBytes(32)
        try {
            prefs.edit().putString(K_RECOVERY_BLOB, b64(Crypto.encrypt(recoveryKek(bytes), dk))).commit()
            return RecoveryCode.encode(bytes)
        } finally {
            Crypto.wipe(bytes)
        }
    }

    // ---------- Unlock ----------

    fun unlockWithMaster(master: CharArray): Boolean {
        val dk = unwrapMaster(master) ?: return false
        dataKey = dk
        return true
    }

    fun unlockWithPin(pin: CharArray): Boolean {
        val salt = get(K_PIN_SALT) ?: return false
        val expected = get(K_PIN_HASH) ?: return false
        val actual = Crypto.pbkdf2(pin, salt, PIN_ITER)
        if (!MessageDigest.isEqual(expected, actual)) return false
        val blob = get(K_PIN_BLOB) ?: return false
        return try {
            val c = Cipher.getInstance(GCM)
            c.init(Cipher.DECRYPT_MODE, gateKey(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
            dataKey = c.doFinal(blob, 12, blob.size - 12)
            true
        } catch (e: GeneralSecurityException) {
            false
        }
    }

    /** Master Password bhool jaye to Recovery Key se naya Master Password set hota hai. */
    fun unlockWithRecovery(code: String, newMaster: CharArray): Boolean {
        if (!isMasterPasswordAcceptable(newMaster)) return false
        val bytes = RecoveryCode.decode(code) ?: return false
        return try {
            val dk = Crypto.decrypt(recoveryKek(bytes), get(K_RECOVERY_BLOB) ?: return false)
            wrapWithMaster(dk, newMaster)
            dataKey = dk
            true
        } catch (e: GeneralSecurityException) {
            false
        } finally {
            Crypto.wipe(bytes)
        }
    }

    /** Sirf check: Master Password theek hai ya nahi. Koi state nahi badalta. */
    fun verifyMaster(master: CharArray): Boolean {
        val dk = unwrapMaster(master) ?: return false
        Crypto.wipe(dk)
        return true
    }

    // ---------- Tabdeeli ----------

    fun changeMasterPassword(old: CharArray, new: CharArray): Boolean {
        if (!isMasterPasswordAcceptable(new)) return false
        val dk = unwrapMaster(old) ?: return false
        wrapWithMaster(dk, new)
        return true
    }

    /** Vault unlocked hona zaroori hai. */
    fun changePin(newPin: CharArray) {
        require(isPinFormatOk(newPin))
        storePin(requireKey(), newPin)
    }

    // ---------- Fingerprint (BiometricPrompt CryptoObject ke sath) ----------

    /** Enable karne ke liye: ye cipher BiometricPrompt ko do, kamyabi par finishBiometricEnroll. */
    fun biometricEnrollCipher(): Cipher {
        val ks = keyStore()
        if (ks.containsAlias(BIO_ALIAS)) ks.deleteEntry(BIO_ALIAS)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                BIO_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        val c = Cipher.getInstance(GCM)
        c.init(Cipher.ENCRYPT_MODE, gen.generateKey())
        return c
    }

    fun finishBiometricEnroll(authedCipher: Cipher) {
        val blob = authedCipher.iv + authedCipher.doFinal(requireKey())
        prefs.edit().putString(K_BIO_BLOB, b64(blob)).commit()
    }

    /** Unlock ke liye cipher. Fingerprint badalne par key invalid ho jati hai, tab null milta hai. */
    fun biometricUnlockCipher(): Cipher? {
        val blob = get(K_BIO_BLOB) ?: return null
        return try {
            val key = keyStore().getKey(BIO_ALIAS, null) as? SecretKey ?: return null
            val c = Cipher.getInstance(GCM)
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(0, 12)))
            c
        } catch (e: KeyPermanentlyInvalidatedException) {
            disableBiometric()
            null
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    fun finishBiometricUnlock(authedCipher: Cipher): Boolean {
        val blob = get(K_BIO_BLOB) ?: return false
        return try {
            dataKey = authedCipher.doFinal(blob, 12, blob.size - 12)
            true
        } catch (e: GeneralSecurityException) {
            false
        }
    }

    fun disableBiometric() {
        prefs.edit().remove(K_BIO_BLOB).commit()
        runCatching { keyStore().deleteEntry(BIO_ALIAS) }
    }

    // ---------- Andar ke madadgar ----------

    private fun wrapWithMaster(dk: ByteArray, master: CharArray) {
        val salt = Crypto.randomBytes(16)
        val kek = Crypto.pbkdf2(master, salt, MASTER_ITER)
        try {
            prefs.edit()
                .putString(K_MASTER_SALT, b64(salt))
                .putString(K_MASTER_BLOB, b64(Crypto.encrypt(kek, dk)))
                .commit()
        } finally {
            Crypto.wipe(kek)
        }
    }

    private fun unwrapMaster(master: CharArray): ByteArray? {
        val salt = get(K_MASTER_SALT) ?: return null
        val blob = get(K_MASTER_BLOB) ?: return null
        val kek = Crypto.pbkdf2(master, salt, MASTER_ITER)
        return try {
            Crypto.decrypt(kek, blob)
        } catch (e: GeneralSecurityException) {
            null
        } finally {
            Crypto.wipe(kek)
        }
    }

    private fun storePin(dk: ByteArray, pin: CharArray) {
        val salt = Crypto.randomBytes(16)
        val hash = Crypto.pbkdf2(pin, salt, PIN_ITER)
        val c = Cipher.getInstance(GCM)
        c.init(Cipher.ENCRYPT_MODE, gateKey())
        val blob = c.iv + c.doFinal(dk)
        prefs.edit()
            .putString(K_PIN_SALT, b64(salt))
            .putString(K_PIN_HASH, b64(hash))
            .putString(K_PIN_BLOB, b64(blob))
            .commit()
    }

    private fun recoveryKek(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update("digitalvault-recovery-v1".toByteArray())
            digest(bytes)
        }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    /** PIN wali Keystore key: device se bandhi hui, user-auth ke baghair. */
    private fun gateKey(): SecretKey {
        val ks = keyStore()
        (ks.getKey(GATE_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                GATE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun get(k: String): ByteArray? =
        prefs.getString(k, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)

    companion object {
        private const val GCM = "AES/GCM/NoPadding"
        private const val MASTER_ITER = 210_000
        private const val PIN_ITER = 100_000
        private const val GATE_ALIAS = "dv_pin_gate_key"
        private const val BIO_ALIAS = "dv_biometric_key"

        private const val K_MASTER_SALT = "master_salt"
        private const val K_MASTER_BLOB = "master_blob"
        private const val K_RECOVERY_BLOB = "recovery_blob"
        private const val K_PIN_SALT = "pin_salt"
        private const val K_PIN_HASH = "pin_hash"
        private const val K_PIN_BLOB = "pin_blob"
        private const val K_BIO_BLOB = "bio_blob"

        /** Kam az kam 10 huroof aur (bara, chhota, number, symbol) mein se 3 qisamein. */
        fun isMasterPasswordAcceptable(p: CharArray): Boolean {
            if (p.size < 10) return false
            val kinds = listOf(
                p.any { it.isUpperCase() }, p.any { it.isLowerCase() },
                p.any { it.isDigit() }, p.any { !it.isLetterOrDigit() }
            ).count { it }
            return kinds >= 3
        }

        fun isPinFormatOk(p: CharArray): Boolean = p.size in 4..6 && p.all { it in '0'..'9' }
    }
}
