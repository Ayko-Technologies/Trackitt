package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

object CategorizationPrompt {

    // Transfers between the user's own accounts are detected from both accounts' SMS, not guessed:
    // a payment to a person or a mobile money number is spending, categorised by what it was for.
    private val CATEGORY_DESCRIPTIONS = mapOf(
        TransactionCategory.FOOD to "meals, restaurants, chop bars, food delivery, drinks",
        TransactionCategory.GROCERIES to "supermarket and market shopping, foodstuff, provisions",
        TransactionCategory.TRANSPORT to "trotro, taxi, ride-hailing, motor or okada, fuel, car repairs and payments",
        TransactionCategory.BILLS to "utilities, subscriptions, internet and data, TV, insurance",
        TransactionCategory.SHOPPING to "clothes, shoes, bags and other goods",
        TransactionCategory.HOUSE to "rent, water, electricity, gas, cleaning and household items",
        TransactionCategory.BEAUTY to "barber, salon, hair, nails, creams and cosmetics",
        TransactionCategory.CHURCH to "offering, tithe, seed and church giving",
        TransactionCategory.HEALTH to "hospital, clinic, pharmacy, medicine, injections, lab tests",
        TransactionCategory.AIRTIME to "airtime and phone credit top-ups",
        TransactionCategory.POCKET_MONEY to "only an allowance or upkeep given to a child or dependant, never a payment for something",
        TransactionCategory.SNACKS to "biscuits, pies, ice cream, sweets and small treats",
        TransactionCategory.EDUCATION to "school fees, lessons, courses, books, transcripts",
        TransactionCategory.GIFTS to "presents, birthdays, weddings, funerals, tips, donations",
        TransactionCategory.ELECTRONICS to "phones, laptops, chargers, gadgets and parts",
        TransactionCategory.HOME_PROJECT to "building, renovation, AC installation, materials and workmanship",
        TransactionCategory.OTHER to "anything that fits none of the above, or when the purpose is unclear",
    )
    private val CATEGORY_LIST = CATEGORY_DESCRIPTIONS.entries.joinToString("\n") { (category, description) ->
        "- ${category.name}: $description"
    }

    fun build(merchant: String, reference: String?, note: String?): String {
        return buildString {
            appendLine("Classify this transaction into exactly one category.")
            appendLine()
            appendLine("Categories:")
            appendLine(CATEGORY_LIST)
            appendLine()
            appendLine("Transaction:")
            appendLine("- Merchant: $merchant")
            appendLine("- Note: ${note ?: "none"}")
            appendLine("- Reference: ${reference ?: "none"}")
            appendLine()
            appendLine("Use the reference and note to decide what the money was for; the merchant may just be a person or a phone number.")
            appendLine("If the reference doesn't say what the money was for, answer OTHER.")
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
