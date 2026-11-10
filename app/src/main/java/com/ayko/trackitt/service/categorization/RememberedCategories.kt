package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.accounts.PayeeNumber

/**
 * The category a payee's next payment gets when its reference and note don't say what it was for.
 *
 * A business sells the same kind of thing every time, so it keeps the category the user last chose.
 * A person (or a bare phone number) is paid for different things, so their payments only get one
 * once their last [RUN] payments all had it: a run the user never had to break.
 */
class RememberedCategories(
    private val chosenFor: suspend (merchant: String) -> TransactionCategory?,
    private val recentCategoriesOf: suspend (merchant: String, count: Int) -> List<TransactionCategory>,
) {
    suspend fun of(merchant: String): TransactionCategory? {
        if (PayeeKind.isBusiness(merchant)) return chosenFor(merchant)
        val recent = recentCategoriesOf(merchant, RUN)
        if (recent.size < RUN) return null
        return recent.distinct().singleOrNull()?.takeUnless { it == TransactionCategory.OTHER }
    }

    companion object {
        const val RUN = 5
    }
}

/** Tells a business's name from a person's, as well as a name alone can. */
object PayeeKind {
    fun isBusiness(merchant: String): Boolean {
        if (PayeeNumber.of(merchant) != null) return false
        // Branch numbers, "Bills.INV", "NOBLE HOUSE (TEMA)": people's names don't have these.
        if (merchant.any { it.isDigit() || it in "&.()/@_" }) return true
        if (knownBusinesses.match(merchant) != null) return true
        return merchant.lowercase().split(NON_LETTER).any { it in BUSINESS_WORDS }
    }

    private val knownBusinesses by lazy { KeywordMatcher(MERCHANT_DICTIONARY) }
    private val NON_LETTER = Regex("[^a-z]+")
    private val BUSINESS_WORDS = setOf(
        "ltd", "limited", "plc", "inc", "llc", "company", "co", "ventures", "venture", "enterprise", "enterprises",
        "trading", "investments", "holdings", "group", "global", "international", "agency", "services", "service",
        "solutions", "consult", "consulting", "logistics", "express", "store", "stores", "shop", "shops", "mart",
        "mall", "market", "supermarket", "boutique", "fashion", "salon", "spa", "pharmacy", "chemist", "chemists",
        "hospital", "clinic", "restaurant", "eatery", "bakery", "kitchen", "foods", "hotel", "lodge", "academy",
        "school", "college", "university", "institute", "church", "chapel", "ministry", "ministries", "online",
        "bundle", "airtime", "petroleum", "oil", "energy", "gas", "station", "motors", "auto", "electronics",
        "tech", "technologies", "telecom", "communications", "network", "media", "studio", "printing", "press",
        "bank", "microfinance", "savings", "insurance", "pay", "ova", "travel", "tours", "airlines", "farms",
        "centre", "center", "hub",
    )
}
