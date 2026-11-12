package com.ayko.trackitt.service.recap

import com.ayko.trackitt.data.local.dao.YearActivity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.accounts.PayeeNumber
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One year of the user's money, told as a story: what they spent, on what, with whom and when. */
data class YearRecap(
    val year: Int,
    /** First and last day Trackitt has transactions for in [year]; a year tracked from April starts then. */
    val from: LocalDate,
    val to: LocalDate,
    val spent: Double,
    val payments: Int,
    /** Different people and places paid. */
    val payees: Int,
    val received: Double,
    val fees: Double,
    val feePayments: Int,
    /** Spending by category, largest first, without OTHER (it says nothing about the user). */
    val categories: List<CategoryShare>,
    val topPayees: List<PayeeTotal>,
    /** Who the user paid most often, when it was at least [YearRecapCalculator.MIN_REGULAR] times. */
    val regular: PayeeTotal?,
    val biggest: Transaction?,
    /** Spending in each month from [from] to [to], January = 1. */
    val months: List<MonthTotal>,
    val busiestWeekday: DayOfWeek?,
    /** Payments made between 10 pm and 5 am. */
    val lateNightPayments: Int,
    val noSpendDays: Int,
    val trackedDays: Int,
    /** Change in spending per day against last year (0.1 = 10% more), when last year was tracked enough. */
    val perDayChange: Double?,
    val personality: Personality,
    /** The year is still under way: everything here is "so far". */
    val ongoing: Boolean = false,
) {
    val perDay: Double get() = if (trackedDays > 0) spent / trackedDays else 0.0
    val coversWholeYear: Boolean get() = from.dayOfYear <= 7 && to.isAfter(LocalDate.of(year, 12, 24))
    val topCategory: CategoryShare? get() = categories.firstOrNull()
}

data class CategoryShare(val category: TransactionCategory, val total: Double, val count: Int, val share: Double)

data class PayeeTotal(val name: String, val total: Double, val count: Int)

/** A month's spending; [complete] when Trackitt has the whole month, so it's fair to compare. */
data class MonthTotal(val month: Int, val total: Double, val complete: Boolean = true)

/** A playful, kind label for how the user spent; never a judgement. */
enum class Personality(val title: String, val line: String) {
    PROVIDER("The Provider", "Your money looked after the people around you."),
    FOODIE("The Foodie", "Good food was your love language."),
    MOVER("The Mover", "Always on the move, never stuck in one place."),
    STEADY_HAND("The Steady Hand", "Bills paid, lights on, home running. Reliable."),
    BUILDER("The Builder", "You put your money into something that lasts."),
    TRENDSETTER("The Trendsetter", "You know what you like, and you treat yourself to it."),
    LEARNER("The Learner", "You invested in what you know."),
    GIVER("The Faithful Giver", "Generous with what you were given."),
    CARETAKER("The Caretaker", "Your health and wellbeing came first."),
    ENTREPRENEUR("The Entrepreneur", "Your money worked for your business."),
    CONNECTOR("The Connector", "Calls, data and bundles: you stayed in touch."),
    ALL_ROUNDER("The All-Rounder", "A little of everything, nothing overdone."),
    ;

    companion object {
        fun of(categories: List<CategoryShare>): Personality {
            val top = categories.firstOrNull() ?: return ALL_ROUNDER
            // No category stands out: none took a fifth of the spending.
            if (top.share < 0.2) return ALL_ROUNDER
            return when (top.category) {
                TransactionCategory.POCKET_MONEY, TransactionCategory.GIFTS, TransactionCategory.TRANSFER -> PROVIDER
                TransactionCategory.FOOD, TransactionCategory.SNACKS, TransactionCategory.GROCERIES -> FOODIE
                TransactionCategory.TRANSPORT -> MOVER
                TransactionCategory.BILLS, TransactionCategory.HOUSE -> STEADY_HAND
                TransactionCategory.HOME_PROJECT -> BUILDER
                TransactionCategory.SHOPPING, TransactionCategory.BEAUTY, TransactionCategory.ELECTRONICS -> TRENDSETTER
                TransactionCategory.EDUCATION -> LEARNER
                TransactionCategory.CHURCH -> GIVER
                TransactionCategory.HEALTH -> CARETAKER
                TransactionCategory.BUSINESS -> ENTREPRENEUR
                TransactionCategory.AIRTIME -> CONNECTOR
                else -> ALL_ROUNDER
            }
        }
    }
}

/**
 * Builds a [YearRecap] from a year's transactions. Spending, income and fees are counted the way
 * Home counts them: money moved between the user's own accounts and transactions they left out of
 * their totals are not spending or income.
 */
class YearRecapCalculator(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) {
    /** Null when Trackitt has no transactions in [year]. */
    fun recap(year: Int, transactions: List<Transaction>, lastYear: List<Transaction> = emptyList()): YearRecap? {
        val inYear = transactions.filter { dayOf(it).year == year }
        if (inYear.isEmpty()) return null

        val from = inYear.minOf { dayOf(it) }
        // The current year runs to today, so quiet days since the last payment still count.
        val lastDay = inYear.maxOf { dayOf(it) }
        val to = if (year == today().year) maxOf(lastDay, today()) else lastDay
        val trackedDays = ChronoUnit.DAYS.between(from, to).toInt() + 1

        val payments = inYear.filter(::isSpending)
        val spent = payments.sumOf { it.amount }
        val received = inYear.filter { it.isIncome && !it.isOwnTransfer && !it.isExcluded }.sumOf { it.amount }
        val withFees = inYear.filter { !it.isIncome && !it.isExcluded && it.fee > 0 }

        val categories = payments
            .filter { it.category != TransactionCategory.OTHER }
            .groupBy { it.category }
            .map { (category, list) ->
                val total = list.sumOf { it.amount }
                CategoryShare(category, total, list.size, if (spent > 0) total / spent else 0.0)
            }
            .sortedByDescending { it.total }

        val byPayee = payments.groupBy { payeeKey(it) }.values.map { list ->
            PayeeTotal(payeeName(list.maxBy { it.date }), list.sumOf { it.amount }, list.size)
        }
        val regular = byPayee.maxWithOrNull(compareBy<PayeeTotal> { it.count }.thenBy { it.total })
            ?.takeIf { it.count >= MIN_REGULAR }

        val spendingDays = payments.map(::dayOf).toSet()
        val byMonth = payments.groupBy { dayOf(it).monthValue }
        val months = (from.monthValue..to.monthValue).map { m ->
            val first = LocalDate.of(year, m, 1)
            val last = first.plusMonths(1).minusDays(1)
            // A first or last payment a few quiet days into or before the month's edge still covers it.
            val complete = !from.isAfter(first.plusDays(EDGE_DAYS)) && !to.isBefore(last.minusDays(EDGE_DAYS)) &&
                // The month still under way isn't over, even on its last day.
                !(year == today().year && m == today().monthValue)
            MonthTotal(m, byMonth[m].orEmpty().sumOf { it.amount }, complete)
        }

        val lastYearRecap = lastYear.takeIf { it.isNotEmpty() }?.let { recap(year - 1, it) }
        val perDayChange = lastYearRecap
            ?.takeIf { it.trackedDays >= MIN_DAYS_TO_COMPARE && it.payments >= MIN_PAYMENTS_TO_COMPARE && it.perDay > 0 }
            ?.let { last -> (spent / trackedDays) / last.perDay - 1 }
            ?.takeIf { trackedDays >= MIN_DAYS_TO_COMPARE }

        return YearRecap(
            year = year,
            from = from,
            to = to,
            spent = spent,
            payments = payments.size,
            payees = byPayee.size,
            received = received,
            fees = withFees.sumOf { it.fee },
            feePayments = withFees.size,
            categories = categories,
            topPayees = byPayee.sortedByDescending { it.total }.take(TOP_PAYEES),
            regular = regular,
            biggest = payments.maxByOrNull { it.amount },
            months = months,
            busiestWeekday = payments.groupingBy { dayOf(it).dayOfWeek }.eachCount().maxByOrNull { it.value }?.key,
            lateNightPayments = payments.count { hourOf(it).let { h -> h >= 22 || h < 5 } },
            noSpendDays = (trackedDays - spendingDays.size).coerceAtLeast(0),
            trackedDays = trackedDays,
            perDayChange = perDayChange,
            personality = Personality.of(categories),
            ongoing = year == today().year,
        )
    }

    /**
     * Years with enough in them for a recap to say something true, latest first. A few stray
     * messages, or the first days of a new year, would make a story out of noise.
     */
    fun readyYears(activity: List<YearActivity>): List<Int> = activity
        .filter { isReady(it.year, dayOf(it.first), dayOf(it.last), it.payments) }
        .map { it.year }

    fun isReady(year: Int, first: LocalDate, last: LocalDate, payments: Int): Boolean {
        val to = if (year == today().year) maxOf(last, today()) else last
        val trackedDays = ChronoUnit.DAYS.between(first, to) + 1
        return trackedDays >= MIN_DAYS_TO_RECAP && payments >= MIN_PAYMENTS_TO_RECAP
    }

    private fun isSpending(t: Transaction) = !t.isIncome && !t.isOwnTransfer && !t.isExcluded

    private fun dayOf(t: Transaction): LocalDate = dayOf(t.date)

    private fun dayOf(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun hourOf(t: Transaction): Int = Instant.ofEpochMilli(t.date).atZone(zone).hour

    private fun payeeKey(t: Transaction) = (t.payeeNumber ?: t.merchant).trim().lowercase()

    /** The payee as the user knows them: their name, or a bare number without the network's label. */
    private fun payeeName(t: Transaction) = PayeeNumber.of(t.merchant) ?: t.merchant.trim()

    companion object {
        const val TOP_PAYEES = 5
        const val MIN_REGULAR = 3
        const val MIN_DAYS_TO_COMPARE = 30
        const val MIN_PAYMENTS_TO_COMPARE = 20
        const val EDGE_DAYS = 6L
        const val MIN_DAYS_TO_RECAP = 30
        const val MIN_PAYMENTS_TO_RECAP = 20
    }
}
