package com.ayko.trackitt.service.budget

import com.ayko.trackitt.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetPlanParserTest {

    private val parser = BudgetPlanParser()

    @Test
    fun parses_budget_plan_with_math_and_aliases() {
        val result = parser.parse(
            """
            Offering ~ 70 × 4
            Date ~ 300
            Glasses ~ 700
            Tithe ~ 400
            Ashiley ~ 800 + 700
            Pure Water ~ 120
            Water ~ 500
            Electricity ~ 1,500
            Fiber ~ 350
            Claude ~ 200
            Chat GPT ~ 1,000
            Prissy ~ 400
            Grocries & Snacks ~ 1,700
            UPS ~ 3,500
            Transportation ~ 350
            Miscellaneous ~ 700
            """.trimIndent()
        )

        val grouped = result.groupedBudgets.toMap()

        assertTrue(result.invalidLines.isEmpty())
        assertEquals(680.0, grouped[TransactionCategory.CHURCH] ?: 0.0, 0.001)
        assertEquals(300.0, grouped[TransactionCategory.FOOD] ?: 0.0, 0.001)
        assertEquals(700.0, grouped[TransactionCategory.HEALTH] ?: 0.0, 0.001)
        assertEquals(1900.0, grouped[TransactionCategory.POCKET_MONEY] ?: 0.0, 0.001)
        assertEquals(120.0, grouped[TransactionCategory.SNACKS] ?: 0.0, 0.001)
        assertEquals(500.0, grouped[TransactionCategory.HOUSE] ?: 0.0, 0.001)
        assertEquals(3050.0, grouped[TransactionCategory.BILLS] ?: 0.0, 0.001)
        assertEquals(1700.0, grouped[TransactionCategory.GROCERIES] ?: 0.0, 0.001)
        assertEquals(350.0, grouped[TransactionCategory.TRANSPORT] ?: 0.0, 0.001)
        assertEquals(3500.0, grouped[TransactionCategory.ELECTRONICS] ?: 0.0, 0.001)
        assertEquals(700.0, grouped[TransactionCategory.OTHER] ?: 0.0, 0.001)
    }

    @Test
    fun invalid_lines_are_reported() {
        val result = parser.parse(
            """
            Good line ~ 50
            Broken line ~ seven hundred
            Missing separator
            """.trimIndent()
        )

        assertEquals(1, result.parsedLines.size)
        assertEquals(2, result.invalidLines.size)
    }
}
