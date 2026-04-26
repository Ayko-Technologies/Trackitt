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
        val amountValue = evaluateExpression(amount.value) ?: return
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

    private fun evaluateExpression(expr: String): Double? {
        if (expr.isEmpty()) return null
        val clean = expr.trimEnd('+', '−', '×', '÷')
        if (clean.isEmpty()) return null

        val simple = clean.toDoubleOrNull()
        if (simple != null) return simple

        val tokens = mutableListOf<Any>()
        var current = ""
        for (ch in clean) {
            if (ch == '+' || ch == '−' || ch == '×' || ch == '÷') {
                val num = current.toDoubleOrNull() ?: return null
                tokens.add(num)
                tokens.add(ch)
                current = ""
            } else {
                current += ch
            }
        }
        val lastNum = current.toDoubleOrNull() ?: return null
        tokens.add(lastNum)

        val simplified = mutableListOf<Any>()
        var i = 0
        while (i < tokens.size) {
            if (i + 2 < tokens.size && (tokens[i + 1] == '×' || tokens[i + 1] == '÷')) {
                var acc = tokens[i] as Double
                while (i + 2 < tokens.size && (tokens[i + 1] == '×' || tokens[i + 1] == '÷')) {
                    val op = tokens[i + 1] as Char
                    val right = tokens[i + 2] as Double
                    acc = if (op == '×') acc * right else if (right != 0.0) acc / right else return null
                    i += 2
                }
                simplified.add(acc)
            } else {
                simplified.add(tokens[i])
            }
            i++
        }

        var result = simplified[0] as Double
        var j = 1
        while (j < simplified.size) {
            val op = simplified[j] as Char
            val right = simplified[j + 1] as Double
            result = if (op == '+') result + right else result - right
            j += 2
        }
        return result
    }
}
