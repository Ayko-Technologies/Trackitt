package com.ayko.trackitt.service.budget

import android.util.Log
import com.ayko.trackitt.data.local.dao.BudgetDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.domain.model.TransactionCategory
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class BudgetEstimateService(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val cache: BudgetEstimateCache,
    private val nano: NanoBudgetEstimateService,
    private val showFees: () -> Boolean,
    private val useOnDeviceAi: () -> Boolean,
) {

    suspend fun estimateForMonth(
        date: LocalDate = LocalDate.now(),
        forceShow: Boolean = false,
    ): BudgetEstimate? {
        if (!forceShow && !shouldShow(date)) return null
        val snapshot = loadSnapshot(date) ?: return null
        return generate(snapshot)
    }

    fun shouldShow(date: LocalDate = LocalDate.now()): Boolean {
        return date.dayOfMonth <= ESTIMATE_VISIBLE_UNTIL_DAY
    }

    suspend fun generate(snapshot: BudgetEstimateSnapshot): BudgetEstimate {
        val fallback = fallback(snapshot)
        val onDeviceEnabled = useOnDeviceAi()
        if (onDeviceEnabled) {
            cache.load(snapshot)?.let {
                Log.d(TAG, "Using cached ${it.origin} budget estimate for ${snapshot.monthLabel}")
                return it
            }
        }

        if (onDeviceEnabled && nano.isAvailable()) {
            nano.generate(snapshot, fallback)?.let {
                cache.save(snapshot, it)
                return it
            }
            Log.d(TAG, "Nano budget estimate did not produce a usable result for ${snapshot.monthLabel}")
        } else if (onDeviceEnabled) {
            Log.d(TAG, "Nano budget estimate unavailable for ${snapshot.monthLabel}")
        } else {
            Log.d(TAG, "On-device AI disabled for budget estimate")
        }

        return fallback
    }

    private suspend fun loadSnapshot(date: LocalDate): BudgetEstimateSnapshot? {
        val historicalSpend = historicalMedianSpend(date)
        val savedBudgetTotal = savedBudgetTotal().takeIf { it > 0.0 }
        val sources = listOfNotNull(
            historicalSpend?.let {
                BudgetEstimateSource(
                    type = BudgetEstimateSourceType.SPENDING_HISTORY,
                    label = historyLabel(it.monthCount),
                    amount = it.amount,
                    supportingMonths = it.monthCount,
                )
            },
            savedBudgetTotal?.let {
                BudgetEstimateSource(
                    type = BudgetEstimateSourceType.SAVED_BUDGET,
                    label = "this month's saved budgets",
                    amount = it,
                )
            },
        )

        if (sources.isEmpty()) return null

        val fingerprint = fingerprint(date, sources)
        return BudgetEstimateSnapshot(
            year = date.year,
            month = date.monthValue,
            monthLabel = monthLabel(date),
            fingerprint = fingerprint,
            sources = sources,
        )
    }

    private suspend fun historicalMedianSpend(date: LocalDate): HistoricalSpend? {
        val startOfMonth = date.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val transactions = transactionDao.getExpensesBefore(startOfMonth)

        if (transactions.isEmpty()) return null

        val totalsByMonth = transactions
            .groupBy { transaction ->
                val localDate = Instant.ofEpochMilli(transaction.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                YearMonth.of(localDate.year, localDate.monthValue)
            }
            .mapValues { (_, items) ->
                items.sumOf { item ->
                    item.amount + if (showFees()) item.fee else 0.0
                }
            }
            .filterValues { it > 0.0 }

        if (totalsByMonth.isEmpty()) return null

        val recentMonths = totalsByMonth.entries
            .sortedByDescending { it.key }
            .take(MAX_HISTORY_MONTHS)
            .map { it.value }
            .sorted()

        return HistoricalSpend(
            amount = median(recentMonths),
            monthCount = recentMonths.size,
        )
    }

    private fun historyLabel(monthCount: Int): String {
        return when (monthCount) {
            1 -> "your most recent completed month's spending"
            else -> "your typical monthly spending over the last $monthCount completed months"
        }
    }

    private suspend fun savedBudgetTotal(): Double {
        return budgetDao.getAllNow()
            .asSequence()
            .filterNot { it.category == TransactionCategory.INCOME.name }
            .sumOf { it.amount }
    }

    private fun fallback(snapshot: BudgetEstimateSnapshot): BudgetEstimate {
        val amount = when (snapshot.sources.size) {
            0 -> 0.0
            1 -> snapshot.sources.first().amount
            else -> snapshot.sources.map { it.amount }.average()
        }

        val summary = when {
            snapshot.sources.size == 1 && snapshot.sources.first().type == BudgetEstimateSourceType.SPENDING_HISTORY ->
                "Estimated from ${snapshot.sources.first().label}."
            snapshot.sources.size == 1 && snapshot.sources.first().type == BudgetEstimateSourceType.SAVED_BUDGET ->
                "Estimated from this month's saved budgets."
            else ->
                "Estimated from ${snapshot.sources.first { it.type == BudgetEstimateSourceType.SPENDING_HISTORY }.label} and this month's saved budgets."
        }

        return BudgetEstimate(
            amount = amount,
            summary = summary,
            origin = BudgetEstimateOrigin.RULES,
            sources = snapshot.sources,
        )
    }

    private fun fingerprint(date: LocalDate, sources: List<BudgetEstimateSource>): String {
        val canonical = buildString {
            append("year=${date.year}|month=${date.monthValue}|showFees=${showFees()}\n")
            sources.sortedBy { it.type.name }.forEach { source ->
                append(source.type.name)
                append('|')
                append(source.label)
                append('|')
                append(source.amount)
                append('\n')
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun monthLabel(date: LocalDate): String {
        val month = Month.of(date.monthValue).name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return "$month ${date.year}"
    }

    companion object {
        const val ESTIMATE_VISIBLE_UNTIL_DAY = 7
        private const val MAX_HISTORY_MONTHS = 4
        private const val TAG = "BudgetEstimate"
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val middle = values.size / 2
        return if (values.size % 2 == 0) {
            (values[middle - 1] + values[middle]) / 2.0
        } else {
            values[middle]
        }
    }

    private data class HistoricalSpend(
        val amount: Double,
        val monthCount: Int,
    )
}
