package com.ayko.trackitt.service.categorization

import android.util.Log
import com.ayko.trackitt.domain.model.TransactionCategory

class SmartCategorizationService(
    private val dictionary: DictionaryCategorizationService,
    private val nano: NanoCategorizationService,
    private val useOnDeviceAi: () -> Boolean,
) {

    suspend fun classify(
        merchant: String,
        reference: String? = null,
        note: String? = null,
        allowAi: Boolean = true,
    ): ClassificationResult {
        Log.d(TAG, "Classifying transaction locally")

        val dictionaryResult = dictionary.categorize(merchant, reference, note)
        if (dictionaryResult != TransactionCategory.OTHER) {
            Log.d(TAG, "Dictionary matched: $dictionaryResult")
            return ClassificationResult.Classified(dictionaryResult)
        }
        Log.d(TAG, "Dictionary returned OTHER, trying on-device AI")

        val onDeviceEnabled = allowAi && useOnDeviceAi()
        if (onDeviceEnabled && nano.isAvailable()) {
            val nanoResult = nano.classify(merchant, reference, note)
            if (nanoResult is ClassificationResult.Classified) {
                return nanoResult
            }
        }

        Log.d(TAG, "No local classification available")
        return ClassificationResult.Unavailable
    }

    /** The category the note or reference names, ignoring the merchant. Null when neither names one. */
    fun purposeOf(reference: String?, note: String?): TransactionCategory? = dictionary.purposeOf(reference, note)

    companion object {
        private const val TAG = "SmartCategorization"
    }
}
