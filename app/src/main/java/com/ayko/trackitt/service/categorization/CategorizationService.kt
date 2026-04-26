package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    fun categorize(merchant: String, reference: String? = null): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override fun categorize(merchant: String, reference: String?): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        val userMapping = kotlinx.coroutines.runBlocking { userLookup(normalized) }
        if (userMapping != null) return userMapping

        // Check reference field first — often tells you what the payment was for
        if (reference != null) {
            val normalizedRef = reference.lowercase().trim()
            for ((keyword, category) in REFERENCE_DICTIONARY) {
                if (normalizedRef.contains(keyword)) return category
            }
        }

        // Then check merchant name
        for ((keyword, category) in MERCHANT_DICTIONARY) {
            if (normalized.contains(keyword)) return category
        }

        return TransactionCategory.OTHER
    }
}
