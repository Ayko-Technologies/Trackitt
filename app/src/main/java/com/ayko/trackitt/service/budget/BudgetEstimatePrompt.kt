package com.ayko.trackitt.service.budget

import java.util.Locale

object BudgetEstimatePrompt {

    fun build(snapshot: BudgetEstimateSnapshot): String {
        val sources = snapshot.sources.joinToString("\n") { source ->
            "- ${source.label}: ${money(source.amount)}"
        }

        return """
            You estimate a practical starting monthly budget for a Ghana expense tracker.
            Use only the source amounts below.

            Return exactly one line:
            AMOUNT: number only

            Rules:
            - If two sources exist, choose one practical estimate between them.
            - If only one source exists, stay close to it and round only if it helps planning.
            - Do not invent new sources or categories.
            - Do not return bullets, ranges, explanations, or currency symbols after AMOUNT.

            Month: ${snapshot.monthLabel}
            Sources:
            $sources
        """.trimIndent()
    }

    fun parseAmount(text: String): Double? {
        val value = prefixedValue(text, "AMOUNT") ?: return null
        return value.replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun prefixedValue(text: String, prefix: String): String? {
        return text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$prefix:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
    }

    private fun money(amount: Double): String {
        return "GH₵${String.format(Locale.US, "%,.2f", amount)}"
    }
}
