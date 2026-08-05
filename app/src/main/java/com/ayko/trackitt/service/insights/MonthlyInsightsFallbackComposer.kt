package com.ayko.trackitt.service.insights

import java.util.Locale
import kotlin.math.roundToInt

class MonthlyInsightsFallbackComposer {

    fun compose(snapshot: MonthlyInsightsSnapshot): InsightNarrative {
        if (snapshot.expenseCount == 0 && snapshot.incomeCount == 0) {
            return InsightNarrative(
                headline = "No transactions yet for ${snapshot.monthLabel}",
                whereMoneyWent = "Import or add transactions for ${snapshot.monthLabel} to unlock spending insights.",
                cutBack = "There is nothing to cut yet because the month has no recorded spending.",
                saveMore = "Once transactions appear, this screen will highlight your biggest spending drivers automatically.",
                source = InsightSource.RULES,
            )
        }

        val topCategory = snapshot.topCategories.firstOrNull()
        val secondCategory = snapshot.topCategories.getOrNull(1)
        val thirdCategory = snapshot.topCategories.getOrNull(2)
        val topMerchant = snapshot.topMerchants.firstOrNull()
        val topFeeMerchant = snapshot.topFeeMerchants.firstOrNull()

        val topCategoriesLine = listOfNotNull(topCategory, secondCategory, thirdCategory)
            .joinToString(", ") { "${it.category.label} at ${money(it.amount)} (${percent(it.shareOfSpend)})" }

        val whereMoneyWent = buildString {
            append("You spent ${money(snapshot.spend)}, paid ${money(snapshot.fees)} in fees, brought in ${money(snapshot.income)}, and closed ${snapshot.monthLabel} at ${signedMoney(snapshot.net)}.")
            if (topCategoriesLine.isNotBlank()) {
                append(" Most of that spend sat in $topCategoriesLine.")
            }
            if (topMerchant != null) {
                val noteHint = topMerchant.notes.firstOrNull()?.let { " for \"$it\"" } ?: ""
                append(" The single biggest outflow was ${topMerchant.merchant} at ${money(topMerchant.amount)}$noteHint.")
            }
        }

        val cutBack = when {
            topCategory != null && topMerchant != null && snapshot.uncategorizedSpend > 0.0 && snapshot.uncategorizedSpend / snapshot.spend >= 0.15 ->
                "${topCategory.category.label} is your biggest lever at ${money(topCategory.amount)}, and ${topMerchant.merchant} is the first merchant worth questioning. You also still have ${money(snapshot.uncategorizedSpend)} sitting in Other, which means part of the month is still too vague to cut confidently."
            topCategory != null && topMerchant != null ->
                "${topCategory.category.label} is your biggest lever at ${money(topCategory.amount)}, and ${topMerchant.merchant} is the first merchant worth questioning because it absorbed ${money(topMerchant.amount)} by itself."
            topCategory != null ->
                "${topCategory.category.label} is your biggest lever at ${money(topCategory.amount)}, so that is the first bucket to cap next month."
            else ->
                "Your spending is spread out enough that cuts will need to come from several smaller decisions rather than one obvious line item."
        }

        val saveMore = when {
            snapshot.net < 0.0 && topFeeMerchant != null && topCategory != null && secondCategory != null ->
                "Because you spent more than you earned, saving next month starts with putting hard budgets on ${topCategory.category.label} and ${secondCategory.category.label}. Fees also cost ${money(snapshot.fees)}, so move larger sends like ${topFeeMerchant.merchant} to cheaper rails where possible."
            snapshot.net < 0.0 && topCategory != null && secondCategory != null ->
                "Because you spent more than you earned, saving next month starts with putting hard budgets on ${topCategory.category.label} and ${secondCategory.category.label}. Those two buckets are carrying most of the month."
            topFeeMerchant != null ->
                "You can save without changing your lifestyle much by cutting transfer friction first. Fees cost ${money(snapshot.fees)}, and ${topFeeMerchant.merchant} was the biggest fee driver."
            else ->
                "Turn this month into next month’s plan by setting budgets around your top categories before the month starts, rather than reacting after the money is gone."
        }

        return InsightNarrative(
            headline = headline(snapshot, topCategory, topMerchant),
            whereMoneyWent = whereMoneyWent,
            cutBack = cutBack,
            saveMore = saveMore,
            source = InsightSource.RULES,
        )
    }

    private fun headline(
        snapshot: MonthlyInsightsSnapshot,
        topCategory: InsightCategoryBreakdown?,
        topMerchant: InsightMerchantBreakdown?,
    ): String {
        return when {
            topCategory != null && topCategory.shareOfSpend >= 0.35 ->
                "${topCategory.category.label} dominated your spending in ${snapshot.monthLabel}"
            topMerchant != null && topMerchant.shareOfSpend >= 0.2 ->
                "${topMerchant.merchant} was your biggest single drain in ${snapshot.monthLabel}"
            snapshot.net < 0.0 ->
                "You spent more than you brought in during ${snapshot.monthLabel}"
            else ->
                "${snapshot.monthLabel} was concentrated in a few main spending buckets"
        }
    }

    private fun money(value: Double): String {
        return "GH₵${String.format(Locale.US, "%,.2f", value)}"
    }

    private fun signedMoney(value: Double): String {
        val prefix = if (value >= 0.0) "+" else "-"
        return "$prefix${money(kotlin.math.abs(value))}"
    }

    private fun percent(value: Double): String {
        return "${(value * 100).roundToInt()}%"
    }
}
