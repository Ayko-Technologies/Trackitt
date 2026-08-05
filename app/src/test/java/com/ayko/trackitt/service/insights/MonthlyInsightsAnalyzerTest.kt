package com.ayko.trackitt.service.insights

import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyInsightsAnalyzerTest {

    private val analyzer = MonthlyInsightsAnalyzer()
    private val fallbackComposer = MonthlyInsightsFallbackComposer()

    @Test
    fun analyze_computes_totals_and_rankings() {
        val snapshot = analyzer.analyze(
            year = 2026,
            month = 4,
            transactions = sampleTransactions(),
        )

        assertEquals(470.0, snapshot.spend, 0.001)
        assertEquals(10.0, snapshot.fees, 0.001)
        assertEquals(300.0, snapshot.income, 0.001)
        assertEquals(-180.0, snapshot.net, 0.001)
        assertEquals(TransactionCategory.HOME_PROJECT, snapshot.topCategories.first().category)
        assertEquals("Bills.INV", snapshot.topMerchants.first().merchant)
        assertEquals(120.0, snapshot.uncategorizedSpend, 0.001)
    }

    @Test
    fun fallback_mentions_biggest_levers() {
        val snapshot = analyzer.analyze(
            year = 2026,
            month = 4,
            transactions = sampleTransactions(),
        )

        val narrative = fallbackComposer.compose(snapshot)

        assertTrue(narrative.headline.contains("April 2026"))
        assertTrue(narrative.whereMoneyWent.contains("Bills.INV"))
        assertTrue(narrative.cutBack.contains("Home Project") || narrative.cutBack.contains("Bills.INV"))
        assertTrue(narrative.saveMore.contains("fees", ignoreCase = true) || narrative.saveMore.contains("budget", ignoreCase = true))
    }

    @Test
    fun fingerprint_changes_when_transactions_change() {
        val original = analyzer.analyze(
            year = 2026,
            month = 4,
            transactions = sampleTransactions(),
        )
        val changed = analyzer.analyze(
            year = 2026,
            month = 4,
            transactions = sampleTransactions().map {
                if (it.merchant == "ePayment") it.copy(note = "updated") else it
            },
        )

        assertTrue(original.fingerprint.isNotBlank())
        assertTrue(changed.fingerprint.isNotBlank())
        assertTrue(original.fingerprint != changed.fingerprint)
    }

    @Test
    fun analyze_can_exclude_transfers_from_spend() {
        val snapshot = analyzer.analyze(
            year = 2026,
            month = 4,
            transactions = sampleTransactions() + Transaction(
                amount = 200.0,
                fee = 3.0,
                category = TransactionCategory.TRANSFER,
                merchant = "Zenith MoMo POS",
                date = 7L,
            ),
            excludeTransfers = true,
        )

        assertEquals(470.0, snapshot.spend, 0.001)
        assertEquals(6.0, snapshot.fees, 0.001)
        assertTrue(snapshot.topCategories.none { it.category == TransactionCategory.TRANSFER })
        assertTrue(snapshot.topMerchants.none { it.merchant == "Zenith MoMo POS" })
    }

    private fun sampleTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                amount = 250.0,
                fee = 4.0,
                category = TransactionCategory.HOME_PROJECT,
                merchant = "Bills.INV",
                note = "AC payment",
                date = 1L,
            ),
            Transaction(
                amount = 120.0,
                fee = 2.0,
                category = TransactionCategory.OTHER,
                merchant = "ePayment",
                date = 2L,
            ),
            Transaction(
                amount = 60.0,
                fee = 0.0,
                category = TransactionCategory.FOOD,
                merchant = "RUTH NIMOTHY",
                note = "jollof",
                date = 3L,
            ),
            Transaction(
                amount = 40.0,
                fee = 0.0,
                category = TransactionCategory.CHURCH,
                merchant = "ICGC DAYSPRING",
                date = 4L,
            ),
            Transaction(
                amount = 300.0,
                isIncome = true,
                category = TransactionCategory.INCOME,
                merchant = "Salary",
                date = 5L,
            ),
            Transaction(
                amount = 0.0,
                fee = 4.0,
                category = TransactionCategory.TRANSFER,
                merchant = "Transfer Fees",
                date = 6L,
            ),
        )
    }
}
