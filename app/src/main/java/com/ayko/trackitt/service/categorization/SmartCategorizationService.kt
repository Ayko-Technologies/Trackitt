package com.ayko.trackitt.service.categorization

import android.util.Log
import com.ayko.trackitt.domain.model.TransactionCategory

class SmartCategorizationService(
    private val dictionary: DictionaryCategorizationService,
    private val nano: NanoCategorizationService,
    private val cloud: CloudCategorizationService,
    private val useOnDeviceAi: () -> Boolean,
) {

    suspend fun classify(
        merchant: String,
        reference: String? = null,
        note: String? = null,
    ): ClassificationResult {
        Log.d(TAG, "Classifying: merchant=$merchant, note=$note, reference=$reference")

        val dictionaryResult = dictionary.categorize(merchant, reference, note)
        if (dictionaryResult != TransactionCategory.OTHER) {
            Log.d(TAG, "Dictionary matched: $dictionaryResult")
            return ClassificationResult.Classified(dictionaryResult)
        }
        Log.d(TAG, "Dictionary returned OTHER, trying LLM")

        val onDeviceEnabled = useOnDeviceAi()
        Log.d(TAG, "On-device AI enabled: $onDeviceEnabled")

        if (onDeviceEnabled && nano.isAvailable()) {
            Log.d(TAG, "Nano available, classifying...")
            val nanoResult = nano.classify(merchant, reference, note)
            Log.d(TAG, "Nano result: $nanoResult")
            if (nanoResult is ClassificationResult.Classified) {
                return nanoResult
            }
        }

        Log.d(TAG, "Falling through to cloud")
        return cloud.classify(merchant, reference, note)
    }

    companion object {
        private const val TAG = "SmartCategorization"
    }
}
