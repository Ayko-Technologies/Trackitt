package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

object CategorizationPrompt {

    // Transfers between the user's own accounts are detected from both accounts' SMS, not guessed:
    // a payment to a person or a mobile money number is spending, categorised by what it was for.
    private val CATEGORY_NAMES = TransactionCategory.entries
        .filter { it != TransactionCategory.INCOME && it != TransactionCategory.TRANSFER }
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
            appendLine("Use the reference and note to decide what the money was for; the merchant may just be a person or a phone number.")
            appendLine("Respond with only the category name, nothing else.")
        }
    }

    fun parseResponse(response: String): TransactionCategory? {
        return try {
            val cleaned = response.trim().uppercase()
            TransactionCategory.valueOf(cleaned).takeUnless { it == TransactionCategory.TRANSFER } ?: TransactionCategory.OTHER
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
