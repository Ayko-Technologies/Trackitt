package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationServiceTest {

    private val service = DictionaryCategorizationService(userLookup = { null })

    @Test
    fun `melcom maps to SHOPPING`() {
        assertEquals(TransactionCategory.SHOPPING, service.categorize("MELCOM COMMUNITY 25"))
    }

    @Test
    fun `bolt maps to TRANSPORT`() {
        assertEquals(TransactionCategory.TRANSPORT, service.categorize("Bolt ride"))
    }

    @Test
    fun `ecg maps to BILLS`() {
        assertEquals(TransactionCategory.BILLS, service.categorize("ECG Prepaid"))
    }

    @Test
    fun `papaye maps to FOOD`() {
        assertEquals(TransactionCategory.FOOD, service.categorize("Papaye Fast Food"))
    }

    @Test
    fun `shoprite maps to GROCERIES`() {
        assertEquals(TransactionCategory.GROCERIES, service.categorize("Shoprite Accra Mall"))
    }

    @Test
    fun `unknown merchant maps to OTHER`() {
        assertEquals(TransactionCategory.OTHER, service.categorize("REBECCA NUETEY"))
    }

    @Test
    fun `user override takes priority`() {
        val serviceWithOverride = DictionaryCategorizationService(
            userLookup = { merchant ->
                if (merchant == "rebecca nuetey") TransactionCategory.FOOD else null
            }
        )
        assertEquals(TransactionCategory.FOOD, serviceWithOverride.categorize("REBECCA NUETEY"))
    }

    @Test
    fun `expressPay maps to INCOME`() {
        assertEquals(TransactionCategory.INCOME, service.categorize("Express Pay ova"))
    }

    @Test
    fun `bills inv maps to BILLS`() {
        assertEquals(TransactionCategory.BILLS, service.categorize("Bills.INV"))
    }

    @Test
    fun `case insensitive matching`() {
        assertEquals(TransactionCategory.SHOPPING, service.categorize("melcom"))
        assertEquals(TransactionCategory.SHOPPING, service.categorize("MELCOM"))
        assertEquals(TransactionCategory.SHOPPING, service.categorize("Melcom"))
    }
}
