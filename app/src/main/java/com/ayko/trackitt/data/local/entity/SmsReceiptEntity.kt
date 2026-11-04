package com.ayko.trackitt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A merchant or payment-service receipt ("You have successfully paid GHS 151.50 to Shell Ring Road").
 * It isn't a transaction of its own (the wallet reports the same payment), but it names the merchant
 * better than the wallet does, so it is kept until it can be matched to that wallet transaction.
 */
@Entity(
    tableName = "sms_receipts",
    indices = [Index(value = ["rawSmsHash"], unique = true), Index(value = ["date"])],
)
data class SmsReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val amount: Double,
    val party: String,
    val date: Long,
    val rawSmsHash: String,
    val matchedTransactionId: Long? = null,
)
