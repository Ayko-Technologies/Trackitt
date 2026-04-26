package com.ayko.trackitt.service.sms

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val addressIdx = it.getColumnIndexOrThrow("address")
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val sender = it.getString(addressIdx)
                val parsed = container.parseSms(sender, body) ?: continue
                repository.insertFromSms(parsed)
            }
        }

        prefs.edit().putBoolean("backfill_completed", true).apply()
        return Result.success()
    }

    companion object {
        const val KEY_LOOKBACK_DAYS = "lookback_days"
        const val DEFAULT_DAYS = 90
    }
}
