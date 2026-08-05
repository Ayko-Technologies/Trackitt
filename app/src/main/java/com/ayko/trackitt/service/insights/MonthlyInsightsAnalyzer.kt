package com.ayko.trackitt.service.insights

import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import java.security.MessageDigest
import java.time.Month

class MonthlyInsightsAnalyzer {

    fun analyze(
        year: Int,
        month: Int,
        transactions: List<Transaction>,
        excludeTransfers: Boolean = false,
    ): MonthlyInsightsSnapshot {
        val expenseTransactions = transactions.filter { transaction ->
            !transaction.isIncome &&
                (!excludeTransfers || transaction.category != TransactionCategory.TRANSFER)
        }
        val incomeTransactions = transactions.filter { it.isIncome }
        val spend = expenseTransactions.sumOf { it.amount }
        val fees = expenseTransactions.sumOf { it.fee }
        val income = incomeTransactions.sumOf { it.amount }
        val net = income - spend - fees

        val topCategories = expenseTransactions
            .groupBy { it.category }
            .map { (category, items) ->
                InsightCategoryBreakdown(
                    category = category,
                    amount = items.sumOf { it.amount },
                    fee = items.sumOf { it.fee },
                    shareOfSpend = share(items.sumOf { it.amount }, spend),
                )
            }
            .sortedByDescending { it.amount }

        val topMerchants = expenseTransactions
            .groupBy { it.merchant.trim() }
            .map { (merchant, items) ->
                InsightMerchantBreakdown(
                    merchant = merchant,
                    amount = items.sumOf { it.amount },
                    fee = items.sumOf { it.fee },
                    shareOfSpend = share(items.sumOf { it.amount }, spend),
                    categories = items.map { it.category }.distinct(),
                    notes = items.mapNotNull { it.note?.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct(),
                )
            }
            .sortedByDescending { it.amount }

        val topFeeMerchants = expenseTransactions
            .groupBy { it.merchant.trim() }
            .map { (merchant, items) ->
                InsightMerchantBreakdown(
                    merchant = merchant,
                    amount = items.sumOf { it.amount },
                    fee = items.sumOf { it.fee },
                    shareOfSpend = share(items.sumOf { it.amount }, spend),
                    categories = items.map { it.category }.distinct(),
                    notes = items.mapNotNull { it.note?.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct(),
                )
            }
            .filter { it.fee > 0.0 }
            .sortedByDescending { it.fee }

        val monthLabel = "${Month.of(month).name.lowercase().replaceFirstChar { it.uppercase() }} $year"
        val uncategorizedSpend = topCategories.find { it.category == TransactionCategory.OTHER }?.amount ?: 0.0
        val fingerprint = fingerprintFor(transactions, excludeTransfers)

        return MonthlyInsightsSnapshot(
            year = year,
            month = month,
            monthLabel = monthLabel,
            fingerprint = fingerprint,
            transactionCount = transactions.size,
            expenseCount = expenseTransactions.size,
            incomeCount = incomeTransactions.size,
            spend = spend,
            fees = fees,
            income = income,
            net = net,
            uncategorizedSpend = uncategorizedSpend,
            topCategories = topCategories,
            topMerchants = topMerchants,
            topFeeMerchants = topFeeMerchants,
        )
    }

    private fun fingerprintFor(transactions: List<Transaction>, excludeTransfers: Boolean): String {
        val canonical = transactions
            .sortedWith(
                compareBy<Transaction> { it.date }
                    .thenBy { it.id }
                    .thenBy { it.transactionId }
                    .thenBy { it.merchant }
            )
            .joinToString(separator = "\n") { transaction ->
                listOf(
                    transaction.id.toString(),
                    transaction.date.toString(),
                    transaction.amount.toString(),
                    transaction.fee.toString(),
                    transaction.isIncome.toString(),
                    transaction.category.name,
                    transaction.merchant,
                    transaction.reference.orEmpty(),
                    transaction.transactionId,
                    transaction.source,
                    transaction.note.orEmpty(),
                    transaction.isFromSms.toString(),
                ).joinToString("|")
            }
            .let { "excludeTransfers=$excludeTransfers\n$it" }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun share(amount: Double, total: Double): Double {
        if (total <= 0.0) return 0.0
        return amount / total
    }
}
