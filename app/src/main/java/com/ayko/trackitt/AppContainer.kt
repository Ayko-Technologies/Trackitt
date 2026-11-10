package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.categorization.NanoCategorizationService
import com.ayko.trackitt.service.categorization.RememberedCategories
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
import com.ayko.trackitt.service.sms.ParsedTransaction
import com.ayko.trackitt.service.sms.SmsProviders
import com.ayko.trackitt.service.sms.SmsTransactionParser

class AppContainer(private val context: Context) {
    val database = TrackittDatabase.getInstance(context)

    private val rememberedCategories = RememberedCategories(
        chosenFor = { merchant ->
            database.merchantMappingDao().getMapping(merchant)?.let {
                try { TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
            }
        },
        recentCategoriesOf = { merchant, count ->
            database.transactionDao().recentCategoriesOf(merchant, count).mapNotNull {
                try { TransactionCategory.valueOf(it) } catch (_: Exception) { null }
            }
        },
    )

    private val dictionaryService = DictionaryCategorizationService(userLookup = rememberedCategories::of)

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
        smsReceiptDao = database.smsReceiptDao(),
        payeeNameDao = database.payeeNameDao(),
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

    private val smsParser = SmsTransactionParser()

    fun parseSms(sender: String?, body: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        if (!isSourceEnabled(sender)) return null
        return smsParser.parse(sender, body, smsTimestamp)
    }

    /** Stores an SMS: a transaction when it is one, else a merchant receipt that may rename one. */
    suspend fun importSms(sender: String?, body: String, smsTimestamp: Long, bulk: Boolean = false): ParsedTransaction? {
        if (!isSourceEnabled(sender)) return null
        val parsed = smsParser.parse(sender, body, smsTimestamp)
        if (parsed != null) {
            return parsed.takeIf { repository.insertFromSms(it, bulk) }
        }
        smsParser.parseReceipt(sender, body, smsTimestamp)?.let { repository.recordReceipt(it) }
        return null
    }

    private fun isSourceEnabled(sender: String?): Boolean {
        val prefs = context.getSharedPreferences("sms_sources", 0)
        val provider = SmsProviders.forSender(sender)
        return if (provider != null) {
            prefs.getBoolean(SmsProviders.prefKey(provider), true)
        } else {
            prefs.getBoolean(SmsProviders.OTHER_SENDERS_PREF, true)
        }
    }
}
