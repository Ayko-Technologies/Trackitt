package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    suspend fun categorize(
        merchant: String,
        reference: String? = null,
        note: String? = null,
    ): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override suspend fun categorize(
        merchant: String,
        reference: String?,
        note: String?,
    ): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        // What the user wrote, then what the sender wrote, says what this payment was for. A category
        // remembered for the merchant (often a person, paid for different things) only fills the gap.
        purposeOf(reference, note)?.let { return it }
        userLookup(normalized)?.let { return it }
        merchantKeywords.match(merchant)?.let { return it }
        // A business named for what it sells ("Golden Spoon Restaurant"). Only the hand-written list:
        // people's names include words the generated list knows, like "Rice" or "Fish".
        return merchantPurposeKeywords.match(merchant) ?: TransactionCategory.OTHER
    }

    /** The category named by the user's note or the payment's reference, if either names one. */
    fun purposeOf(reference: String?, note: String?): TransactionCategory? =
        note?.let { purpose(it) } ?: reference?.let { purpose(it) }

    // Hand-written keywords are chosen with care; the generated ones only fill in when none matches.
    private fun purpose(text: String) = merchantPurposeKeywords.match(text) ?: generatedKeywords.match(text)

    private companion object {
        val generatedKeywords by lazy { KeywordMatcher(PurposeKeywords.generated) }
        val merchantPurposeKeywords by lazy { KeywordMatcher(PurposeKeywords.curated) }
        val merchantKeywords by lazy { KeywordMatcher(MERCHANT_DICTIONARY) }
    }
}

/**
 * Finds keywords as whole words, so "ice" doesn't match "price", and returns the category of the
 * longest one found, so "ice cream" beats "cream". Plurals ("pies", "pastries") and run-together or
 * hyphenated phrases ("pocketmoney", "car-wash") match too.
 */
class KeywordMatcher(dictionary: Map<String, TransactionCategory>) {
    private val categories = HashMap<String, TransactionCategory>()
    private val longestPhrase: Int

    init {
        var longest = 1
        for ((keyword, category) in dictionary) {
            val words = words(keyword)
            if (words.isEmpty()) continue
            longest = maxOf(longest, words.size)
            categories[words.joinToString(" ")] = category
            if (words.size > 1) categories.putIfAbsent(words.joinToString(""), category)
        }
        longestPhrase = longest
    }

    fun match(text: String): TransactionCategory? {
        val words = words(text)
        var best: String? = null
        for (start in words.indices) {
            for (size in 1..minOf(longestPhrase, words.size - start)) {
                val phrase = words.subList(start, start + size)
                for (candidate in listOf(phrase.joinToString(" "), phrase.joinToString(""))) {
                    val found = forms(candidate).firstOrNull { it in categories } ?: continue
                    if (best == null || found.length > best.length) best = found
                }
            }
        }
        return best?.let { categories[it] }
    }

    private fun words(text: String): List<String> =
        text.lowercase().split(NON_WORD).filter { it.isNotEmpty() }

    /** The word as written, then as a singular. */
    private fun forms(word: String): List<String> = buildList {
        add(word)
        if (word.length > 3 && word.endsWith("ies")) add(word.dropLast(3) + "y")
        if (word.length > 3 && word.endsWith("es")) add(word.dropLast(2))
        if (word.length > 2 && word.endsWith("s")) add(word.dropLast(1))
    }

    private companion object {
        val NON_WORD = Regex("[^a-z0-9]+")
    }
}
