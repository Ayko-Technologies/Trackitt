package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    fun categorize(merchant: String): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override fun categorize(merchant: String): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        val userMapping = kotlinx.coroutines.runBlocking { userLookup(normalized) }
        if (userMapping != null) return userMapping

        for ((keyword, category) in MERCHANT_DICTIONARY) {
            if (normalized.contains(keyword)) return category
        }

        return TransactionCategory.OTHER
    }
}
