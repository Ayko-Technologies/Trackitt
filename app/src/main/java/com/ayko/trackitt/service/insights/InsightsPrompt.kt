package com.ayko.trackitt.service.insights

import java.util.Locale
import kotlin.math.roundToInt

object InsightsPrompt {

    fun buildHeadlineAndWherePrompt(snapshot: MonthlyInsightsSnapshot): String {
        return """
            You are a sharp personal finance analyst for a Ghana-based expense tracker.
            Use only the data below. Do not invent merchants, categories, causes, or advice not grounded in the numbers.

            Return exactly two lines in this format:
            HEADLINE: one short sentence
            WHERE: one short paragraph, 2 to 4 sentences

            Rules:
            - Mention exact GH₵ amounts when helpful.
            - Write like a human financial coach, not a dashboard.
            - Do not return bullets, lists, or fragments.
            - Focus only on what happened this month and where the money went.

            ${buildContext(snapshot)}
        """.trimIndent()
    }

    fun buildCutPrompt(snapshot: MonthlyInsightsSnapshot, current: InsightNarrativeDraft): String {
        return """
            You are a sharp personal finance analyst for a Ghana-based expense tracker.
            Use only the data below. Do not invent merchants, categories, causes, or advice not grounded in the numbers.

            Return exactly one line in this format:
            CUT: one short paragraph, 2 to 4 sentences

            Rules:
            - Write like a human financial coach.
            - Be specific about the highest-value cuts first.
            - If "Other" is high, say the data still needs cleanup before cutting blindly.
            - Do not repeat the heading label inside the paragraph.

            Existing draft:
            HEADLINE: ${current.headline ?: "none"}
            WHERE: ${current.whereMoneyWent ?: "none"}

            ${buildContext(snapshot)}
        """.trimIndent()
    }

    fun buildSavePrompt(snapshot: MonthlyInsightsSnapshot, current: InsightNarrativeDraft): String {
        return """
            You are a sharp personal finance analyst for a Ghana-based expense tracker.
            Use only the data below. Do not invent merchants, categories, causes, or advice not grounded in the numbers.

            Return exactly one line in this format:
            SAVE: one short paragraph, 2 to 4 sentences

            Rules:
            - Write like a human financial coach.
            - Focus on what the user should do next month to save more.
            - Mention budgets, fee reduction, or reducing concentration when supported by the data.
            - Do not repeat the heading label inside the paragraph.

            Existing draft:
            HEADLINE: ${current.headline ?: "none"}
            WHERE: ${current.whereMoneyWent ?: "none"}
            CUT: ${current.cutBack ?: "none"}

            ${buildContext(snapshot)}
        """.trimIndent()
    }

    fun parseHeadlineAndWhere(text: String): InsightNarrativeDraft? {
        val lines = normalizedLines(text)
        val headline = parsePrefixedValue(lines, "HEADLINE") ?: return null
        val where = parsePrefixedValue(lines, "WHERE") ?: return null
        return InsightNarrativeDraft(
            headline = headline,
            whereMoneyWent = where,
        )
    }

    fun parseCut(text: String): InsightNarrativeDraft? {
        val lines = normalizedLines(text)
        val cut = parsePrefixedValue(lines, "CUT") ?: return null
        return InsightNarrativeDraft(cutBack = cut)
    }

    fun parseSave(text: String): InsightNarrativeDraft? {
        val lines = normalizedLines(text)
        val save = parsePrefixedValue(lines, "SAVE") ?: return null
        return InsightNarrativeDraft(saveMore = save)
    }

    private fun buildContext(snapshot: MonthlyInsightsSnapshot): String {
        val topCategories = snapshot.topCategories.take(5).joinToString("\n") { category ->
            "- ${category.category.label}: ${money(category.amount)} (${percent(category.shareOfSpend)})"
        }
        val topMerchants = snapshot.topMerchants.take(6).joinToString("\n") { merchant ->
            val noteHint = merchant.notes.take(2).joinToString(", ").ifBlank { "none" }
            "- ${merchant.merchant}: ${money(merchant.amount)} (${percent(merchant.shareOfSpend)}), notes: $noteHint"
        }
        val topFeeMerchants = snapshot.topFeeMerchants.take(3).joinToString("\n") { merchant ->
            "- ${merchant.merchant}: fees ${money(merchant.fee)}, amount ${money(merchant.amount)}"
        }.ifBlank { "- none" }

        return """
            Month: ${snapshot.monthLabel}
            Spend: ${money(snapshot.spend)}
            Fees: ${money(snapshot.fees)}
            Income: ${money(snapshot.income)}
            Net: ${signedMoney(snapshot.net)}
            Uncategorized Other spend: ${money(snapshot.uncategorizedSpend)}

            Top categories:
            $topCategories

            Top merchants:
            $topMerchants

            Top fee merchants:
            $topFeeMerchants
        """.trimIndent()
    }

    private fun normalizedLines(text: String): List<String> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun parsePrefixedValue(lines: List<String>, prefix: String): String? {
        return lines.firstOrNull { it.startsWith("$prefix:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
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
