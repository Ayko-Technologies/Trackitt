package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

val MERCHANT_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    "melcom" to TransactionCategory.SHOPPING,
    "game" to TransactionCategory.SHOPPING,
    "palace" to TransactionCategory.SHOPPING,
    "koala" to TransactionCategory.SHOPPING,

    "shoprite" to TransactionCategory.GROCERIES,
    "maxmart" to TransactionCategory.GROCERIES,
    "marina mall" to TransactionCategory.GROCERIES,

    "papaye" to TransactionCategory.FOOD,
    "kfc" to TransactionCategory.FOOD,
    "chicken inn" to TransactionCategory.FOOD,
    "pizza inn" to TransactionCategory.FOOD,
    "marwako" to TransactionCategory.FOOD,
    "burger king" to TransactionCategory.FOOD,
    "frankies" to TransactionCategory.FOOD,

    "bolt" to TransactionCategory.TRANSPORT,
    "uber" to TransactionCategory.TRANSPORT,
    "yango" to TransactionCategory.TRANSPORT,
    "trotro" to TransactionCategory.TRANSPORT,
    "taxi" to TransactionCategory.TRANSPORT,

    "ecg" to TransactionCategory.BILLS,
    "gwcl" to TransactionCategory.BILLS,
    "dstv" to TransactionCategory.BILLS,
    "gotv" to TransactionCategory.BILLS,
    "vodafone" to TransactionCategory.BILLS,
    "airtel" to TransactionCategory.BILLS,
    "bills.inv" to TransactionCategory.BILLS,
    "surfline" to TransactionCategory.BILLS,

    "expresspay" to TransactionCategory.INCOME,
    "express pay" to TransactionCategory.INCOME,
    "ecobank" to TransactionCategory.INCOME,
    "gcb" to TransactionCategory.INCOME,
    "stanbic" to TransactionCategory.INCOME,
    "fidelity" to TransactionCategory.INCOME,
    "calbank" to TransactionCategory.INCOME,
    "absa" to TransactionCategory.INCOME,
    "uba" to TransactionCategory.INCOME,

    "bigpay" to TransactionCategory.TRANSFER,
)
