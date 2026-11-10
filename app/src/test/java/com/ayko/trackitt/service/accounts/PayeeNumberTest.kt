package com.ayko.trackitt.service.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayeeNumberTest {

    @Test
    fun reads_the_number_a_wallet_gave_instead_of_a_name() {
        assertEquals("0240000001", PayeeNumber.of("0240000001 (MTN MOBILE MONEY)"))
        assertEquals("0240000001", PayeeNumber.of("233240000001 (TELECEL CASH)"))
        assertEquals("0240000001", PayeeNumber.of("+233240000001"))
    }

    @Test
    fun ignores_names_masked_numbers_and_merchant_codes() {
        assertNull(PayeeNumber.of("ROAD TEC DRIVING ACADEMY"))
        assertNull(PayeeNumber.of("2335****150 , MTN MOBILE MONEY"))
        assertNull(PayeeNumber.of("123456 - KOFI MENSAH"))
    }
}
