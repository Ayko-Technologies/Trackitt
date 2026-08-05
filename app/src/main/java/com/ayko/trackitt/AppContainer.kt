package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.categorization.NanoCategorizationService
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.budget.BudgetAlertService
import com.ayko.trackitt.service.budget.BudgetEstimateCache
import com.ayko.trackitt.service.budget.BudgetEstimateNotificationService
import com.ayko.trackitt.service.budget.BudgetEstimateService
import com.ayko.trackitt.service.budget.NanoBudgetEstimateService
import com.ayko.trackitt.service.insights.MonthlyInsightsAnalyzer
import com.ayko.trackitt.service.insights.MonthlyInsightsCache
import com.ayko.trackitt.service.insights.MonthlyInsightsFallbackComposer
import com.ayko.trackitt.service.insights.NanoInsightsService
import com.ayko.trackitt.service.insights.SmartInsightsService
import com.ayko.trackitt.service.sms.EcobankSmsParser
import com.ayko.trackitt.service.sms.MoMoSmsParser
import com.ayko.trackitt.service.sms.ParsedTransaction
import com.ayko.trackitt.service.sms.TelecelCashParser

class AppContainer(private val context: Context) {
    val database = TrackittDatabase.getInstance(context)

    private val dictionaryService = DictionaryCategorizationService(
        userLookup = { merchant ->
            val mapping = database.merchantMappingDao().getMapping(merchant)
            mapping?.let {
                try { TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
            }
        }
    )

    private val nanoService = NanoCategorizationService()
    private val nanoInsightsService = NanoInsightsService()
    private val nanoBudgetEstimateService = NanoBudgetEstimateService()
    private val insightsCache = MonthlyInsightsCache(
        context.getSharedPreferences("insights_cache", 0)
    )
    private val budgetEstimateCache = BudgetEstimateCache(
        context.getSharedPreferences("budget_estimate_cache", 0)
    )
    private val budgetAlertService = BudgetAlertService(
        context = context,
        budgetDao = database.budgetDao(),
        transactionDao = database.transactionDao(),
    )
    val budgetEstimateService = BudgetEstimateService(
        budgetDao = database.budgetDao(),
        transactionDao = database.transactionDao(),
        cache = budgetEstimateCache,
        nano = nanoBudgetEstimateService,
        showFees = {
            context.getSharedPreferences("trackitt_display", 0)
                .getBoolean("show_fees", false)
        },
        excludeTransfers = {
            context.getSharedPreferences("trackitt_display", 0)
                .getBoolean("exclude_transfers", false)
        },
        useOnDeviceAi = {
            context.getSharedPreferences("trackitt", 0)
                .getBoolean("use_on_device_ai", true)
        },
    )
    val budgetEstimateNotifications = BudgetEstimateNotificationService(
        context = context,
        estimateService = budgetEstimateService,
        prefs = context.getSharedPreferences("budget_estimate_notifications", 0),
    )

    private val smartCategorization = SmartCategorizationService(
        dictionary = dictionaryService,
        nano = nanoService,
        useOnDeviceAi = {
            context.getSharedPreferences("trackitt", 0)
                .getBoolean("use_on_device_ai", true)
        },
    )

    val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        merchantMappingDao = database.merchantMappingDao(),
        smartCategorization = smartCategorization,
        budgetAlerts = budgetAlertService,
    )

    val insightsService = SmartInsightsService(
        analyzer = MonthlyInsightsAnalyzer(),
        fallbackComposer = MonthlyInsightsFallbackComposer(),
        cache = insightsCache,
        nano = nanoInsightsService,
        useOnDeviceAi = {
            context.getSharedPreferences("trackitt", 0)
                .getBoolean("use_on_device_ai", true)
        },
    )

    private val momoParser = MoMoSmsParser()
    private val ecobankParser = EcobankSmsParser()
    private val telecelParser = TelecelCashParser()

    private val allSenders = mapOf(
        "MobileMoney" to momoParser,
        "ECOBANK" to ecobankParser,
        "T-CASH" to telecelParser,
    )

    val smsSenders: List<String>
        get() {
            val prefs = context.getSharedPreferences("sms_sources", 0)
            return allSenders.keys.filter { prefs.getBoolean("source_$it", true) }
        }

    fun parseSms(sender: String?, body: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val prefs = context.getSharedPreferences("sms_sources", 0)

        return when {
            sender.equals("MobileMoney", ignoreCase = true) -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body, smsTimestamp) else null
            }
            sender.equals("ECOBANK", ignoreCase = true) -> {
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body) else null
            }
            sender.equals("T-CASH", ignoreCase = true) -> {
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body) else null
            }
            else -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body, smsTimestamp)?.let { return it }
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body)?.let { return it }
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body)?.let { return it }
                null
            }
        }
    }
}
