package com.ayko.trackitt.service.categorization

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
        val dictionaryResult = dictionary.categorize(merchant, reference, note)
        if (dictionaryResult != TransactionCategory.OTHER) {
            return ClassificationResult.Classified(dictionaryResult)
        }

        if (useOnDeviceAi() && nano.isAvailable()) {
            val nanoResult = nano.classify(merchant, reference, note)
            if (nanoResult is ClassificationResult.Classified) {
                return nanoResult
            }
        }

        return cloud.classify(merchant, reference, note)
    }
}
