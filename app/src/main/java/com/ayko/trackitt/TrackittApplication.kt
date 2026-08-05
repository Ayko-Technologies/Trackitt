package com.ayko.trackitt

import android.app.Application
import com.ayko.trackitt.service.budget.BudgetEstimateScheduler

class TrackittApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BudgetEstimateScheduler.schedule(this)
    }
}
