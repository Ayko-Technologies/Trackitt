package com.ayko.trackitt.ui.screen.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    /** The name the user gave this transaction's phone number, if it has one. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val payeeName: Flow<String?> = transaction.flatMapLatest { tx ->
        tx?.payeeNumber?.let(repository::observePayeeName) ?: flowOf(null)
    }

    fun namePayee(number: String, name: String) {
        viewModelScope.launch { repository.namePayee(number, name) }
    }

    fun updateMerchant(merchant: String) {
        viewModelScope.launch {
            repository.updateMerchant(transactionId, merchant)
        }
    }

    fun updateNote(note: String?, merchant: String = "", reference: String? = null) {
        viewModelScope.launch {
            repository.updateNote(transactionId, note, merchant, reference)
        }
    }

    fun updateAmount(amount: Double) {
        viewModelScope.launch {
            repository.updateAmount(transactionId, amount)
        }
    }

    fun setExcluded(excluded: Boolean) {
        viewModelScope.launch { repository.setExcluded(transactionId, excluded) }
    }

    fun markAsTransfer() {
        viewModelScope.launch { repository.markAsTransfer(transactionId) }
    }

    fun unlinkOwnTransfer() {
        viewModelScope.launch {
            repository.unlinkOwnTransfer(transactionId)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(transactionId)
            _deleted.value = true
        }
    }
}
