package com.ayko.trackitt.ui.screen.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository
    private val prefs = application.getSharedPreferences("search", Context.MODE_PRIVATE)

    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: Flow<List<Transaction>> = query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.length < 2) flowOf(emptyList())
            else repository.searchByMerchant(q)
        }

    val recentSearches = MutableStateFlow(loadRecent())

    fun saveSearch(term: String) {
        if (term.isBlank()) return
        val recent = loadRecent().toMutableList()
        recent.remove(term)
        recent.add(0, term)
        val trimmed = recent.take(5)
        prefs.edit().putStringSet("recent", trimmed.toSet()).apply()
        recentSearches.value = trimmed
    }

    private fun loadRecent(): List<String> {
        return prefs.getStringSet("recent", emptySet())?.toList() ?: emptyList()
    }
}
