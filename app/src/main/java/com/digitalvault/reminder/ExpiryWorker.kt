package com.digitalvault.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.digitalvault.data.VaultDatabase
import java.util.concurrent.TimeUnit

/**
 * Roz ek dafa chalta hai. Vault locked hota hai, is liye ye kuch decrypt nahi karta:
 * sirf expiry tareekhon ki ginti dekh kar aam sa notification deta hai (koi naam ya password nahi).
 */
class ExpiryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val soon = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val count = VaultDatabase.get(applicationContext).accounts().countExpiring(soon)
        if (count > 0) notify(count)
        return Result.success()
    }

    private fun notify(count: Int) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Password reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Digital Vault")
            .setContentText("$count password(s) badalne ka waqt aa gaya hai")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, n)
    }

    companion object { private const val CHANNEL = "expiry" }
}

object ExpiryScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "expiry_check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS).build()
        )
    }
}
