package com.ayko.trackitt.service.categorization

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

class NanoCategorizationService {

    private var model: GenerativeModel? = null

    suspend fun isAvailable(): Boolean {
        return try {
            val m = Generation.getClient()
            val status = m.checkStatus()
            Log.d(TAG, "Nano status: $status (AVAILABLE=${FeatureStatus.AVAILABLE}, DOWNLOADABLE=${FeatureStatus.DOWNLOADABLE}, DOWNLOADING=${FeatureStatus.DOWNLOADING}, UNAVAILABLE=${FeatureStatus.UNAVAILABLE})")
            if (status == FeatureStatus.AVAILABLE) {
                model = m
                true
            } else {
                if (status == FeatureStatus.DOWNLOADABLE) {
                    Log.d(TAG, "Nano model downloadable but not yet downloaded — triggering download")
                    m.download().collect { dlStatus ->
                        Log.d(TAG, "Nano download status: $dlStatus")
                    }
                    val retryStatus = m.checkStatus()
                    if (retryStatus == FeatureStatus.AVAILABLE) {
                        model = m
                        return true
                    }
                }
                m.close()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano availability check failed", e)
            false
        }
    }

    suspend fun classify(
        merchant: String,
        reference: String?,
        note: String?,
    ): ClassificationResult {
        val m = model ?: return ClassificationResult.Unavailable
        return try {
            val prompt = CategorizationPrompt.build(merchant, reference, note)
            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.1f
                topK = 1
                maxOutputTokens = 10
            }
            val response = m.generateContent(request)
            val text = response.candidates.firstOrNull()?.text
            if (text == null) {
                Log.w(TAG, "Nano returned no candidates")
                return ClassificationResult.Unavailable
            }
            val category = CategorizationPrompt.parseResponse(text)
            if (category != null) {
                ClassificationResult.Classified(category)
            } else {
                Log.w(TAG, "Nano returned unparseable: $text")
                ClassificationResult.Unavailable
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano classification failed", e)
            ClassificationResult.Unavailable
        }
    }

    fun close() {
        model?.close()
        model = null
    }

    companion object {
        private const val TAG = "NanoCategorization"
    }
}
