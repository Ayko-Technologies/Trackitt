package com.ayko.trackitt.ui.screen.home

import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.ChartCategory

val fakeTransactions = listOf(
    Transaction(
        id = 1,
        amount = 215.00,
        category = TransactionCategory.SHOPPING,
        merchant = "Lydia — supermarket",
        date = System.currentTimeMillis(),
        reference = "8827104",
        source = "MTN MoMo",
        isFromSms = true,
    ),
    Transaction(
        id = 2,
        amount = 85.50,
        category = TransactionCategory.FOOD,
        merchant = "Papaye Fast Food",
        date = System.currentTimeMillis(),
        source = "MTN MoMo",
        isFromSms = true,
    ),
)

val fakeChartCategories = listOf(
    ChartCategory(TransactionCategory.FOOD, 842.50),
    ChartCategory(TransactionCategory.GROCERIES, 680.00),
    ChartCategory(TransactionCategory.TRANSPORT, 412.00),
    ChartCategory(TransactionCategory.BILLS, 280.00),
)

const val fakeTotalSpent = 2378.50
