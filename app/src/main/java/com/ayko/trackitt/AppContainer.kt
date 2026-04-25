package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CategorizationService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.sms.MoMoSmsParser

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

    val smsParser = MoMoSmsParser()
}
