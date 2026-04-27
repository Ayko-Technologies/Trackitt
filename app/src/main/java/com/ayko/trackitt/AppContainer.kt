package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CloudCategorizationService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.categorization.NanoCategorizationService
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.network.NetworkMonitor
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
    private val cloudService = CloudCategorizationService()

    private val smartCategorization = SmartCategorizationService(
        dictionary = dictionaryService,
        nano = nanoService,
        cloud = cloudService,
        useOnDeviceAi = {
            context.getSharedPreferences("trackitt", 0)
                .getBoolean("use_on_device_ai", true)
        },
    )

    val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        merchantMappingDao = database.merchantMappingDao(),
        smartCategorization = smartCategorization,
    )

    val networkMonitor = NetworkMonitor(context)

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

    fun parseSms(sender: String?, body: String): ParsedTransaction? {
        val prefs = context.getSharedPreferences("sms_sources", 0)

        return when {
            sender.equals("MobileMoney", ignoreCase = true) -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body) else null
            }
            sender.equals("ECOBANK", ignoreCase = true) -> {
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body) else null
            }
            sender.equals("T-CASH", ignoreCase = true) -> {
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body) else null
            }
            else -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body)?.let { return it }
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body)?.let { return it }
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body)?.let { return it }
                null
            }
        }
    }
}
