package com.ayko.trackitt.service.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ayko.trackitt.TrackittApplication
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Imports SMS that [SmsBroadcastReceiver] missed. Android delivers no SMS to an app that was
 * force-stopped until it is opened again, and it can kill a receiver part-way through, so this
 * reads the inbox from where the last scan finished. importSms skips messages already imported.
 */
class SmsCatchUpWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = lock.withLock {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
        // Until the first full import finishes, it covers everything up to now itself.
        if (!prefs.getBoolean("backfill_completed", false)) return Result.success()
        val canRead = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canRead) return Result.success()

        val from = prefs.getLong(KEY_SCANNED_UNTIL, System.currentTimeMillis() - FIRST_RUN_LOOKBACK_MILLIS)
        val cursor = applicationContext.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date", "address"),
            "date > ?",
            arrayOf(from.toString()),
            "date ASC",
        ) ?: return Result.retry()

        var scannedUntil = from
        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val addressIdx = it.getColumnIndexOrThrow("address")
            val dateIdx = it.getColumnIndexOrThrow("date")
            while (it.moveToNext()) {
                val smsTimestamp = it.getLong(dateIdx)
                val body = it.getString(bodyIdx)
                if (body != null) app.container.importSms(it.getString(addressIdx), body, smsTimestamp)
                scannedUntil = smsTimestamp
            }
        }
        markScanned(applicationContext, scannedUntil)
        Result.success()
    }

    companion object {
        private const val KEY_SCANNED_UNTIL = "sms_scanned_until"
        private const val WORK_NAME = "sms_catch_up"
        private const val PERIODIC_WORK_NAME = "sms_catch_up_periodic"
        private val FIRST_RUN_LOOKBACK_MILLIS = TimeUnit.DAYS.toMillis(2)
        private val lock = Mutex()

        /** Checks the inbox now, and every few hours while the app isn't opened. */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SmsCatchUpWorker>(6, TimeUnit.HOURS).build(),
            )
            runNow(context)
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SmsCatchUpWorker>().build(),
            )
        }

        /** Records that every inbox message up to [date] has been through importSms. */
        fun markScanned(context: Context, date: Long) {
            val prefs = context.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
            if (date > prefs.getLong(KEY_SCANNED_UNTIL, 0L)) {
                prefs.edit().putLong(KEY_SCANNED_UNTIL, date).apply()
            }
        }
    }
}
