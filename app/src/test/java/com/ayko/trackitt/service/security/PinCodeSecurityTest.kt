package com.ayko.trackitt.service.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PinCodeSecurityTest {

    @Test
    fun hash_is_stable_for_same_pin_and_salt() {
        val hashOne = PinCodeSecurity.hashPin("1234", "salt")
        val hashTwo = PinCodeSecurity.hashPin("1234", "salt")

        assertEquals(hashOne, hashTwo)
    }

    @Test
    fun hash_changes_when_pin_changes() {
        val hashOne = PinCodeSecurity.hashPin("1234", "salt")
        val hashTwo = PinCodeSecurity.hashPin("9999", "salt")

        assertNotEquals(hashOne, hashTwo)
    }
}
