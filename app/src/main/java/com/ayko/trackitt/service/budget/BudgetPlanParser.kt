package com.ayko.trackitt.service.budget

import com.ayko.trackitt.domain.model.TransactionCategory

data class BudgetPlanLine(
    val label: String,
    val amount: Double,
    val category: TransactionCategory,
    val rawLine: String,
)

data class BudgetPlanParseResult(
    val parsedLines: List<BudgetPlanLine>,
    val invalidLines: List<String>,
) {
    val groupedBudgets: List<Pair<TransactionCategory, Double>>
        get() = groupBudgetLines(parsedLines)
}

fun groupBudgetLines(lines: List<BudgetPlanLine>): List<Pair<TransactionCategory, Double>> {
    return lines
            .groupBy { it.category }
            .map { (category, lines) -> category to lines.sumOf { it.amount } }
            .sortedByDescending { it.second }
}

class BudgetPlanParser {

    fun parse(input: String): BudgetPlanParseResult {
        val parsedLines = mutableListOf<BudgetPlanLine>()
        val invalidLines = mutableListOf<String>()

        input.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                parseLine(line)?.let(parsedLines::add) ?: invalidLines.add(line)
            }

        return BudgetPlanParseResult(
            parsedLines = parsedLines,
            invalidLines = invalidLines,
        )
    }

    private fun parseLine(line: String): BudgetPlanLine? {
        val separatorIndex = line.indexOf('~').takeIf { it >= 0 }
            ?: line.indexOf(':').takeIf { it >= 0 }
            ?: return null

        val label = line.substring(0, separatorIndex).trim()
        val expression = line.substring(separatorIndex + 1).trim()
        if (label.isEmpty() || expression.isEmpty()) return null

        val amount = evaluateExpression(expression) ?: return null
        if (amount <= 0.0) return null

        return BudgetPlanLine(
            label = label,
            amount = amount,
            category = categorize(label),
            rawLine = line,
        )
    }

    private fun categorize(label: String): TransactionCategory {
        val normalized = label.lowercase()
            .replace("&", " ")
            .replace("/", " ")
            .replace(".", " ")
            .trim()

        return when {
            normalized.containsAny("offering", "tithe", "seed", "church") -> TransactionCategory.CHURCH
            normalized.containsAny("pocket money", "allowance", "upkeep", "kids") -> TransactionCategory.POCKET_MONEY
            normalized.containsAny("glasses", "glass", "hospital", "clinic", "pharmacy", "health") -> TransactionCategory.HEALTH
            normalized.containsAny("transport", "transportation", "uber", "bolt", "taxi", "trotro", "fuel") -> TransactionCategory.TRANSPORT
            normalized.containsAny("electricity", "fiber", "fibre", "internet", "wifi", "claude", "chat gpt", "chatgpt", "light", "ecg") -> TransactionCategory.BILLS
            normalized.containsAny("grocries", "groceries", "grocery", "market") -> TransactionCategory.GROCERIES
            normalized.containsAny("pure water", "snacks", "snack") -> TransactionCategory.SNACKS
            normalized.containsAny("ups", "laptop", "charger", "phone", "tablet", "screen", "tv", "electronics") -> TransactionCategory.ELECTRONICS
            normalized.containsAny("rent", "house", "water") -> TransactionCategory.HOUSE
            normalized.containsAny("date", "food", "restaurant", "lunch", "dinner") -> TransactionCategory.FOOD
            normalized.containsAny("misc", "miscellaneous", "other") -> TransactionCategory.OTHER
            else -> TransactionCategory.OTHER
        }
    }

    private fun evaluateExpression(expression: String): Double? {
        val sanitized = expression
            .replace(",", "")
            .replace("×", "*")
            .replace("x", "*", ignoreCase = true)
            .replace(Regex("\\s+"), "")

        if (sanitized.isEmpty()) return null

        val additionParts = sanitized.split("+")
        var total = 0.0
        for (part in additionParts) {
            if (part.isBlank()) return null
            val multiplicationParts = part.split("*")
            var value = 1.0
            for (factor in multiplicationParts) {
                val number = factor.toDoubleOrNull() ?: return null
                value *= number
            }
            total += value
        }
        return total
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it) }
    }
}
