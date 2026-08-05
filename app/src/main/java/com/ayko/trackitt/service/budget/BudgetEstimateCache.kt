package com.ayko.trackitt.service.budget

import android.content.SharedPreferences

class BudgetEstimateCache(
    private val prefs: SharedPreferences,
) {

    fun load(snapshot: BudgetEstimateSnapshot): BudgetEstimate? {
        val key = monthKey(snapshot)
        val fingerprint = prefs.getString("${key}_fingerprint", null) ?: return null
        if (fingerprint != snapshot.fingerprint) return null

        val summary = prefs.getString("${key}_summary", null) ?: return null
        val amount = prefs.getFloat("${key}_amount", -1f).toDouble()
        if (amount <= 0.0) return null

        val origin = prefs.getString("${key}_origin", null)
            ?.let { runCatching { BudgetEstimateOrigin.valueOf(it) }.getOrNull() }
            ?: return null

        return BudgetEstimate(
            amount = amount,
            summary = summary,
            origin = origin,
            sources = snapshot.sources,
        )
    }

    fun save(snapshot: BudgetEstimateSnapshot, estimate: BudgetEstimate) {
        if (estimate.origin == BudgetEstimateOrigin.RULES) return

        val key = monthKey(snapshot)
        prefs.edit()
            .putString("${key}_fingerprint", snapshot.fingerprint)
            .putString("${key}_summary", estimate.summary)
            .putFloat("${key}_amount", estimate.amount.toFloat())
            .putString("${key}_origin", estimate.origin.name)
            .apply()
    }

    private fun monthKey(snapshot: BudgetEstimateSnapshot): String {
        return "budget_estimate_v4_${snapshot.year}_${snapshot.month.toString().padStart(2, '0')}"
    }
}
