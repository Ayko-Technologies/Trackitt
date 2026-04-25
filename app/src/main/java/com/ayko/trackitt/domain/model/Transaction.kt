package com.ayko.trackitt.domain.model

import java.time.LocalDateTime

data class Transaction(
    val id: String,
    val amount: Double,
    val category: TransactionCategory,
    val merchant: String,
    val date: LocalDateTime,
    val reference: String? = null,
    val source: String = "MTN MoMo",
    val note: String? = null,
    val isFromSms: Boolean = false,
)
