package com.ayko.trackitt.service.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ayko.trackitt.R
import com.ayko.trackitt.TrackittApplication

class SmsBackfillWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        val container = app.container
        val repository = container.repository

        val prefs = applicationContext.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
        if (prefs.getBoolean("backfill_completed", false)) return Result.success()

        val days = inputData.getInt(KEY_LOOKBACK_DAYS, prefs.getInt("backfill_days", DEFAULT_DAYS))
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)

        val senders = container.smsSenders
        val placeholders = senders.joinToString(",") { "?" }

        val cursor = applicationContext.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date", "address"),
            "date > ? AND address IN ($placeholders)",
            arrayOf(cutoff.toString()) + senders.toTypedArray(),
            "date ASC"
        ) ?: return Result.failure()

        var count = 0
        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val addressIdx = it.getColumnIndexOrThrow("address")
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val sender = it.getString(addressIdx)
                val parsed = container.parseSms(sender, body) ?: continue
                repository.insertFromSms(parsed)
                count++
            }
        }

        prefs.edit().putBoolean("backfill_completed", true).apply()
        showDoneNotification(count)
        return Result.success()
    }

    private fun showDoneNotification(count: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("trackitt_scan", "SMS Scan", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val text = if (count == 0) "No new transactions found" else "$count transaction${if (count != 1) "s" else ""} imported"
        val notification = NotificationCompat.Builder(applicationContext, "trackitt_scan")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SMS scan complete")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(9999, notification)
    }

    companion object {
        const val KEY_LOOKBACK_DAYS = "lookback_days"
        const val DEFAULT_DAYS = 90
    }
}
