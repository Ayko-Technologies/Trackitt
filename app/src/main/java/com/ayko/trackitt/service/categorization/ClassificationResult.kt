package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

sealed class ClassificationResult {
    data class Classified(val category: TransactionCategory) : ClassificationResult()
    data object Unavailable : ClassificationResult()
}
