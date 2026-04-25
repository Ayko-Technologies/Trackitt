package com.ayko.trackitt.ui.screen.home

import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.ChartCategory
import java.time.LocalDateTime

val fakeTransactions = listOf(
    Transaction(
        id = "1",
        amount = 215.00,
        category = TransactionCategory.SHOPPING,
        merchant = "Lydia — supermarket",
        date = LocalDateTime.of(2026, 4, 25, 14, 14),
        reference = "8827104",
        source = "MTN MoMo",
        isFromSms = true,
    ),
    Transaction(
        id = "2",
        amount = 85.50,
        category = TransactionCategory.FOOD,
        merchant = "Papaye Fast Food",
        date = LocalDateTime.of(2026, 4, 25, 12, 30),
        source = "MTN MoMo",
        isFromSms = true,
    ),
    Transaction(
        id = "3",
        amount = 42.00,
        category = TransactionCategory.TRANSPORT,
        merchant = "Bolt ride",
        date = LocalDateTime.of(2026, 4, 24, 18, 45),
        source = "MTN MoMo",
        isFromSms = true,
    ),
    Transaction(
        id = "4",
        amount = 280.00,
        category = TransactionCategory.BILLS,
        merchant = "ECG Prepaid",
        date = LocalDateTime.of(2026, 4, 23, 9, 0),
        source = "Telecel Cash",
        isFromSms = true,
    ),
    Transaction(
        id = "5",
        amount = 680.00,
        category = TransactionCategory.GROCERIES,
        merchant = "Shoprite Accra Mall",
        date = LocalDateTime.of(2026, 4, 22, 16, 20),
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
