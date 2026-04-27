package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    suspend fun categorize(
        merchant: String,
        reference: String? = null,
        note: String? = null,
    ): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override suspend fun categorize(
        merchant: String,
        reference: String?,
        note: String?,
    ): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        val userMapping = userLookup(normalized)
        if (userMapping != null) return userMapping

        if (note != null) {
            val normalizedNote = note.lowercase().trim()
            for ((keyword, category) in REFERENCE_DICTIONARY) {
                if (normalizedNote.contains(keyword)) return category
            }
        }

        if (reference != null) {
            val normalizedRef = reference.lowercase().trim()
            for ((keyword, category) in REFERENCE_DICTIONARY) {
                if (normalizedRef.contains(keyword)) return category
            }
        }

        for ((keyword, category) in MERCHANT_DICTIONARY) {
            if (normalized.contains(keyword)) return category
        }

        return TransactionCategory.OTHER
    }
}
