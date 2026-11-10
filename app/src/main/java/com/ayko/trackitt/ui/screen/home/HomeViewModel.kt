package com.ayko.trackitt.ui.screen.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.sms.SmsBackfillWorker
import com.ayko.trackitt.ui.component.ChartCategory
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId

enum class Period { DAY, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository
    private val displayPrefs = application.getSharedPreferences("trackitt_display", 0)
    private fun includeFees() = displayPrefs.getBoolean("show_fees", false)

    init {
        viewModelScope.launch {
            // Cheap, and fixes transfers between the user's own accounts stored before pairing existed.
            repository.relinkOwnTransfers()
            repository.forgetPaymentServiceCategories()
            val prefs = application.getSharedPreferences("trackitt", 0)
            if (!prefs.getBoolean(KEY_REVIEWED_AUTO_TRANSFERS, false)) {
                repository.reviewAutoTransfers()
                prefs.edit().putBoolean(KEY_REVIEWED_AUTO_TRANSFERS, true).apply()
            }
            if (prefs.getInt(KEY_DICTIONARY_VERSION, 0) < DICTIONARY_VERSION) {
                repository.recategorizeFromDictionary()
                prefs.edit().putInt(KEY_DICTIONARY_VERSION, DICTIONARY_VERSION).apply()
            }
        }
        SmsBackfillWorker.resumeIfUnfinished(application)
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
        when (period) {
            Period.DAY -> {
                val date = LocalDate.of(year, month, day)
                val zone = ZoneId.systemDefault()
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                repository.getDayTotal(start, end, includeFees())
            }
            Period.MONTH -> repository.getMonthTotal(year, month, includeFees())
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
                    repository.getChartTotalsForRange(start, end, includeFees())
                }
                Period.MONTH -> repository.getChartTotals(year, month, includeFees())
            }
            totalsFlow.map { totals ->
                totals.mapNotNull { ct ->
                    val cat = try { TransactionCategory.valueOf(ct.category) } catch (_: Exception) { null }
                    cat?.let { ChartCategory(it, ct.total) }
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
                if (categoryName == TransactionCategory.FEES.name) repository.getWithFeesForRange(start, end)
                else repository.getByCategoryForRange(categoryName, start, end)
            }
            Period.MONTH ->
                if (categoryName == TransactionCategory.FEES.name) repository.getWithFees(year, month)
                else repository.getByCategory(categoryName, year, month)
        }
    }

    private val categorySheet = MutableStateFlow<String?>(null)

    /**
     * The open category list and its transactions. It outlives the Home screen while a transaction
     * opened from the list is showing, so going back draws the list on the first frame.
     */
    val categorySheetTransactions: StateFlow<Pair<String, List<com.ayko.trackitt.domain.model.Transaction>>?> =
        categorySheet
            .flatMapLatest { name ->
                if (name == null) flowOf(null) else getCategoryTransactions(name).map { name to it }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun showCategorySheet(categoryName: String?) { categorySheet.value = categoryName }

    /** Wallets and banks heard from in the last few months, with their latest reported balance. */
    val balances: Flow<List<com.ayko.trackitt.service.accounts.AccountBalance>> =
        repository.observeAccountBalances().map { accounts ->
            val activeSince = System.currentTimeMillis() - ACTIVE_ACCOUNT_MILLIS
            accounts.filter { it.asOf >= activeSince }
        }

    val recentGapsSince: Long get() = System.currentTimeMillis() - RECENT_GAP_MILLIS

    /**
     * Categorises transactions waiting for on-device AI. Android only runs Gemini Nano for the app on
     * screen, so the Home screen calls this while it's showing; leaving cancels it, and it resumes later.
     */
    suspend fun classifyPending() = repository.retryPendingClassifications()

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
        if (granted) SmsBackfillWorker.resumeIfUnfinished(context)
    }

    private companion object {
        const val KEY_REVIEWED_AUTO_TRANSFERS = "reviewed_auto_transfers_v1"
        const val KEY_DICTIONARY_VERSION = "dictionary_version"

        /** Raise when the keyword dictionary changes, so past imports are categorised again. */
        const val DICTIONARY_VERSION = 2
        const val ACTIVE_ACCOUNT_MILLIS = 120L * 24 * 60 * 60 * 1000
        const val RECENT_GAP_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
