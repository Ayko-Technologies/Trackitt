package com.ayko.trackitt.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val fee: Double = 0.0,
    val isIncome: Boolean = false,
    val category: TransactionCategory,
    val merchant: String,
    val reference: String? = null,
    val transactionId: String = "",
    val source: String = "MTN MoMo",
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val isFromSms: Boolean = false,
    val rawSms: String? = null,
    val balance: Double? = null,
    val isOwnTransfer: Boolean = false,
    val isExcluded: Boolean = false,
    val payeeNumber: String? = null,
)
