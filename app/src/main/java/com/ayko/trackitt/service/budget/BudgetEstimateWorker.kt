package com.ayko.trackitt.service.budget

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ayko.trackitt.TrackittApplication

class BudgetEstimateWorker(
    appContext: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        app.container.budgetEstimateNotifications.notifyForTodayIfNeeded()
        return Result.success()
    }
}
