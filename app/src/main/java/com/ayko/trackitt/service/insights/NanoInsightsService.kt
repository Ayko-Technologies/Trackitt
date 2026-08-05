package com.ayko.trackitt.service.insights

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

class NanoInsightsService {

    private var model: GenerativeModel? = null

    suspend fun isAvailable(): Boolean {
        return try {
            val client = Generation.getClient()
            val status = client.checkStatus()
            Log.d(TAG, "Nano insight status: $status")
            if (status == FeatureStatus.AVAILABLE) {
                model = client
                true
            } else {
                if (status == FeatureStatus.DOWNLOADABLE) {
                    client.download().collect { downloadStatus ->
                        Log.d(TAG, "Nano insight download status: $downloadStatus")
                    }
                    if (client.checkStatus() == FeatureStatus.AVAILABLE) {
                        model = client
                        return true
                    }
                }
                Log.d(TAG, "Nano insight unavailable with status=$status")
                client.close()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano insight availability failed", e)
            false
        }
    }

    suspend fun generate(
        snapshot: MonthlyInsightsSnapshot,
        fallback: InsightNarrative,
    ): InsightNarrative? {
        val client = model ?: run {
            Log.d(TAG, "Nano insight generate skipped because model is null")
            return null
        }
        return try {
            var draft = InsightNarrativeDraft()

            requestSection(
                client = client,
                label = "headline_where",
                prompt = InsightsPrompt.buildHeadlineAndWherePrompt(snapshot),
                maxOutputTokens = 120,
                parser = InsightsPrompt::parseHeadlineAndWhere,
            )?.let { section ->
                draft = draft.copy(
                    headline = section.headline ?: draft.headline,
                    whereMoneyWent = section.whereMoneyWent ?: draft.whereMoneyWent,
                )
            }

            requestSection(
                client = client,
                label = "cut",
                prompt = InsightsPrompt.buildCutPrompt(snapshot, draft),
                maxOutputTokens = 110,
                parser = InsightsPrompt::parseCut,
            )?.let { section ->
                draft = draft.copy(cutBack = section.cutBack ?: draft.cutBack)
            }

            requestSection(
                client = client,
                label = "save",
                prompt = InsightsPrompt.buildSavePrompt(snapshot, draft),
                maxOutputTokens = 110,
                parser = InsightsPrompt::parseSave,
            )?.let { section ->
                draft = draft.copy(saveMore = section.saveMore ?: draft.saveMore)
            }

            if (!draft.hasAnyContent()) return null
            draft.mergeInto(fallback, InsightSource.ON_DEVICE_AI)
        } catch (e: Exception) {
            Log.w(TAG, "Nano insight generation failed", e)
            null
        }
    }

    private suspend fun requestSection(
        client: GenerativeModel,
        label: String,
        prompt: String,
        maxOutputTokens: Int,
        parser: (String) -> InsightNarrativeDraft?,
    ): InsightNarrativeDraft? {
        val request = generateContentRequest(TextPart(prompt)) {
            temperature = 0.2f
            topK = 1
            this.maxOutputTokens = maxOutputTokens
        }
        val response = client.generateContent(request)
        val text = response.candidates.firstOrNull()?.text ?: run {
            Log.w(TAG, "Nano insight section $label returned no candidates")
            return null
        }
        val parsed = parser(text)
        if (parsed == null) {
            Log.w(TAG, "Nano insight section $label returned unparseable text: ${text.take(240)}")
        } else {
            Log.d(TAG, "Nano insight section $label parsed successfully")
        }
        return parsed
    }

    fun close() {
        model?.close()
        model = null
    }

    companion object {
        private const val TAG = "NanoInsights"
    }
}
