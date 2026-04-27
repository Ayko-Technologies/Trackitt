package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

object CategorizationPrompt {

    private val CATEGORY_NAMES = TransactionCategory.entries
        .filter { it != TransactionCategory.INCOME }
        .joinToString(", ") { it.name }

    fun build(merchant: String, reference: String?, note: String?): String {
        return buildString {
            appendLine("Classify this transaction into exactly one category.")
            appendLine()
            appendLine("Categories: $CATEGORY_NAMES")
            appendLine()
            appendLine("Transaction:")
            appendLine("- Merchant: $merchant")
            appendLine("- Note: ${note ?: "none"}")
            appendLine("- Reference: ${reference ?: "none"}")
            appendLine()
            appendLine("Respond with only the category name, nothing else.")
        }
    }

    fun parseResponse(response: String): TransactionCategory? {
        return try {
            val cleaned = response.trim().uppercase()
            TransactionCategory.valueOf(cleaned)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
