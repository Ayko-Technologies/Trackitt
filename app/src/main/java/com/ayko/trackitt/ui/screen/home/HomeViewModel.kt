package com.ayko.trackitt.ui.screen.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.sms.SmsBackfillWorker
import com.ayko.trackitt.ui.component.ChartCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class Period { TODAY, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod

    private val now = LocalDate.now()
    val currentMonthName: String = now.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val currentYear: Int = now.year

    @OptIn(ExperimentalCoroutinesApi::class)
    val total: Flow<Double> = _selectedPeriod.flatMapLatest { period ->
        when (period) {
            Period.TODAY -> repository.getTodayTotal()
            Period.MONTH -> repository.getMonthTotal(now.year, now.monthValue)
        }
    }

    val chartCategories: Flow<List<ChartCategory>> =
        repository.getCategoryTotals(now.year, now.monthValue).map { totals ->
            totals.mapNotNull { ct ->
                val cat = try { TransactionCategory.valueOf(ct.category) } catch (_: Exception) { null }
                cat?.let { ChartCategory(it, ct.total) }
            }
        }

    val smsPermissionGranted = MutableStateFlow(false)

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun onSmsPermissionResult(granted: Boolean, context: Context) {
        smsPermissionGranted.value = granted
        if (granted) {
            val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "sms_backfill",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
