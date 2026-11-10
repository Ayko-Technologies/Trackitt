package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun reference_says_what_a_payment_to_a_known_person_was_for() = runBlocking {
        val category = service.categorize(merchant = "ADWOA FRIMPONG", reference = "Injection")

        assertEquals(TransactionCategory.HEALTH, category)
    }

    @Test
    fun keywords_match_whole_words_only() = runBlocking {
        assertEquals(TransactionCategory.OTHER, service.categorize(merchant = "KOFI MENSAH", reference = "price list"))
        assertEquals(TransactionCategory.OTHER, service.categorize(merchant = "KOFI MENSAH", reference = "card"))
        assertEquals(TransactionCategory.TRANSPORT, service.categorize(merchant = "KOFI MENSAH", reference = "car repayment"))
    }

    @Test
    fun longer_phrase_wins_over_a_word_inside_it() = runBlocking {
        assertEquals(TransactionCategory.SNACKS, service.categorize(merchant = "KOFI MENSAH", reference = "ice cream"))
        assertEquals(TransactionCategory.BEAUTY, service.categorize(merchant = "KOFI MENSAH", reference = "body cream"))
        assertEquals(TransactionCategory.FOOD, service.categorize(merchant = "KOFI MENSAH", reference = "light soup"))
    }

    @Test
    fun plurals_and_run_together_phrases_match() = runBlocking {
        assertEquals(TransactionCategory.SNACKS, service.categorize(merchant = "KOFI MENSAH", reference = "meat pies"))
        assertEquals(TransactionCategory.POCKET_MONEY, service.categorize(merchant = "KOFI MENSAH", reference = "pocketmoney"))
        assertEquals(TransactionCategory.CHURCH, service.categorize(merchant = "KOFI MENSAH", reference = "2nd offering"))
    }

    @Test
    fun note_beats_reference() = runBlocking {
        val category = service.categorize(merchant = "KOFI MENSAH", reference = "motor", note = "school fees")

        assertEquals(TransactionCategory.EDUCATION, category)
    }

    @Test
    fun merchant_dictionary_still_names_known_businesses() = runBlocking {
        assertEquals(TransactionCategory.GROCERIES, service.categorize(merchant = "SHOPRITE ACCRA MALL"))
    }

    @Test
    fun hand_written_keywords_beat_generated_ones() = runBlocking {
        // "tutu" (a skirt) is only in the generated list.
        assertEquals(TransactionCategory.TRANSPORT, service.categorize(merchant = "KOFI MENSAH", reference = "tutu uber"))
    }

    @Test
    fun generated_keywords_fill_in_when_no_hand_written_one_matches() = runBlocking {
        assertEquals(TransactionCategory.FOOD, service.categorize(merchant = "KOFI MENSAH", reference = "guacamole"))
        assertEquals(TransactionCategory.HEALTH, service.categorize(merchant = "KOFI MENSAH", reference = "ibuprofen"))
    }

    @Test
    fun bank_fees_are_bills_not_school_fees() = runBlocking {
        assertEquals(TransactionCategory.BILLS, service.categorize(merchant = "MONTHLY CARD SERVICE FEES FOR"))
        assertEquals(TransactionCategory.EDUCATION, service.categorize(merchant = "KOFI MENSAH", reference = "fees"))
    }

    @Test
    fun purpose_ignores_the_merchant() {
        assertEquals(null, service.purposeOf(reference = null, note = null))
        assertEquals(TransactionCategory.HEALTH, service.purposeOf(reference = "injection", note = null))
    }

    @Test
    fun business_costs_have_their_own_category() {
        assertEquals(TransactionCategory.BUSINESS, service.purposeOf(reference = "restock", note = null))
        assertEquals(TransactionCategory.BUSINESS, service.purposeOf(reference = null, note = "business"))
    }

    @Test
    fun keyword_files_load_without_a_keyword_in_two_categories() {
        assertTrue(PurposeKeywords.curated.size > 500)
        assertTrue(PurposeKeywords.generated.size > 5000)
    }

    @Test
    fun ai_prompt_describes_every_category_it_may_answer() {
        val prompt = CategorizationPrompt.build("KOFI MENSAH", "go bed", null)
        TransactionCategory.assignable
            .filter { it != TransactionCategory.INCOME && it != TransactionCategory.TRANSFER }
            .forEach { assertTrue("${it.name} missing", prompt.contains("- ${it.name}:")) }
    }
}
