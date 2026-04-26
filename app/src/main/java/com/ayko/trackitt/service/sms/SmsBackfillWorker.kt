package com.ayko.trackitt.service.sms

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ayko.trackitt.TrackittApplication

class SmsBackfillWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        val parser = app.container.smsParser
        val repository = app.container.repository

        val prefs = applicationContext.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
        if (prefs.getBoolean("backfill_completed", false)) return Result.success()

        val days = inputData.getInt(KEY_LOOKBACK_DAYS, prefs.getInt("backfill_days", DEFAULT_DAYS))
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)

        val cursor = applicationContext.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date"),
            "date > ?",
            arrayOf(cutoff.toString()),
            "date ASC"
        ) ?: return Result.failure()

        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val parsed = parser.parse(body) ?: continue
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
