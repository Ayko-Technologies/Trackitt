package com.ayko.trackitt.service.accounts

import com.ayko.trackitt.data.local.entity.SmsReceiptEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountMatchersTest {

    private val minute = 60_000L
    private val start = 1_786_000_000_000L
    private var nextId = 1L

    private fun tx(
        source: String,
        amount: Double,
        isIncome: Boolean,
        merchant: String,
        at: Long = start,
        balance: Double? = null,
        fee: Double = 0.0,
    ) = TransactionEntity(
        id = nextId++, amount = amount, fee = fee, isIncome = isIncome, category = "OTHER",
        merchant = merchant, reference = null, transactionId = "t$nextId", source = source,
        note = null, date = at, isFromSms = true, rawSms = "", balance = balance,
    )

    @Test
    fun pairs_a_wallet_to_wallet_move_that_names_the_other_network() {
        val out = tx("GhanaPay", 6000.0, false, "0240000001 (MTN MOBILE MONEY)")
        val into = tx("MTN MoMo", 6000.0, true, "KOFI MENSAH", at = start + 6 * minute)

        assertTrue(OwnTransferMatcher.isPair(out, into))
        assertEquals(listOf(out.id to into.id), OwnTransferMatcher.findPairs(listOf(into, out)))
    }

    @Test
    fun pairs_near_simultaneous_moves_even_without_names() {
        val out = tx("Telecel Cash", 100.0, false, "KOFI MENSAH")
        val into = tx("MTN MoMo", 100.0, true, "AMA OWUSU", at = start + 30_000)

        assertTrue(OwnTransferMatcher.isPair(out, into))
    }

    @Test
    fun does_not_pair_unrelated_payments_minutes_apart() {
        val paidSomeone = tx("MTN MoMo", 50.0, false, "YAW BOATENG")
        val friendSentMoney = tx("Telecel Cash", 50.0, true, "AMA OWUSU", at = start + 8 * minute)

        assertFalse(OwnTransferMatcher.isPair(paidSomeone, friendSentMoney))
    }

    @Test
    fun does_not_pair_different_amounts_same_wallet_or_far_apart() {
        val out = tx("GhanaPay", 100.0, false, "MTN MOBILE MONEY")
        assertFalse(OwnTransferMatcher.isPair(out, tx("MTN MoMo", 99.0, true, "X", at = start + minute)))
        assertFalse(OwnTransferMatcher.isPair(out, tx("GhanaPay", 100.0, true, "X", at = start + minute)))
        assertFalse(OwnTransferMatcher.isPair(out, tx("MTN MoMo", 100.0, true, "X", at = start + 30 * minute)))
    }

    @Test
    fun respects_a_pair_the_user_dismissed() {
        val out = tx("GhanaPay", 100.0, false, "MTN MOBILE MONEY").copy(ownTransferDismissed = true)
        val into = tx("MTN MoMo", 100.0, true, "GHANAPAY PUSH", at = start + minute)

        assertFalse(OwnTransferMatcher.isPair(out, into))
    }

    @Test
    fun uses_each_transaction_in_one_pair_only() {
        val out = tx("GhanaPay", 20.0, false, "MTN MOBILE MONEY")
        val firstIn = tx("MTN MoMo", 20.0, true, "GHANAPAY PUSH", at = start + minute)
        val secondIn = tx("MTN MoMo", 20.0, true, "GHANAPAY PUSH", at = start + 2 * minute)

        assertEquals(listOf(out.id to firstIn.id), OwnTransferMatcher.findPairs(listOf(out, firstIn, secondIn)))
    }

    @Test
    fun matches_receipt_to_wallet_payment_through_a_payment_service() {
        val wallet = tx("MTN MoMo", 156.55, false, "JUNIPAY")
        val receipt = SmsReceiptEntity(1, "JuniPay", 155.0, "Acme Store Osu", start + 5 * minute, "h")

        assertEquals(wallet, ReceiptMatcher.bestTransaction(receipt, listOf(wallet)))
    }

    @Test
    fun does_not_rename_a_named_merchant_or_mismatched_amount() {
        val named = tx("MTN MoMo", 151.5, false, "KOFI MENSAH")
        val service = tx("MTN MoMo", 400.0, false, "VPOS")
        val receipt = SmsReceiptEntity(1, "Shell", 151.5, "Shell Ring Road", start, "h")

        assertNull(ReceiptMatcher.bestTransaction(receipt, listOf(named, service)))
        assertFalse(ReceiptMatcher.isIntermediary("JOSEPH APAYA"))
        assertTrue(ReceiptMatcher.isIntermediary("Slydepay ova"))
        assertTrue(ReceiptMatcher.isIntermediary("ZeepayCOT"))
    }

    @Test
    fun keeps_the_merchant_name_when_its_receipt_names_only_a_branch() {
        fun receipt(sender: String, party: String) = SmsReceiptEntity(1, sender, 10.0, party, start, "h")

        assertEquals("CityPharm (Osu)", ReceiptMatcher.displayName(receipt("CityPharm", "Osu")))
        assertEquals("Shell Ring Road", ReceiptMatcher.displayName(receipt("Shell", "Shell Ring Road")))
        assertEquals("Acme Unit Trust", ReceiptMatcher.displayName(receipt("Zeepay", "Acme Unit Trust")))
    }

    @Test
    fun reports_latest_balance_and_finds_a_missed_sms() {
        val transactions = listOf(
            tx("MTN MoMo", 100.0, true, "A", at = start, balance = 100.0),
            tx("MTN MoMo", 30.0, false, "B", at = start + minute, balance = 69.5, fee = 0.5),
            // 20 left the wallet here without an SMS.
            tx("MTN MoMo", 10.0, false, "C", at = start + 2 * minute, balance = 39.5),
            tx("GhanaPay", 5.0, true, "D", at = start + 3 * minute, balance = 5.0),
        )

        val balances = BalanceReconciler.reconcile(transactions).associateBy { it.source }

        val momo = balances.getValue("MTN MoMo")
        assertEquals(39.5, momo.balance, 0.001)
        assertEquals(1, momo.gaps.size)
        assertEquals(-20.0, momo.gaps.single().unexplained, 0.001)
        assertTrue(balances.getValue("GhanaPay").gaps.isEmpty())
    }
}
