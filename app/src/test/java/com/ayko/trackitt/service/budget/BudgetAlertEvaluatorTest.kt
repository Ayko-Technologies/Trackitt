package com.ayko.trackitt.service.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetAlertEvaluatorTest {

    private val evaluator = BudgetAlertEvaluator()

    @Test
    fun calculates_previous_total_for_insert() {
        val key = BudgetAlertKey(year = 2026, month = 5, category = "FOOD")
        val after = BudgetAlertContribution(key = key, amount = 90.0)

        val previous = evaluator.previousTotalForKey(
            key = key,
            currentTotal = 510.0,
            before = null,
            after = after,
        )

        assertEquals(420.0, previous, 0.001)
    }

    @Test
    fun calculates_previous_total_for_same_category_amount_change() {
        val key = BudgetAlertKey(year = 2026, month = 5, category = "FOOD")
        val before = BudgetAlertContribution(key = key, amount = 100.0)
        val after = BudgetAlertContribution(key = key, amount = 180.0)

        val previous = evaluator.previousTotalForKey(
            key = key,
            currentTotal = 600.0,
            before = before,
            after = after,
        )

        assertEquals(520.0, previous, 0.001)
    }

    @Test
    fun calculates_previous_total_for_category_move() {
        val oldKey = BudgetAlertKey(year = 2026, month = 5, category = "OTHER")
        val newKey = BudgetAlertKey(year = 2026, month = 5, category = "POCKET_MONEY")
        val before = BudgetAlertContribution(key = oldKey, amount = 400.0)
        val after = BudgetAlertContribution(key = newKey, amount = 400.0)

        val oldPrevious = evaluator.previousTotalForKey(
            key = oldKey,
            currentTotal = 100.0,
            before = before,
            after = after,
        )
        val newPrevious = evaluator.previousTotalForKey(
            key = newKey,
            currentTotal = 950.0,
            before = before,
            after = after,
        )

        assertEquals(500.0, oldPrevious, 0.001)
        assertEquals(550.0, newPrevious, 0.001)
    }

    @Test
    fun only_alerts_when_crossing_over_budget() {
        assertTrue(evaluator.crossedBudget(previousTotal = 900.0, currentTotal = 1100.0, budgetAmount = 1000.0))
        assertFalse(evaluator.crossedBudget(previousTotal = 1100.0, currentTotal = 1200.0, budgetAmount = 1000.0))
        assertFalse(evaluator.crossedBudget(previousTotal = 700.0, currentTotal = 900.0, budgetAmount = 1000.0))
    }
}
