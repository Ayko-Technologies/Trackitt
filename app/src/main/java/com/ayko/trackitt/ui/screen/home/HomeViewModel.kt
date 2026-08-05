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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId

enum class Period { DAY, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository
    private val displayPrefs = application.getSharedPreferences("trackitt_display", 0)
    private fun includeFees() = displayPrefs.getBoolean("show_fees", false)
    private fun excludeTransfers() = displayPrefs.getBoolean("exclude_transfers", false)

    init {
        val container = (application as TrackittApplication).container
        viewModelScope.launch {
            repository.retryPendingClassifications()
        }
        viewModelScope.launch {
            container.networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    repository.retryPendingClassifications()
                }
            }
        }
    }

    private val _refreshTrigger = MutableStateFlow(0)
    fun refresh() { _refreshTrigger.value++ }

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod

    private val now = LocalDate.now()
    private val _selectedYear = MutableStateFlow(now.year)
    private val _selectedMonth = MutableStateFlow(now.monthValue)
    private val _selectedDay = MutableStateFlow(now.dayOfMonth)

    val currentMonthName: Flow<String> = _selectedMonth.map { monthVal ->
        Month.of(monthVal).name.lowercase().replaceFirstChar { it.uppercase() }
    }
    val currentYear: Flow<Int> = _selectedYear
    val selectedMonthValue: Int get() = _selectedMonth.value
    val selectedYearValue: Int get() = _selectedYear.value
    val selectedDayValue: Int get() = _selectedDay.value

    val dayLabel: Flow<String> = combine(_selectedDay, _selectedMonth, _selectedYear) { day, month, year ->
        val date = LocalDate.of(year, month, day)
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> "${Month.of(month).name.take(3).lowercase().replaceFirstChar { it.uppercase() }} $day"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val total: Flow<Double> = combine(
        _selectedPeriod, _selectedYear, _selectedMonth, _selectedDay, _refreshTrigger
    ) { args -> args
    }.flatMapLatest { args ->
        val period = args[0] as Period
        val year = args[1] as Int
        val month = args[2] as Int
        val day = args[3] as Int
        val totalsFlow = when (period) {
            Period.DAY -> {
                val date = LocalDate.of(year, month, day)
                val zone = ZoneId.systemDefault()
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                repository.getCategoryTotalsForRange(start, end, includeFees())
            }
            Period.MONTH -> repository.getCategoryTotals(year, month, includeFees())
        }
        totalsFlow.map { totals ->
            totals
                .filter { it.category != TransactionCategory.INCOME.name }
                .filterNot { excludeTransfers() && it.category == TransactionCategory.TRANSFER.name }
                .sumOf { it.total }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartCategories: Flow<List<ChartCategory>> =
        combine(_selectedPeriod, _selectedYear, _selectedMonth, _selectedDay, _refreshTrigger) { args -> args
        }.flatMapLatest { args ->
            val period = args[0] as Period
            val year = args[1] as Int
            val month = args[2] as Int
            val day = args[3] as Int
            val totalsFlow = when (period) {
                Period.DAY -> {
                    val date = LocalDate.of(year, month, day)
                    val zone = ZoneId.systemDefault()
                    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    repository.getCategoryTotalsForRange(start, end, includeFees())
                }
                Period.MONTH -> repository.getCategoryTotals(year, month, includeFees())
            }
            totalsFlow.map { totals ->
                totals.mapNotNull { ct ->
                    val cat = try { TransactionCategory.valueOf(ct.category) } catch (_: Exception) { null }
                    cat?.takeUnless { excludeTransfers() && it == TransactionCategory.TRANSFER }
                        ?.let { ChartCategory(it, ct.total) }
                }
            }
        }

    fun getCategoryTransactions(categoryName: String?): Flow<List<com.ayko.trackitt.domain.model.Transaction>> {
        if (categoryName == null) return kotlinx.coroutines.flow.flowOf(emptyList())
        val period = _selectedPeriod.value
        val year = _selectedYear.value
        val month = _selectedMonth.value
        val day = _selectedDay.value
        return when (period) {
            Period.DAY -> {
                val date = LocalDate.of(year, month, day)
                val zone = ZoneId.systemDefault()
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                repository.getByCategoryForRange(categoryName, start, end)
            }
            Period.MONTH -> repository.getByCategory(categoryName, year, month)
        }
    }

    val smsPermissionGranted = MutableStateFlow(false)

    fun selectPeriod(period: Period) {
        if (period == Period.DAY) {
            _selectedDay.value = LocalDate.now().dayOfMonth
        }
        _selectedPeriod.value = period
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        _selectedPeriod.value = Period.MONTH
    }

    fun setDay(day: Int) {
        _selectedDay.value = day
        _selectedPeriod.value = Period.DAY
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
