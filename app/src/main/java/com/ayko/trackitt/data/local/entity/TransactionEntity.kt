package com.ayko.trackitt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val fee: Double,
    val isIncome: Boolean,
    val category: String,
    val merchant: String,
    val reference: String?,
    val transactionId: String,
    val source: String,
    val note: String?,
    val date: Long,
    val isFromSms: Boolean,
    val rawSms: String?,
    val needsClassification: Boolean = false,
    /** Account balance the SMS reported after this transaction, when it gave one. */
    val balance: Double? = null,
    /** Money moved between two of the user's own accounts; not spending or income. */
    val isOwnTransfer: Boolean = false,
    /** The user said this isn't a transfer between their accounts; never pair it again. */
    val ownTransferDismissed: Boolean = false,
    /** The user left this out of their totals (say, a purchase someone else made on their card). */
    val isExcluded: Boolean = false,
)
