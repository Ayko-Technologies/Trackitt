package com.ayko.trackitt.data.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ayko.trackitt.MainActivity
import com.ayko.trackitt.R
import java.util.concurrent.TimeUnit

/**
 * Opt-in weekly backup to the file the user chose ([BackupLocation]). Off unless the user turns it on.
 * Trackitt itself never uploads anything: if the file is in Google Drive, the Drive app syncs it.
 */
object AutoBackup {

    private const val PREFS = "backup"
    private const val KEY_ENABLED = "auto_enabled"
    private const val WORK_NAME = "auto_backup"
    private const val CHANNEL = "trackitt_backup"
    private const val NOTIFICATION_ID = 9996

    fun isEnabled(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(KEY_ENABLED, false)

    fun enable(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_ENABLED, true).apply()
        val request = PeriodicWorkRequestBuilder<Worker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_ENABLED, false).apply()
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            if (!isEnabled(applicationContext)) return Result.success()
            return when (val result = BackupLocation.backUp(applicationContext)) {
                BackupResult.Success -> Result.success()
                is BackupResult.Failure -> {
                    if (result.message == BackupLocation.LOST_ACCESS) {
                        // Retrying can't fix this: only the user can choose the file again.
                        notifyLostAccess()
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
            }
        }

        private fun notifyLostAccess() {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "Backups", NotificationManager.IMPORTANCE_DEFAULT))
            }
            val open = PendingIntent.getActivity(
                applicationContext,
                2,
                Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Trackitt couldn't back up")
                .setContentText("Open Settings and choose where to keep your backup again.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        }
    }
}
