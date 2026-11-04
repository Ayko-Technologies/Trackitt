package com.ayko.trackitt.service.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MoMoSmsParserTest {

    private val parser = MoMoSmsParser()

    @Test
    fun parses_payment_for_merchant_without_bills_marker() {
        val sms = "Payment for GHS60.90 to Zen Online .Current Balance: GHS 29.46. Transaction Id: 80000000011. Fee charged: GHS0.00,Tax Charged 0.Download the MoMo App for a Faster & Easier Experience."

        val parsed = parser.parse(sms)

        assertNotNull(parsed)
        assertEquals(60.90, parsed!!.amount, 0.01)
        assertEquals("Zen Online", parsed.merchant)
        assertEquals("80000000011", parsed.transactionId)
        assertEquals(0.0, parsed.fee, 0.01)
    }

    @Test
    fun parses_payment_for_zenith_pos_without_bills_marker() {
        val sms = "Payment for GHS631.15 to Zenith MoMo POS .Current Balance: GHS 104.36. Transaction Id: 70000000010. Fee charged: GHS0.00,Tax Charged 0.Download the MoMo App for a Faster & Easier Experience."
        val smsTimestamp = 1776763962636L

        val parsed = parser.parse(sms, smsTimestamp)

        assertNotNull(parsed)
        assertEquals(631.15, parsed!!.amount, 0.01)
        assertEquals("Zenith MoMo POS", parsed.merchant)
        assertEquals("70000000010", parsed.transactionId)
        assertEquals(smsTimestamp, parsed.date)
    }

    @Test
    fun still_parses_bill_payments() {
        val sms = "Payment for GHS4510.00 to Bills.INV ..Current Balance: GHS 22.96. Transaction Id: 70000000003. Fee charged: GHS0.00,Tax Charged 0."

        val parsed = parser.parse(sms)

        assertNotNull(parsed)
        assertEquals("Bills.INV", parsed!!.merchant)
        assertEquals("70000000003", parsed.transactionId)
    }
}
