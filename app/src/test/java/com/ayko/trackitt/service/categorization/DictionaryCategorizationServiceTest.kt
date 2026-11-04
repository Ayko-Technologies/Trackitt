package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryCategorizationServiceTest {

    // People are categorised from the user's own mappings on the device, never from the shipped dictionary.
    private val userMappings = mapOf(
        "adwoa frimpong" to TransactionCategory.POCKET_MONEY,
        "akua addo" to TransactionCategory.FOOD,
    )
    private val service = DictionaryCategorizationService(userLookup = { userMappings[it] })

    @Test
    fun pocket_money_note_maps_to_pocket_money() = runBlocking {
        val category = service.categorize(
            merchant = "ESI QUAYE",
            note = "Pocket money",
        )

        assertEquals(TransactionCategory.POCKET_MONEY, category)
    }

    @Test
    fun snack_note_overrides_generic_food_keywords() = runBlocking {
        val category = service.categorize(
            merchant = "PERFECT G VENTURES",
            note = "drinks and biscuits",
        )

        assertEquals(TransactionCategory.SNACKS, category)
    }

    @Test
    fun education_note_maps_to_education() = runBlocking {
        val category = service.categorize(
            merchant = "TEST TUTOR",
            note = "ga lessons",
        )

        assertEquals(TransactionCategory.EDUCATION, category)
    }

    @Test
    fun home_project_note_maps_to_home_project() = runBlocking {
        val category = service.categorize(
            merchant = "AKOSUA BOATENG",
            note = "AC materials",
        )

        assertEquals(TransactionCategory.HOME_PROJECT, category)
    }

    @Test
    fun user_mapping_maps_a_person_to_pocket_money() = runBlocking {
        val category = service.categorize(
            merchant = "ADWOA FRIMPONG",
            note = null,
        )

        assertEquals(TransactionCategory.POCKET_MONEY, category)
    }

    @Test
    fun user_mapping_maps_repeat_food_vendor_to_food() = runBlocking {
        val category = service.categorize(
            merchant = "AKUA ADDO",
            note = null,
        )

        assertEquals(TransactionCategory.FOOD, category)
    }
}
