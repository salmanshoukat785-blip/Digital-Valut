package com.digitalvault.security

enum class Strength { WEAK, MEDIUM, STRONG }

object PasswordStrength {
    fun evaluate(p: String): Strength {
        if (p.length < 8 || p.toSet().size <= 3) return Strength.WEAK
        val kinds = listOf(
            p.any { it.isUpperCase() }, p.any { it.isLowerCase() },
            p.any { it.isDigit() }, p.any { !it.isLetterOrDigit() }
        ).count { it }
        return when {
            p.length >= 12 && kinds >= 3 -> Strength.STRONG
            p.length >= 16 && kinds >= 2 -> Strength.STRONG
            kinds >= 2 -> Strength.MEDIUM
            else -> Strength.WEAK
        }
    }
}
