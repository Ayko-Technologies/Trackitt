package com.ayko.trackitt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The name the user gave a phone number that their SMS show without one. */
@Entity(tableName = "payee_names")
data class PayeeNameEntity(
    @PrimaryKey
    val number: String,
    val name: String,
)
