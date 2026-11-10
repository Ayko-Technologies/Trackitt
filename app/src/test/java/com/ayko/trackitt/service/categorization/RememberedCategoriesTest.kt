package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.domain.model.TransactionCategory.FOOD
import com.ayko.trackitt.domain.model.TransactionCategory.GROCERIES
import com.ayko.trackitt.domain.model.TransactionCategory.HEALTH
import com.ayko.trackitt.domain.model.TransactionCategory.OTHER
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RememberedCategoriesTest {

    private fun remembered(chosen: TransactionCategory?, recent: List<TransactionCategory>) =
        RememberedCategories(chosenFor = { chosen }, recentCategoriesOf = { _, count -> recent.take(count) })

    @Test
    fun a_business_keeps_the_category_chosen_for_it() = runBlocking {
        assertEquals(GROCERIES, remembered(GROCERIES, emptyList()).of("melcom accra mall"))
        assertEquals(GROCERIES, remembered(GROCERIES, emptyList()).of("kofi mensah ventures"))
    }

    @Test
    fun a_person_gets_a_category_only_after_a_run_of_the_same_one() = runBlocking {
        assertEquals(FOOD, remembered(HEALTH, List(5) { FOOD }).of("ama owusu"))
        assertNull(remembered(HEALTH, List(4) { FOOD }).of("ama owusu"))
        assertNull(remembered(FOOD, listOf(FOOD, FOOD, HEALTH, FOOD, FOOD)).of("ama owusu"))
        assertNull(remembered(null, List(5) { OTHER }).of("ama owusu"))
    }

    @Test
    fun a_phone_number_is_treated_like_a_person() = runBlocking {
        assertNull(remembered(HEALTH, emptyList()).of("0240000001 (mtn mobile money)"))
        assertEquals(FOOD, remembered(HEALTH, List(5) { FOOD }).of("0240000001 (mtn mobile money)"))
    }

    @Test
    fun a_payment_service_is_never_remembered() = runBlocking {
        assertNull(remembered(GROCERIES, List(5) { FOOD }).of("paystack ghana limited"))
        assertNull(remembered(GROCERIES, emptyList()).of("expresspay ova"))
    }

    @Test
    fun tells_payment_services_from_shops() {
        listOf("Paystack Ghana Limited", "expressPay ova", "Express Pay ova", "EXPRESSPAY RRN: 1234", "HubtelPOS.sp",
            "Slydepay ova", "OVA for Payswitch instore service", "Flutterwave")
            .forEach { assertTrue(it, PayeeKind.isPaymentService(it)) }
        listOf("SHOPRITE ACCRA", "MTN Bundle", "Zen Online", "KOFI MENSAH", "Road Tech")
            .forEach { assertFalse(it, PayeeKind.isPaymentService(it)) }
    }

    @Test
    fun tells_businesses_from_people() {
        listOf("SHOPRITE ACCRA", "Bills.INV", "NOBLE HOUSE (TEMA)", "Zen Online", "Road Tech", "MTN Bundle", "ABC Ventures")
            .forEach { assertTrue(it, PayeeKind.isBusiness(it)) }
        listOf("KOFI MENSAH", "AMA SERWAA OWUSU", "0240000001 (MTN MOBILE MONEY)")
            .forEach { assertFalse(it, PayeeKind.isBusiness(it)) }
    }
}
