package com.ayko.trackitt.ui.screen.recap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.service.recap.YearRecap
import com.ayko.trackitt.service.recap.YearRecapCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RecapState {
    data object Loading : RecapState
    data object Empty : RecapState
    data class Ready(val recap: YearRecap) : RecapState
}

class RecapViewModel(application: Application, private val year: Int) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    private val _state = MutableStateFlow<RecapState>(RecapState.Loading)
    val state: StateFlow<RecapState> = _state

    init {
        viewModelScope.launch {
            val recap = withContext(Dispatchers.Default) {
                YearRecapCalculator().recap(
                    year,
                    repository.transactionsInYear(year),
                    repository.transactionsInYear(year - 1),
                )
            }
            _state.value = recap?.let { RecapState.Ready(it) } ?: RecapState.Empty
        }
    }
}
