package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CategorizationService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.sms.EcobankSmsParser
import com.ayko.trackitt.service.sms.MoMoSmsParser
import com.ayko.trackitt.service.sms.ParsedTransaction

class AppContainer(context: Context) {
    val database = TrackittDatabase.getInstance(context)

    val categorizationService: CategorizationService = DictionaryCategorizationService(
        userLookup = { merchant ->
            val mapping = database.merchantMappingDao().getMapping(merchant)
            mapping?.let {
                try { TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
            }
        }
    )

    val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        merchantMappingDao = database.merchantMappingDao(),
        categorizationService = categorizationService,
    )

    private val momoParser = MoMoSmsParser()
    private val ecobankParser = EcobankSmsParser()

    val smsSenders = listOf("MobileMoney", "ECOBANK")

    fun parseSms(sender: String?, body: String): ParsedTransaction? {
        return when {
            sender.equals("MobileMoney", ignoreCase = true) -> momoParser.parse(body)
            sender.equals("ECOBANK", ignoreCase = true) -> ecobankParser.parse(body)
            else -> momoParser.parse(body) ?: ecobankParser.parse(body)
        }
    }
}
