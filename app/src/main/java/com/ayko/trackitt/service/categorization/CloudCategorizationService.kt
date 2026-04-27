package com.ayko.trackitt.service.categorization

import android.util.Log

class CloudCategorizationService {

    // TODO: Enable after Firebase setup — uncomment firebase-ai dependency and restore Firebase implementation
    // private val model by lazy {
    //     Firebase.ai(backend = GenerativeBackend.googleAI())
    //         .generativeModel("gemini-2.5-flash-lite")
    // }

    suspend fun classify(
        merchant: String,
        reference: String?,
        note: String?,
    ): ClassificationResult {
        Log.d(TAG, "Cloud classification skipped — Firebase not configured")
        return ClassificationResult.Unavailable
    }

    companion object {
        private const val TAG = "CloudCategorization"
    }
}
