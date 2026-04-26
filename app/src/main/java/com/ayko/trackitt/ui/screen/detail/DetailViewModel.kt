package com.ayko.trackitt.ui.screen.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    application: Application,
    private val transactionId: Long,
) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    val transaction: Flow<Transaction?> = repository.getById(transactionId)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun updateCategory(category: TransactionCategory, merchant: String) {
        viewModelScope.launch {
            repository.updateCategory(transactionId, category, merchant)
        }
    }

    fun updateMerchant(merchant: String) {
        viewModelScope.launch {
            repository.updateMerchant(transactionId, merchant)
        }
    }

    fun updateNote(note: String?) {
        viewModelScope.launch {
            repository.updateNote(transactionId, note)
        }
    }

    fun updateAmount(amount: Double) {
        viewModelScope.launch {
            repository.updateAmount(transactionId, amount)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(transactionId)
            _deleted.value = true
        }
    }
}
