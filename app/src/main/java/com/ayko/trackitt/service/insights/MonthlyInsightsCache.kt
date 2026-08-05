package com.ayko.trackitt.service.insights

import android.content.SharedPreferences

class MonthlyInsightsCache(
    private val prefs: SharedPreferences,
) {

    fun load(snapshot: MonthlyInsightsSnapshot): InsightNarrative? {
        val monthKey = monthKey(snapshot)
        val fingerprint = prefs.getString("${monthKey}_fingerprint", null) ?: return null
        if (fingerprint != snapshot.fingerprint) return null

        val sourceName = prefs.getString("${monthKey}_source", null) ?: return null
        val source = runCatching { InsightSource.valueOf(sourceName) }.getOrNull() ?: return null
        if (source == InsightSource.RULES) return null

        val headline = prefs.getString("${monthKey}_headline", null) ?: return null
        val where = prefs.getString("${monthKey}_where", null) ?: return null
        val cut = prefs.getString("${monthKey}_cut", null) ?: return null
        val save = prefs.getString("${monthKey}_save", null) ?: return null

        return InsightNarrative(
            headline = headline,
            whereMoneyWent = where,
            cutBack = cut,
            saveMore = save,
            source = source,
        )
    }

    fun save(snapshot: MonthlyInsightsSnapshot, narrative: InsightNarrative) {
        if (narrative.source == InsightSource.RULES) return

        val monthKey = monthKey(snapshot)
        prefs.edit()
            .putString("${monthKey}_fingerprint", snapshot.fingerprint)
            .putString("${monthKey}_source", narrative.source.name)
            .putString("${monthKey}_headline", narrative.headline)
            .putString("${monthKey}_where", narrative.whereMoneyWent)
            .putString("${monthKey}_cut", narrative.cutBack)
            .putString("${monthKey}_save", narrative.saveMore)
            .apply()
    }

    private fun monthKey(snapshot: MonthlyInsightsSnapshot): String {
        return "insights_${snapshot.year}_${snapshot.month.toString().padStart(2, '0')}"
    }
}
