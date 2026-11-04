package com.ayko.trackitt.service.sms

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ayko.trackitt.TrackittApplication

/**
 * Categorises imported transactions the dictionary couldn't place, using on-device AI. It can take a
 * while after a big import, so it runs as its own work: if Android stops it, WorkManager runs it
 * again later and it continues with whatever is still uncategorised.
 */
class PendingClassificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        try {
            app.container.repository.retryPendingClassifications { done, total ->
                if (done % PROGRESS_EVERY == 0) showProgress(done, total)
            }
        } finally {
            notificationManager().cancel(NOTIFICATION_ID)
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ScanNotifications.foregroundInfo(NOTIFICATION_ID, ScanNotifications.progress(applicationContext, TITLE, 0, 0))

    private suspend fun showProgress(done: Int, total: Int) {
        val notification = ScanNotifications.progress(applicationContext, TITLE, done, total)
        try {
            setForeground(ScanNotifications.foregroundInfo(NOTIFICATION_ID, notification))
        } catch (_: IllegalStateException) {
            notificationManager().notify(NOTIFICATION_ID, notification)
        }
    }

    private fun notificationManager() =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val WORK_NAME = "classify_pending"
        private const val NOTIFICATION_ID = 9997
        private const val PROGRESS_EVERY = 10
        private const val TITLE = "Categorising transactions"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PendingClassificationWorker>().build(),
            )
        }
    }
}
