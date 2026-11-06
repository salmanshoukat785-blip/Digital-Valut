package com.digitalvault.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-GCM aur key derivation. Output format: IV(12 bytes) + ciphertext+tag. */
object Crypto {
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private val rng = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val iv = randomBytes(IV_SIZE)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return iv + c.doFinal(plain)
    }

    /** Ghalat key ya kharab data par GeneralSecurityException phenkta hai. */
    @Throws(GeneralSecurityException::class)
    fun decrypt(key: ByteArray, blob: ByteArray): ByteArray {
        if (blob.size <= IV_SIZE) throw GeneralSecurityException("blob too short")
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(
            Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, blob.copyOfRange(0, IV_SIZE))
        )
        return c.doFinal(blob, IV_SIZE, blob.size - IV_SIZE)
    }

    fun encryptString(key: ByteArray, text: String): ByteArray = encrypt(key, text.toByteArray(Charsets.UTF_8))
    fun decryptString(key: ByteArray, blob: ByteArray): String = String(decrypt(key, blob), Charsets.UTF_8)

    fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun wipe(b: ByteArray?) { b?.fill(0) }
    fun wipe(c: CharArray?) { c?.fill('\u0000') }
}
