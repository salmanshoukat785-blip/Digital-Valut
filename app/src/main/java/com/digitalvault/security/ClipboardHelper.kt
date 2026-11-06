package com.digitalvault.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Copy karta hai, clip ko "sensitive" mark karta hai (Android 13+ par preview mein nahi dikhta),
 * aur 30 second baad clipboard khud saaf kar deta hai. Agar app band ho jaye to WorkManager
 * 45 second par saaf kar deta hai.
 */
object ClipboardHelper {
    private const val PREFS = "vault_clip"
    private const val CLEAR_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())

    fun copy(context: Context, text: String) {
        val app = context.applicationContext
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Digital Vault", text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        cm.setPrimaryClip(clip)
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("pending", true).commit()

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ clearIfPending(app) }, CLEAR_MS)
        WorkManager.getInstance(app).enqueueUniqueWork(
            "clip_clear", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ClipboardClearWorker>()
                .setInitialDelay(45, TimeUnit.SECONDS).build()
        )
    }

    fun clearIfPending(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pending", false)) return
        prefs.edit().putBoolean("pending", false).commit()
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip()
        else cm.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}

class ClipboardClearWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        ClipboardHelper.clearIfPending(applicationContext)
        return Result.success()
    }
}
