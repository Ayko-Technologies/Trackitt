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
    val source: String = "MTN MoMo",
    /** Account balance the SMS reported after this transaction, if any. */
    val balance: Double? = null,
)

data class ParsedReceipt(
    val amount: Double,
    val party: String,
    val date: Long,
    val sender: String,
    val rawSms: String,
)
