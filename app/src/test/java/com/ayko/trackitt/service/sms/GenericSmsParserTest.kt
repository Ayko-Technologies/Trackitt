package com.ayko.trackitt.service.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericSmsParserTest {

    private val parser = SmsTransactionParser()
    private val smsTimestamp = 1786000000000L

    @Test
    fun parses_ghanapay_transfer_to_a_wallet_number() {
        val sms = "You have transferred GHS 6000.00 to 0240000001 MTN MOBILE MONEY. Ref: Rent. Transaction ID: 621300000001. Balance: GHS 3900.00."

        val parsed = parser.parse("GhanaPay", sms, smsTimestamp)

        assertNotNull(parsed)
        assertEquals(6000.0, parsed!!.amount, 0.001)
        assertFalse(parsed.isIncome)
        assertEquals("0240000001 (MTN MOBILE MONEY)", parsed.merchant)
        assertEquals("Rent", parsed.reference)
        assertEquals("ghanapay_621300000001", parsed.transactionId)
        assertEquals("GhanaPay", parsed.source)
    }

    @Test
    fun parses_ghanapay_money_received_from_a_named_person() {
        val sms = "You have received GHS 2500.00 from 0240000002, KOFI MENSAH. Reference: upkeep . Transaction ID: 621300000002. Transaction Date: 01-AUG-2026. Balance: GHS 2500.00"

        val parsed = parser.parse("GhanaPay", sms, smsTimestamp)

        assertNotNull(parsed)
        assertTrue(parsed!!.isIncome)
        assertEquals(2500.0, parsed.amount, 0.001)
        assertEquals("KOFI MENSAH", parsed.merchant)
        assertEquals("upkeep", parsed.reference)
    }

    @Test
    fun reads_amount_written_after_the_currency_or_before_it() {
        val after = "Y'ello you have received 25.00 GHS from AMA OWUSU. Your new balance: 134.45 GHS. Transaction ID: 7700000001."
        val before = "Y'ello you have received GH¢25.00 from AMA OWUSU. Your new balance: GH¢134.45. Transaction ID: 7700000001."

        assertEquals(25.0, parser.parse("NewWallet", after, smsTimestamp)!!.amount, 0.001)
        assertEquals(25.0, parser.parse("NewWallet", before, smsTimestamp)!!.amount, 0.001)
    }

    @Test
    fun separates_amount_balance_and_fee() {
        val sms = "0000010000000001 Confirmed. GHS200.00 sent to 0550000003 YAW BOATENG on MTN MOBILE MONEY on 2025-08-15 at 18:46:31. Your Telecel Cash balance is GHS250.00. You were charged GHS1.50. Your E-levy charge is GHS0.00."

        val parsed = parser.parse("T-CASH", sms, smsTimestamp)

        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount, 0.001)
        assertEquals(1.5, parsed.fee, 0.001)
        assertEquals("YAW BOATENG", parsed.merchant)
        assertEquals("Telecel Cash", parsed.source)
    }

    @Test
    fun parses_bank_alert_with_credit_marker_and_description() {
        val sms = "Alert\nAmount:GHS100.00Cr\nAcc No: 043******0001\nDate: 04-08-2026,15:59\nDesc: CASHDEP ACME LTD ,ACCRA BR - 043\nAvail Bal:GHS100.00"

        val parsed = parser.parse("SomeBank", sms, smsTimestamp)

        assertNotNull(parsed)
        assertTrue(parsed!!.isIncome)
        assertEquals(100.0, parsed.amount, 0.001)
        assertEquals("CASHDEP ACME LTD", parsed.merchant)
        assertEquals("SomeBank", parsed.source)
    }

    @Test
    fun parses_debit_marker() {
        val sms = "Acct: ****1234 Amt: GHS 45.50 DR Desc: POS SHOPRITE ACCRA MALL Avail Bal: GHS 1,204.10"

        val parsed = parser.parse("ANYBANK", sms, smsTimestamp)

        assertNotNull(parsed)
        assertFalse(parsed!!.isIncome)
        assertEquals(45.5, parsed.amount, 0.001)
        assertEquals("POS SHOPRITE ACCRA MALL", parsed.merchant)
    }

    @Test
    fun parses_reversal_as_income_with_its_own_id() {
        val sms = "Hello KOFI MENSAH, transaction with ID 624600000001, GHS 300 at: 03-09-2026 09:00:00 was reversed successfully. Wallet Balance: GHS 450.00"

        val parsed = parser.parse("GhanaPay", sms, smsTimestamp)

        assertNotNull(parsed)
        assertTrue(parsed!!.isIncome)
        assertEquals("Reversal", parsed.merchant)
        assertEquals("ghanapay_rev_624600000001", parsed.transactionId)
    }

    @Test
    fun keeps_refund_even_when_reference_mentions_a_failure() {
        val sms = "Payment received for GHS 4.50 from MTN- DISCOUNT 2, Date: 2021-10-01 12:00:00. Reference: Being Refund Payment for FAILED APPS PURCHASE. Current Bal:GHS Available Bal: 50.00. Transaction Id: 14100000001. TRANSACTION FEE IS 0"

        val parsed = parser.parse("MobileMoney", sms, smsTimestamp)

        assertNotNull(parsed)
        assertTrue(parsed!!.isIncome)
        assertEquals(4.5, parsed.amount, 0.001)
    }

    @Test
    fun parses_old_ecobank_format_without_ref() {
        val sms = "GHS 5.00 debited from ac xxxx0001 POS PURCHASE (ON-US)-ExpressPay Gh          Accra          GH on 31-Mar-20. Your Available Balance is GHS 400.00"

        val parsed = parser.parse("ECOBANK", sms, smsTimestamp)

        assertNotNull(parsed)
        assertFalse(parsed!!.isIncome)
        assertEquals(5.0, parsed.amount, 0.001)
        assertEquals("ExpressPay Gh", parsed.merchant)
        assertTrue(parsed.transactionId.startsWith("eco_h"))
    }

    @Test
    fun parses_ecobank_amount_without_leading_zero() {
        val sms = "GHS .11 Debited from ac 144******001 REF:H28TEST00000001 CHARGE REF:500000001  VODAFONE_CASH MOBILE NO 233200000001 on 13-MAR-23. Your Available Balance is GHS 100.00. Do not respond to unsolicited SMS and emails."

        val parsed = parser.parse("ECOBANK", sms, smsTimestamp)

        assertNotNull(parsed)
        assertEquals(0.11, parsed!!.amount, 0.0001)
        assertEquals("eco_H28TEST00000001", parsed.transactionId)
    }

    @Test
    fun routes_every_vodafone_cash_sender_name_to_telecel() {
        val sms = "0000000500000001 Confirmed. You have received GHS5.00 from MTN MOBILE MONEY with transaction reference 977071 on 2020-03-30 at 16:30:50. Your Vodafone Cash balance is GHS5.35."

        for (sender in listOf("VF-CASH", "VFCash", "VF Cash", "T Cash", "T-CASH")) {
            val parsed = parser.parse(sender, sms, smsTimestamp)
            assertEquals(sender, "tc_0000000500000001", parsed?.transactionId)
            assertEquals(sender, "Telecel Cash", parsed?.source)
        }
    }

    @Test
    fun ignores_promotions_that_mention_amounts() {
        val promos = listOf(
            "Get 1.45GB at GHs4.5 only on Telecel Cash. Dial *110# now and select options 3,3 to enjoy.",
            "Your balance is low! Get SOS Credit of GHs 5.00 and pay back on your next recharge. Reply \"A\" to this SMS.",
            "Congrats! you have purchased a Mashup of Ghc5 to be used for all networks. Dial *550# to check balance.",
            "We have 20,000 GHC to give you!! Reply SUB for a chance to win!",
        )
        promos.forEach { assertNull(it, parser.parse("Promo", it, smsTimestamp)) }
    }

    @Test
    fun ignores_failures_codes_and_future_debits() {
        val messages = listOf(
            "Transaction failed. You do not have enough money in your Telecel Cash account to perform this transaction. Your Telecel Cash balance is GHS14.97.",
            "Enter code 155867 to pay GHS 100.00 to ACME Ltd. DO NOT SHARE with anyone.",
            "QWIKLOAN: GHS 21.38 will be deducted from your account today to repay your loan.",
            "Y'ello Valued customer, the transfer of GHS 100.00 to 233550000001 has been withheld and is pending reversal within 15 days.",
        )
        messages.forEach { assertNull(it, parser.parse("Anything", it, smsTimestamp)) }
    }

    @Test
    fun ignores_merchant_receipts_already_reported_by_the_wallet() {
        val receipts = listOf(
            "Hello KOFI you have successfully received GHS 400.00 from Kofi Mensah via INSTNTMNY. Time:2022-10-10 10:25:00 Powered by Zeepay. Ref 903500001",
            "You have successfully paid GHS 150.00 to Shell Ring Road, in reference to 12. Ref #: 5500000000001",
            "GHS85 has been debited from your mtn account as payment to Acme Store Osu. Charges GHS 0.85. Reference: COL100000000001.",
        )
        receipts.forEach { assertNull(it, parser.parse("Merchant", it, smsTimestamp)) }
    }

    @Test
    fun ignores_transaction_lookalikes_from_personal_numbers() {
        val sms = "You have received GHS 500.00 from MTN MOBILE MONEY. Your new balance: GHS 512.00. Transaction ID: 9900000001."

        assertNull(parser.parse("+233240000009", sms, smsTimestamp))
        assertNull(parser.parse("0240000009", sms, smsTimestamp))
        assertNotNull(parser.parse("1355", sms, smsTimestamp))
    }

    @Test
    fun uses_the_date_in_the_message_when_the_sms_arrived_late() {
        val sms = "You have received GHS 10.00 from AMA OWUSU on your mobile money account at 2026-07-01 10:00:00. Your new balance: GHS 20.00. Financial Transaction Id: 5500000001."
        val arrivedTwoDaysLater = 1782986400000L + 2 * 24 * 60 * 60 * 1000

        val parsed = parser.parse("NewWallet", sms, arrivedTwoDaysLater)

        assertNotNull(parsed)
        assertTrue(parsed!!.date < arrivedTwoDaysLater)
    }

    @Test
    fun same_message_always_gets_the_same_id() {
        val sms = "Dear customer, you have received GHS0.40 from Telecel Cash as interest earned on your mobile money wallet. Your new balance is GHS80.00."

        val first = parser.parse("T-CASH", sms, smsTimestamp)
        val second = parser.parse("T-CASH", sms, smsTimestamp + 5_000)

        assertNotNull(first)
        assertEquals("Interest", first!!.merchant)
        assertEquals(first.transactionId, second!!.transactionId)
    }

    @Test
    fun parses_at_money_formats() {
        val sent = "Dear Customer, you have sent GHS 20.00 to KOFI MENSAH, mobile money wallet 0270000001. Charge: GHS 0.20 ELevy:GHS 0.00. Your Curr Bal: GHS 100.00, Avail Bal: GHS 100.00. Trans ID: GM260328.1028.C04014, GHIPSS ID:400270392806"
        val received = "Dear Customer, you have received GH¢ 50.00 from AMA OWUSU 0240000001 in reference to school fees. Your Curr Bal:GH¢ 70.00, Avail Bal: GH¢ 70.00 Trans ID: GW230805.1447.A06574, GHIPSS ID:02687891433"
        val paid = "You have paid GHS5.00 to DATA in reference to 111MB. Your new balance is GHS15.00. Trans ID: MM260401.1914.D05432 E-Levy:GHS 0.00"

        val s = parser.parse("ATMoney", sent, smsTimestamp)!!
        assertFalse(s.isIncome)
        assertEquals(20.0, s.amount, 0.001)
        assertEquals(0.2, s.fee, 0.001)
        assertEquals("KOFI MENSAH", s.merchant)
        assertEquals("atmoney_GM260328.1028.C04014", s.transactionId)
        assertEquals("AT Money", s.source)

        val r = parser.parse("ATMoney", received, smsTimestamp)!!
        assertTrue(r.isIncome)
        assertEquals(50.0, r.amount, 0.001)
        assertEquals("AMA OWUSU", r.merchant)
        assertEquals("school fees", r.reference)

        val p = parser.parse("ATMoney", paid, smsTimestamp)!!
        assertEquals(5.0, p.amount, 0.001)
        assertEquals("DATA", p.merchant)
    }

    @Test
    fun parses_field_style_alert_with_direction_on_first_line() {
        val sms = "Debit\nAmt:GHS50.00\nAcc:003******915\nDesc:STAMP DUTY FOR 1 TXNS\nDate:25/07/2026\nAvail Bal:GHS1,546.86"

        val parsed = parser.parse("SomeBank", sms, smsTimestamp)

        assertNotNull(parsed)
        assertFalse(parsed!!.isIncome)
        assertEquals(50.0, parsed.amount, 0.001)
        assertEquals("STAMP DUTY FOR 1 TXNS", parsed.merchant)
    }
}
