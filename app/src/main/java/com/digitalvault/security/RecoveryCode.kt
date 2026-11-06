package com.digitalvault.security

/** 32 random bytes ko parhne wale Base32 code (XXXX-XXXX-...) mein badalta hai. */
object RecoveryCode {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                sb.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        if (bits > 0) sb.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return sb.chunked(4).joinToString("-")
    }

    /** Ghalat code par null. Dash, space aur chhote huroof maaf hain. */
    fun decode(code: String): ByteArray? {
        val clean = code.uppercase().filter { it != '-' && !it.isWhitespace() }
        val out = ArrayList<Byte>()
        var buffer = 0
        var bits = 0
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.add(((buffer shr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        return if (out.size == 32) out.toByteArray() else null
    }
}
