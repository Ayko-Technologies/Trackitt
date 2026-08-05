package com.ayko.trackitt.service.budget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BudgetEstimateScheduler {

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val periodicWork = PeriodicWorkRequestBuilder<BudgetEstimateWorker>(1, TimeUnit.DAYS)
            .build()
        val immediateWork = OneTimeWorkRequestBuilder<BudgetEstimateWorker>().build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork,
        )
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateWork,
        )
    }

    private const val PERIODIC_WORK_NAME = "budget_estimate_daily"
    private const val IMMEDIATE_WORK_NAME = "budget_estimate_bootstrap"
}
