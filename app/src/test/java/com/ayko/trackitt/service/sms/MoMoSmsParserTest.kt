package com.ayko.trackitt.service.sms

import org.junit.Assert.*
import org.junit.Test

class MoMoSmsParserTest {

    private val parser = MoMoSmsParser()

    @Test
    fun `parse payment made P2P`() {
        val sms = "Payment made for GHS 355.00 to REBECCA NUETEY Current Balance: GHS 13.06 . Available Balance: GHS 13.06. Reference: . Transaction ID: 79917286657. Fee charged: GHS2.66 Tax charged: 0. Download the MoMo App for a Faster & Easier Experience. Click here: https://mtnmymomo.onelink.me/XJOt/MoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(355.00, result!!.amount, 0.01)
        assertEquals(2.66, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("REBECCA NUETEY", result.merchant)
        assertEquals("79917286657", result.transactionId)
    }

    @Test
    fun `parse payment received`() {
        val sms = "Payment received for GHS 365.00 from Express Pay ova  Current Balance: GHS 370.72 . Available Balance: GHS 370.72. Reference: expressPay. Transaction ID: 79917181506. TRANSACTION FEE: 0.00"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(365.00, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Express Pay ova", result.merchant)
        assertEquals("79917181506", result.transactionId)
        assertEquals("expressPay", result.reference)
    }

    @Test
    fun `parse merchant payment yello`() {
        val sms = "Y'ello. You have Paid GHS 85.47 to Merchant 032234 on your mobile money account at 202623110051. Message from sender: melcom. Your new balance: GHS 240.72 . Fee was GHS 0.85 . Financial Transaction Id: 79851134041.Download the MoMo App for a Faster & Easier Experience Click here: http://mtnghana.app.link/nsBnhItDoob"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(85.47, result!!.amount, 0.01)
        assertEquals(0.85, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("melcom", result.merchant)
        assertEquals("79851134041", result.transactionId)
    }

    @Test
    fun `parse merchant payment confirmation`() {
        val sms = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 79851134041. External Transaction Id: -.Download the MoMo App for a Faster & Easier Experience Click here: https://bit.ly/downloadMyMoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(85.47, result!!.amount, 0.01)
        assertEquals("MELCOM COMMUNITY 25", result.merchant)
        assertEquals("79851134041", result.transactionId)
        assertEquals("melcom", result.reference)
    }

    @Test
    fun `parse cash out`() {
        val sms = "Cash Out made for GHS200.00 to VICTORIOUS EUNILUV VENTURES. Current Balance: GHS38.72 Financial Transaction Id: 79851433208. Cash-out fee is charged automatically from your MTN MoMo wallet. Please do not pay any fees to the Agent. Thank you for using MTN MobileMoney. Fee charged: GHS2.00."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(200.00, result!!.amount, 0.01)
        assertEquals(2.00, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("VICTORIOUS EUNILUV VENTURES", result.merchant)
        assertEquals("79851433208", result.transactionId)
    }

    @Test
    fun `parse bank transfer received`() {
        val sms = "You have received GHS 400.00 from Ecobank ova on your mobile money account at 2026-04-23 11:30:29. Message from sender: . Your new balance:GHS 408.72. Financial Transaction Id: 79852151833."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(400.00, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Ecobank ova", result.merchant)
        assertEquals("79852151833", result.transactionId)
    }

    @Test
    fun `parse cash transfer received`() {
        val sms = "Cash transfer recieved: You have received 344.08 GHS in a cash transfer from 17000000003 17000000003 through agent Bigpay Ghana Limited - 233559990091 on your mobile money account at 2026-04-23 10:38:07. Message from sender: TransactionId - 2242353910809367129-n8NrjBgl. Your new balance: 367.04 GHS. Fee was 0.00 GHS. Financial Transaction Id: 79849724169."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(344.08, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Bigpay Ghana Limited", result.merchant)
        assertEquals("79849724169", result.transactionId)
    }

    @Test
    fun `parse bill payment`() {
        val sms = "Payment for GHS4510.00 to Bills.INV ..Current Balance: GHS 22.96. Transaction Id: 79799129949. Fee charged: GHS0.00,Tax Charged 0.Download the MoMo App for a Faster & Easier Experience. Click here: https://bit.ly/downloadMyMoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(4510.00, result!!.amount, 0.01)
        assertFalse(result.isIncome)
        assertEquals("Bills.INV", result.merchant)
        assertEquals("79799129949", result.transactionId)
    }

    @Test
    fun `parse returns null for non-MoMo SMS`() {
        val sms = "Your OTP is 123456. Do not share this code."
        val result = parser.parse(sms)
        assertNull(result)
    }
}
