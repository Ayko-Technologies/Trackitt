package com.ayko.trackitt.service.budget

enum class BudgetEstimateSourceType {
    SPENDING_HISTORY,
    SAVED_BUDGET,
}

data class BudgetEstimateSource(
    val type: BudgetEstimateSourceType,
    val label: String,
    val amount: Double,
    val supportingMonths: Int? = null,
)

data class BudgetEstimateSnapshot(
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val fingerprint: String,
    val sources: List<BudgetEstimateSource>,
)

enum class BudgetEstimateOrigin {
    RULES,
    ON_DEVICE_AI,
}

data class BudgetEstimate(
    val amount: Double,
    val summary: String,
    val origin: BudgetEstimateOrigin,
    val sources: List<BudgetEstimateSource>,
)
