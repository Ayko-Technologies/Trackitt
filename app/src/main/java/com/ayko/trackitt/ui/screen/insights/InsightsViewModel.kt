package com.ayko.trackitt.ui.screen.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.service.insights.InsightNarrative
import com.ayko.trackitt.service.insights.MonthlyInsightsSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isLoading: Boolean = true,
    val snapshot: MonthlyInsightsSnapshot? = null,
    val narrative: InsightNarrative? = null,
    val errorMessage: String? = null,
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as TrackittApplication).container
    private val repository = container.repository
    private val insightsService = container.insightsService
    private val displayPrefs = application.getSharedPreferences("trackitt_display", 0)

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    private var loadJob: Job? = null
    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

    fun setMonth(year: Int, month: Int, force: Boolean = false) {
        if (!force && selectedYear == year && selectedMonth == month && loadJob != null) return

        selectedYear = year
        selectedMonth = month
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getByMonth(year, month)
                .distinctUntilChanged()
                .collect { transactions ->
                val snapshot = insightsService.analyze(
                    year = year,
                    month = month,
                    transactions = transactions,
                    excludeTransfers = displayPrefs.getBoolean("exclude_transfers", false),
                )
                val fallback = insightsService.fallbackNarrative(snapshot)
                _uiState.value = InsightsUiState(
                    isLoading = true,
                    snapshot = snapshot,
                    narrative = fallback,
                )

                val narrative = insightsService.generateNarrative(snapshot)
                _uiState.value = InsightsUiState(
                    isLoading = false,
                    snapshot = snapshot,
                    narrative = narrative,
                )
            }
        }
    }

    fun refresh() {
        val year = selectedYear ?: return
        val month = selectedMonth ?: return
        setMonth(year, month, force = true)
    }
}
