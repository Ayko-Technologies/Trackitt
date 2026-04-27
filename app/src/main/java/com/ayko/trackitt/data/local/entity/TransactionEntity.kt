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
)
