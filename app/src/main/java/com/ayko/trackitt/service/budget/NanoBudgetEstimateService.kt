package com.ayko.trackitt.service.budget

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

class NanoBudgetEstimateService {

    private var model: GenerativeModel? = null

    suspend fun isAvailable(): Boolean {
        return try {
            val client = Generation.getClient()
            val status = client.checkStatus()
            Log.d(TAG, "Nano budget estimate status: $status")
            if (status == FeatureStatus.AVAILABLE) {
                model = client
                true
            } else {
                if (status == FeatureStatus.DOWNLOADABLE) {
                    client.download().collect { downloadStatus ->
                        Log.d(TAG, "Nano budget estimate download status: $downloadStatus")
                    }
                    if (client.checkStatus() == FeatureStatus.AVAILABLE) {
                        model = client
                        return true
                    }
                }
                Log.d(TAG, "Nano budget estimate unavailable with status=$status")
                client.close()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano budget estimate availability failed", e)
            false
        }
    }

    suspend fun generate(
        snapshot: BudgetEstimateSnapshot,
        fallback: BudgetEstimate,
    ): BudgetEstimate? {
        val client = model ?: run {
            Log.d(TAG, "Nano budget estimate skipped because model is null")
            return null
        }
        return try {
            val request = generateContentRequest(TextPart(BudgetEstimatePrompt.build(snapshot))) {
                temperature = 0.2f
                topK = 1
                maxOutputTokens = 80
            }
            val response = client.generateContent(request)
            val text = response.candidates.firstOrNull()?.text ?: run {
                Log.w(TAG, "Nano budget estimate returned no candidates")
                return null
            }
            val amount = BudgetEstimatePrompt.parseAmount(text)
            if (amount == null) {
                Log.w(TAG, "Nano budget estimate returned unparseable text: ${text.take(240)}")
                return null
            }
            fallback.copy(
                amount = amount,
                origin = BudgetEstimateOrigin.ON_DEVICE_AI,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Nano budget estimate generation failed", e)
            null
        }
    }

    fun close() {
        model?.close()
        model = null
    }

    companion object {
        private const val TAG = "NanoBudgetEstimate"
    }
}
