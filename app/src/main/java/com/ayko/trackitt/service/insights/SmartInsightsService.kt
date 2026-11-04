package com.ayko.trackitt.service.insights

import android.util.Log
import com.ayko.trackitt.domain.model.Transaction

class SmartInsightsService(
    private val analyzer: MonthlyInsightsAnalyzer,
    private val fallbackComposer: MonthlyInsightsFallbackComposer,
    private val cache: MonthlyInsightsCache,
    private val nano: NanoInsightsService,
    private val useOnDeviceAi: () -> Boolean,
) {

    fun analyze(
        year: Int,
        month: Int,
        transactions: List<Transaction>,
    ): MonthlyInsightsSnapshot {
        return analyzer.analyze(year, month, transactions)
    }

    fun fallbackNarrative(snapshot: MonthlyInsightsSnapshot): InsightNarrative {
        return fallbackComposer.compose(snapshot)
    }

    suspend fun generateNarrative(snapshot: MonthlyInsightsSnapshot): InsightNarrative {
        val fallback = fallbackNarrative(snapshot)

        val onDeviceEnabled = useOnDeviceAi()
        if (onDeviceEnabled) {
            cache.load(snapshot)?.let {
                Log.d(TAG, "Using cached ${it.source} insights for ${snapshot.monthLabel}")
                return it
            }
        }

        if (onDeviceEnabled && nano.isAvailable()) {
            nano.generate(snapshot, fallback)?.let {
                cache.save(snapshot, it)
                return it
            }
            Log.d(TAG, "Nano insight did not produce a usable narrative for ${snapshot.monthLabel}")
        } else if (onDeviceEnabled) {
            Log.d(TAG, "Nano insight unavailable for ${snapshot.monthLabel}")
        } else {
            Log.d(TAG, "On-device AI disabled for insights")
        }

        Log.d(TAG, "Falling back to deterministic insights for ${snapshot.monthLabel}")
        return fallback
    }

    companion object {
        private const val TAG = "SmartInsights"
    }
}
