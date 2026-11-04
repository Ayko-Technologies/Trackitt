package com.ayko.trackitt.service.sms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ayko.trackitt.MainActivity
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
        val cutoff = if (days == ALL_TIME) 0L else java.time.LocalDate.now()
            .minusDays(days.toLong() - 1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        // Android may stop the import (leaving the app, low memory). WorkManager then runs it again,
        // and it carries on from the last message it finished instead of starting over.
        val resumeFrom = maxOf(cutoff, prefs.getLong(KEY_CHECKPOINT, 0L))

        // Every sender is read so new banks and wallets are picked up; importSms skips the rest.
        val cursor = applicationContext.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date", "address"),
            "date > ?",
            arrayOf(resumeFrom.toString()),
            "date ASC"
        ) ?: return Result.failure()

        showProgress("Reading your SMS", 0, cursor.count)
        var count = prefs.getInt(KEY_IMPORTED, 0)
        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val addressIdx = it.getColumnIndexOrThrow("address")
            val dateIdx = it.getColumnIndexOrThrow("date")
            var lastDate = resumeFrom
            while (it.moveToNext()) {
                if (it.position % PROGRESS_EVERY == 0) {
                    showProgress("Reading your SMS", it.position, it.count)
                    // Only a fully processed timestamp is safe to resume after.
                    prefs.edit().putLong(KEY_CHECKPOINT, lastDate - 1).putInt(KEY_IMPORTED, count).apply()
                }
                val smsTimestamp = it.getLong(dateIdx)
                lastDate = smsTimestamp
                val body = it.getString(bodyIdx) ?: continue
                val sender = it.getString(addressIdx)
                if (container.importSms(sender, body, smsTimestamp, bulk = true) != null) count++
            }
        }
        repository.relinkOwnTransfers()

        prefs.edit()
            .putBoolean("backfill_completed", true)
            .remove(KEY_CHECKPOINT)
            .remove(KEY_IMPORTED)
            .apply()
        cancelProgress()
        showDoneNotification(count)

        // On-device AI categorises what the dictionary couldn't the next time the app is open:
        // Android only lets Gemini Nano run for the app on screen.
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(progressNotification("Reading your SMS", 0, 0))

    /**
     * Shows import progress and runs the work in the foreground, which Android lets run past the usual
     * ten-minute limit. If the app is in the background Android may refuse; the import still runs.
     */
    private suspend fun showProgress(title: String, done: Int, total: Int) {
        val notification = progressNotification(title, done, total)
        try {
            setForeground(foregroundInfo(notification))
        } catch (_: IllegalStateException) {
            // Android refuses a foreground start while the app is in the background. The import
            // carries on regardless; show the same progress as a plain notification.
            notificationManager().notify(PROGRESS_NOTIFICATION_ID, notification)
        }
    }

    private fun cancelProgress() = notificationManager().cancel(PROGRESS_NOTIFICATION_ID)

    private fun notificationManager() =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun progressNotification(title: String, done: Int, total: Int) =
        ScanNotifications.progress(applicationContext, title, done, total)

    private fun foregroundInfo(notification: android.app.Notification) =
        ScanNotifications.foregroundInfo(PROGRESS_NOTIFICATION_ID, notification)

    private fun showDoneNotification(count: Int) {
        val nm = notificationManager()
        ScanNotifications.ensureChannel(applicationContext)
        val title = if (count == 0) "All caught up" else "SMS scan complete"
        val text = if (count == 0) "Your transactions are up to date" else "$count new transaction${if (count != 1) "s" else ""} imported"
        val notification = NotificationCompat.Builder(applicationContext, ScanNotifications.CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(buildLaunchAppPendingIntent())
            .setAutoCancel(true)
            .build()
        nm.notify(9999, notification)
    }

    private fun buildLaunchAppPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val KEY_LOOKBACK_DAYS = "lookback_days"
        const val DEFAULT_DAYS = 180
        const val ALL_TIME = 0
        private const val WORK_NAME = "sms_backfill"
        private const val KEY_CHECKPOINT = "backfill_checkpoint"
        private const val KEY_IMPORTED = "backfill_imported"
        private const val PROGRESS_NOTIFICATION_ID = 9998
        private const val PROGRESS_EVERY = 250

        /**
         * Starts a fresh import of the last [days] days (or [ALL_TIME]). It runs as expedited work so it
         * begins at once, while the app is still open, and keeps going after the user leaves the app.
         */
        fun start(context: Context, days: Int) {
            context.getSharedPreferences("trackitt", Context.MODE_PRIVATE).edit()
                .putBoolean("backfill_completed", false)
                .putInt("backfill_days", days)
                .remove(KEY_CHECKPOINT)
                .remove(KEY_IMPORTED)
                .apply()
            val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>()
                .setInputData(workDataOf(KEY_LOOKBACK_DAYS to days))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Finishes an import that hasn't completed (first launch, or after the app was updated). */
        fun resumeIfUnfinished(context: Context) {
            val prefs = context.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
            if (prefs.getBoolean("backfill_completed", false)) return
            val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun isRunning(context: Context): Boolean =
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
                .any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }

        fun describe(days: Int): String = when (days) {
            ALL_TIME -> "all time"
            1 -> "today"
            else -> "the last $days days"
        }
    }
}
