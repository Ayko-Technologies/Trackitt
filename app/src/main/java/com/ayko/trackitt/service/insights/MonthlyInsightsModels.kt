package com.ayko.trackitt.service.insights

import com.ayko.trackitt.domain.model.TransactionCategory

data class InsightCategoryBreakdown(
    val category: TransactionCategory,
    val amount: Double,
    val fee: Double,
    val shareOfSpend: Double,
)

data class InsightMerchantBreakdown(
    val merchant: String,
    val amount: Double,
    val fee: Double,
    val shareOfSpend: Double,
    val categories: List<TransactionCategory>,
    val notes: List<String>,
)

data class MonthlyInsightsSnapshot(
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val fingerprint: String,
    val transactionCount: Int,
    val expenseCount: Int,
    val incomeCount: Int,
    val spend: Double,
    val fees: Double,
    val income: Double,
    val net: Double,
    val uncategorizedSpend: Double,
    val topCategories: List<InsightCategoryBreakdown>,
    val topMerchants: List<InsightMerchantBreakdown>,
    val topFeeMerchants: List<InsightMerchantBreakdown>,
)

enum class InsightSource {
    RULES,
    ON_DEVICE_AI,
}

data class InsightNarrative(
    val headline: String,
    val whereMoneyWent: String,
    val cutBack: String,
    val saveMore: String,
    val source: InsightSource,
)

data class InsightNarrativeDraft(
    val headline: String? = null,
    val whereMoneyWent: String? = null,
    val cutBack: String? = null,
    val saveMore: String? = null,
) {
    fun hasAnyContent(): Boolean {
        return !headline.isNullOrBlank() ||
            !whereMoneyWent.isNullOrBlank() ||
            !cutBack.isNullOrBlank() ||
            !saveMore.isNullOrBlank()
    }

    fun mergeInto(fallback: InsightNarrative, source: InsightSource): InsightNarrative {
        return fallback.copy(
            headline = headline ?: fallback.headline,
            whereMoneyWent = whereMoneyWent ?: fallback.whereMoneyWent,
            cutBack = cutBack ?: fallback.cutBack,
            saveMore = saveMore ?: fallback.saveMore,
            source = source,
        )
    }
}
