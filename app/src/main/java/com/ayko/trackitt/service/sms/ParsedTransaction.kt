package com.ayko.trackitt.service.sms

data class ParsedTransaction(
    val amount: Double,
    val fee: Double,
    val isIncome: Boolean,
    val merchant: String,
    val transactionId: String,
    val reference: String?,
    val date: Long,
    val rawSms: String,
)
