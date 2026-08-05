package com.ayko.trackitt.service.budget

data class BudgetAlertKey(
    val year: Int,
    val month: Int,
    val category: String,
)

data class BudgetAlertContribution(
    val key: BudgetAlertKey,
    val amount: Double,
)

class BudgetAlertEvaluator {

    fun impactedKeys(
        before: BudgetAlertContribution?,
        after: BudgetAlertContribution?,
    ): Set<BudgetAlertKey> {
        return buildSet {
            before?.key?.let(::add)
            after?.key?.let(::add)
        }
    }

    fun previousTotalForKey(
        key: BudgetAlertKey,
        currentTotal: Double,
        before: BudgetAlertContribution?,
        after: BudgetAlertContribution?,
    ): Double {
        return when {
            before?.key == key && after?.key == key -> currentTotal - after.amount + before.amount
            before?.key == key -> currentTotal + before.amount
            after?.key == key -> currentTotal - after.amount
            else -> currentTotal
        }
    }

    fun crossedBudget(
        previousTotal: Double,
        currentTotal: Double,
        budgetAmount: Double,
    ): Boolean {
        return previousTotal <= budgetAmount && currentTotal > budgetAmount
    }
}
