package com.ayko.trackitt.service.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEstimatePromptTest {

    @Test
    fun builds_and_parses_budget_estimate_response() {
        val snapshot = BudgetEstimateSnapshot(
            year = 2026,
            month = 5,
            monthLabel = "May 2026",
            fingerprint = "abc",
            sources = listOf(
                BudgetEstimateSource(
                    type = BudgetEstimateSourceType.SPENDING_HISTORY,
                    label = "your typical monthly spending over the last 4 completed months",
                    amount = 1800.0,
                    supportingMonths = 4,
                ),
                BudgetEstimateSource(
                    type = BudgetEstimateSourceType.SAVED_BUDGET,
                    label = "this month's saved budgets",
                    amount = 1600.0,
                ),
            ),
        )

        val prompt = BudgetEstimatePrompt.build(snapshot)

        assertTrue(prompt.contains("your typical monthly spending over the last 4 completed months"))
        assertTrue(prompt.contains("this month's saved budgets"))
        assertEquals(1750.0, BudgetEstimatePrompt.parseAmount("AMOUNT: 1,750") ?: 0.0, 0.001)
    }
}
