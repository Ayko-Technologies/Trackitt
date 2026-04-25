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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Month

enum class Period { TODAY, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod

    private val now = LocalDate.now()
    private val _selectedYear = MutableStateFlow(now.year)
    private val _selectedMonth = MutableStateFlow(now.monthValue)

    val currentMonthName: Flow<String> = _selectedMonth.map { monthVal ->
        Month.of(monthVal).name.lowercase().replaceFirstChar { it.uppercase() }
    }
    val currentYear: Flow<Int> = _selectedYear
    val selectedMonthValue: Int get() = _selectedMonth.value
    val selectedYearValue: Int get() = _selectedYear.value

    @OptIn(ExperimentalCoroutinesApi::class)
    val total: Flow<Double> = combine(_selectedPeriod, _selectedYear, _selectedMonth) { period, year, month ->
        Triple(period, year, month)
    }.flatMapLatest { (period, year, month) ->
        when (period) {
            Period.TODAY -> repository.getTodayTotal()
            Period.MONTH -> repository.getMonthTotal(year, month)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartCategories: Flow<List<ChartCategory>> =
        combine(_selectedYear, _selectedMonth) { year, month -> year to month }
            .flatMapLatest { (year, month) ->
                repository.getCategoryTotals(year, month).map { totals ->
                    totals.mapNotNull { ct ->
                        val cat = try { TransactionCategory.valueOf(ct.category) } catch (_: Exception) { null }
                        cat?.let { ChartCategory(it, ct.total) }
                    }
                }
            }

    val smsPermissionGranted = MutableStateFlow(false)

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        _selectedPeriod.value = Period.MONTH
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
