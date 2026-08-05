package com.ayko.trackitt.service.insights

import android.util.Log

class CloudInsightsService {

    // TODO: Enable after Firebase setup and google-services.json are in place.
    suspend fun generate(
        snapshot: MonthlyInsightsSnapshot,
        fallback: InsightNarrative,
    ): InsightNarrative? {
        Log.d(TAG, "Cloud insights skipped — Firebase not configured for ${snapshot.monthLabel}")
        return null
    }

    companion object {
        private const val TAG = "CloudInsights"
    }
}
