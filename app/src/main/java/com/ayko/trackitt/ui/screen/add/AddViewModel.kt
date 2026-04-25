package com.ayko.trackitt.ui.screen.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    val amount = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<TransactionCategory?>(null)
    val note = MutableStateFlow("")
    val source = MutableStateFlow("MTN MoMo")

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun save() {
        val amountValue = amount.value.toDoubleOrNull() ?: return
        val category = selectedCategory.value ?: return
        if (amountValue <= 0) return

        viewModelScope.launch {
            repository.insertManual(
                Transaction(
                    amount = amountValue,
                    category = category,
                    merchant = note.value.ifEmpty { "Manual entry" },
                    source = source.value,
                    note = note.value.ifEmpty { null },
                    date = System.currentTimeMillis(),
                )
            )
            _saved.value = true
        }
    }
}
