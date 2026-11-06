package com.digitalvault.security

import android.content.Context

/**
 * Ghalat PIN/password ki hadd. Har 5 ghalat koshishon par temporary lock lagta hai,
 * aur har agle round mein lock lamba hota jata hai: 30s, 2m, 10m, 30m, phir 1 ghanta.
 */
class AttemptLimiter(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("vault_attempts", Context.MODE_PRIVATE)

    private fun failures(): Int = prefs.getInt("failures", 0)

    /** Lock mein kitna waqt baqi hai (milliseconds). 0 = lock nahi. */
    fun lockRemainingMs(now: Long = System.currentTimeMillis()): Long {
        val remaining = prefs.getLong("lock_until", 0L) - now
        return remaining.coerceIn(0L, LOCK_STEPS_MS.last())
    }

    fun attemptsLeft(): Int = MAX_ATTEMPTS - failures() % MAX_ATTEMPTS

    fun recordFailure() {
        val f = failures() + 1
        val e = prefs.edit().putInt("failures", f)
        if (f % MAX_ATTEMPTS == 0) {
            val step = (f / MAX_ATTEMPTS - 1).coerceAtMost(LOCK_STEPS_MS.lastIndex)
            e.putLong("lock_until", System.currentTimeMillis() + LOCK_STEPS_MS[step])
        }
        e.commit()
    }

    fun recordSuccess() {
        prefs.edit().clear().commit()
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        private val LOCK_STEPS_MS = longArrayOf(30_000, 120_000, 600_000, 1_800_000, 3_600_000)
    }
}
