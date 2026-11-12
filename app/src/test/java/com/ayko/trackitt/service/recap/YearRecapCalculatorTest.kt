package com.ayko.trackitt.service.recap

import com.ayko.trackitt.data.local.dao.YearActivity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.domain.model.TransactionCategory.FOOD
import com.ayko.trackitt.domain.model.TransactionCategory.INCOME
import com.ayko.trackitt.domain.model.TransactionCategory.OTHER
import com.ayko.trackitt.domain.model.TransactionCategory.TRANSFER
import com.ayko.trackitt.domain.model.TransactionCategory.TRANSPORT
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YearRecapCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val calculator = YearRecapCalculator(zone, today = { LocalDate.of(2027, 2, 1) })

    private fun tx(
        on: String,
        amount: Double,
        category: TransactionCategory = FOOD,
        merchant: String = "Ama's Kitchen",
        income: Boolean = false,
        ownTransfer: Boolean = false,
        excluded: Boolean = false,
        fee: Double = 0.0,
        payeeNumber: String? = null,
    ) = Transaction(
        amount = amount,
        fee = fee,
        isIncome = income,
        category = category,
        merchant = merchant,
        date = LocalDateTime.parse(on).toInstant(zone).toEpochMilli(),
        isOwnTransfer = ownTransfer,
        isExcluded = excluded,
        payeeNumber = payeeNumber,
    )

    @Test
    fun counts_spending_like_home_does() {
        val recap = calculator.recap(
            2026,
            listOf(
                tx("2026-03-02T09:00", 100.0, fee = 1.0),
                tx("2026-03-03T09:00", 50.0, TRANSPORT, merchant = "Kofi"),
                tx("2026-03-04T09:00", 500.0, INCOME, merchant = "Employer", income = true),
                tx("2026-03-05T09:00", 400.0, TRANSFER, merchant = "Own wallet", ownTransfer = true, fee = 3.0),
                tx("2026-03-06T09:00", 900.0, merchant = "Someone else's purchase", excluded = true),
            ),
        )!!
        assertEquals(150.0, recap.spent, 0.001)
        assertEquals(2, recap.payments)
        assertEquals(500.0, recap.received, 0.001)
        // A fee on a move between own accounts is still money gone.
        assertEquals(4.0, recap.fees, 0.001)
        assertEquals(2, recap.feePayments)
    }

    @Test
    fun a_year_tracked_from_april_starts_then_and_counts_quiet_days_from_there() {
        val recap = calculator.recap(
            2026,
            listOf(tx("2026-04-01T10:00", 10.0), tx("2026-04-03T10:00", 10.0), tx("2026-04-10T10:00", 10.0)),
        )!!
        assertEquals(LocalDate.of(2026, 4, 1), recap.from)
        assertEquals(LocalDate.of(2026, 4, 10), recap.to)
        assertEquals(10, recap.trackedDays)
        assertEquals(7, recap.noSpendDays)
        assertEquals(listOf(4), recap.months.map { it.month })
    }

    @Test
    fun the_current_year_runs_to_today() {
        val calculator = YearRecapCalculator(zone, today = { LocalDate.of(2026, 11, 30) })
        val recap = calculator.recap(2026, listOf(tx("2026-11-01T10:00", 10.0)))!!
        assertEquals(LocalDate.of(2026, 11, 30), recap.to)
        assertEquals(30, recap.trackedDays)
    }

    @Test
    fun only_whole_months_count_as_complete() {
        val calculator = YearRecapCalculator(zone, today = { LocalDate.of(2026, 11, 11) })
        val recap = calculator.recap(
            2026,
            listOf(
                tx("2026-04-03T10:00", 10.0), // a few quiet days in: April still counts
                tx("2026-06-20T10:00", 10.0),
                tx("2026-11-10T10:00", 10.0),
            ),
        )!!
        assertEquals(listOf(4, 5, 6, 7, 8, 9, 10, 11), recap.months.map { it.month })
        // November is still under way.
        assertEquals(listOf(4, 5, 6, 7, 8, 9, 10), recap.months.filter { it.complete }.map { it.month })

        val fromMidMonth = calculator.recap(2026, listOf(tx("2026-04-15T10:00", 10.0), tx("2026-06-20T10:00", 10.0)))!!
        assertEquals(false, fromMidMonth.months.first().complete)
    }

    @Test
    fun other_is_never_the_top_category() {
        val recap = calculator.recap(
            2026,
            listOf(tx("2026-05-01T10:00", 900.0, OTHER), tx("2026-05-02T10:00", 100.0, TRANSPORT)),
        )!!
        assertEquals(TRANSPORT, recap.topCategory!!.category)
        assertEquals(0.1, recap.topCategory!!.share, 0.001)
    }

    @Test
    fun groups_a_payee_by_number_and_shows_the_latest_name() {
        val recap = calculator.recap(
            2026,
            listOf(
                tx("2026-05-01T10:00", 20.0, merchant = "0240000001 (MTN MOBILE MONEY)", payeeNumber = "0240000001"),
                tx("2026-05-02T10:00", 30.0, merchant = "0240000001 (MTN MOBILE MONEY)", payeeNumber = "0240000001"),
                tx("2026-05-03T10:00", 40.0, merchant = "Asante Driving School", payeeNumber = "0240000001"),
                tx("2026-05-04T10:00", 5.0, merchant = "Kofi"),
            ),
        )!!
        assertEquals(PayeeTotal("Asante Driving School", 90.0, 3), recap.topPayees.first())
        assertEquals(2, recap.payees)
        assertEquals("Asante Driving School", recap.regular!!.name)
    }

    @Test
    fun an_unnamed_number_is_shown_without_the_networks_label() {
        val recap = calculator.recap(
            2026,
            listOf(tx("2026-05-01T10:00", 20.0, merchant = "0240000001 (MTN MOBILE MONEY)", payeeNumber = "0240000001")),
        )!!
        assertEquals("0240000001", recap.topPayees.single().name)
    }

    @Test
    fun a_regular_needs_three_payments() {
        val recap = calculator.recap(2026, listOf(tx("2026-05-01T10:00", 20.0), tx("2026-05-02T10:00", 20.0)))!!
        assertNull(recap.regular)
    }

    @Test
    fun finds_the_rhythm_of_the_week_and_late_nights() {
        val recap = calculator.recap(
            2026,
            listOf(
                tx("2026-05-02T23:30", 10.0), // Saturday
                tx("2026-05-09T12:00", 10.0), // Saturday
                tx("2026-05-04T02:00", 10.0), // Monday
            ),
        )!!
        assertEquals(DayOfWeek.SATURDAY, recap.busiestWeekday)
        assertEquals(2, recap.lateNightPayments)
    }

    @Test
    fun compares_spending_per_day_with_last_year() {
        // Last year: 20 a day for 60 days. This year: 30 a day for 60 days, though tracked from a later month.
        val lastYear = (0 until 60).map { tx(LocalDate.of(2025, 1, 1).plusDays(it.toLong()).toString() + "T10:00", 20.0) }
        val thisYear = (0 until 60).map { tx(LocalDate.of(2026, 4, 1).plusDays(it.toLong()).toString() + "T10:00", 30.0) }
        val recap = calculator.recap(2026, thisYear, lastYear)!!
        assertEquals(0.5, recap.perDayChange!!, 0.001)
    }

    @Test
    fun does_not_compare_with_a_barely_tracked_year() {
        val lastYear = listOf(tx("2025-12-30T10:00", 20.0), tx("2025-12-31T10:00", 20.0))
        val thisYear = (0 until 60).map { tx(LocalDate.of(2026, 1, 1).plusDays(it.toLong()).toString() + "T10:00", 30.0) }
        assertNull(calculator.recap(2026, thisYear, lastYear)!!.perDayChange)
    }

    @Test
    fun personality_follows_the_top_category() {
        val provider = calculator.recap(
            2026,
            listOf(tx("2026-05-01T10:00", 600.0, TransactionCategory.POCKET_MONEY), tx("2026-05-02T10:00", 400.0)),
        )!!
        assertEquals(Personality.PROVIDER, provider.personality)

        val spread = TransactionCategory.assignable
            .filter { it != OTHER && it != INCOME }
            .mapIndexed { i, c -> tx("2026-05-${(i % 28 + 1).toString().padStart(2, '0')}T10:00", 10.0, c) }
        assertEquals(Personality.ALL_ROUNDER, calculator.recap(2026, spread)!!.personality)
    }

    @Test
    fun a_year_needs_a_month_and_twenty_payments_for_a_recap() {
        val calculator = YearRecapCalculator(zone, today = { LocalDate.of(2026, 11, 12) })
        fun millis(day: String) = LocalDate.parse(day).atStartOfDay(zone).toInstant().toEpochMilli()
        val years = calculator.readyYears(
            listOf(
                YearActivity(2026, millis("2026-10-20"), millis("2026-11-01"), 25), // 24 days to today
                YearActivity(2025, millis("2025-01-01"), millis("2025-12-31"), 19), // too few payments
                YearActivity(2024, millis("2024-12-01"), millis("2024-12-31"), 20), // a month, just
                YearActivity(2023, millis("2023-03-01"), millis("2023-03-05"), 40), // a few days
            ),
        )
        assertEquals(listOf(2024), years)
        // The current year counts its quiet days up to today.
        assertEquals(true, calculator.isReady(2026, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), 20))
    }

    @Test
    fun only_the_current_year_is_so_far() {
        val calculator = YearRecapCalculator(zone, today = { LocalDate.of(2026, 11, 12) })
        assertEquals(true, calculator.recap(2026, listOf(tx("2026-05-01T10:00", 10.0)))!!.ongoing)
        assertEquals(false, calculator.recap(2025, listOf(tx("2025-05-01T10:00", 10.0)))!!.ongoing)
    }

    @Test
    fun no_recap_for_a_year_without_transactions() {
        assertNull(calculator.recap(2024, listOf(tx("2026-05-01T10:00", 10.0))))
        assertNotNull(calculator.recap(2026, listOf(tx("2026-05-01T10:00", 10.0))))
    }
}
