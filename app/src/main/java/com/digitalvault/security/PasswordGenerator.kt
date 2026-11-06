package com.digitalvault.security

import java.security.SecureRandom

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.?"
    private val rng = SecureRandom()

    /** length 8 se 32. Har chuni hui qism ka kam az kam ek huroof zaroor aata hai. */
    fun generate(
        length: Int = 16,
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true
    ): String {
        val sets = buildList {
            if (upper) add(UPPER)
            if (lower) add(LOWER)
            if (digits) add(DIGITS)
            if (symbols) add(SYMBOLS)
        }.ifEmpty { listOf(LOWER) }
        val len = length.coerceIn(8, 32)
        val all = sets.joinToString("")
        val chars = ArrayList<Char>(len)
        sets.forEach { chars.add(it[rng.nextInt(it.length)]) }
        while (chars.size < len) chars.add(all[rng.nextInt(all.length)])
        for (i in chars.indices.reversed()) {   // Fisher-Yates shuffle
            val j = rng.nextInt(i + 1)
            val t = chars[i]; chars[i] = chars[j]; chars[j] = t
        }
        return chars.joinToString("")
    }
}
