package com.ayko.trackitt.service.categorization

import android.util.Log
import com.ayko.trackitt.domain.model.TransactionCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class CloudCategorizationService {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash-lite")
    }

    suspend fun classify(
        merchant: String,
        reference: String?,
        note: String?,
    ): ClassificationResult {
        return try {
            val prompt = CategorizationPrompt.build(merchant, reference, note)
            val response = model.generateContent(prompt)
            val text = response.text
            if (text == null) {
                Log.w(TAG, "Cloud returned no text")
                return ClassificationResult.Unavailable
            }
            val category = CategorizationPrompt.parseResponse(text)
            if (category != null) {
                ClassificationResult.Classified(category)
            } else {
                Log.w(TAG, "Cloud returned unparseable: $text")
                ClassificationResult.Classified(TransactionCategory.OTHER)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud classification failed", e)
            ClassificationResult.Unavailable
        }
    }

    companion object {
        private const val TAG = "CloudCategorization"
    }
}
