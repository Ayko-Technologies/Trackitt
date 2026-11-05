package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

/** Known businesses, matched against the merchant name. */
val MERCHANT_DICTIONARY: Map<String, TransactionCategory> = keywords(
    TransactionCategory.SHOPPING to listOf(
        "melcom", "game", "palace", "koala", "amazon", "ebay", "jumia", "temu", "shein", "aliexpress", "etsy",
        "tonaton", "jiji", "ikea", "zara", "primark", "decathlon", "max mart", "china mall", "makro",
    ),
    TransactionCategory.GROCERIES to listOf(
        "shoprite", "maxmart", "marina mall", "spring supermarket", "hs home supermarket", "supermarket",
        "carrefour", "spar", "walmart", "tesco", "aldi", "lidl", "costco", "kroger", "sainsbury", "asda",
        "pick n pay", "woolworths", "checkers", "trader joe", "whole foods", "safeway", "target",
    ),
    TransactionCategory.FOOD to listOf(
        "papaye", "kfc", "chicken inn", "pizza inn", "marwako", "burger king", "frankies", "slice and topping",
        "bolt food", "glovo", "uber eats", "jumia food", "chowdeck", "doordash", "grubhub", "deliveroo",
        "just eat", "mcdonald", "mcdonalds", "domino", "dominos", "pizza hut", "subway", "starbucks",
        "chicken republic", "barcelos", "starbites", "auntie anne", "cold stone", "papa john", "wendys",
        "taco bell", "chipotle", "nando", "tim hortons", "dunkin", "costa coffee",
    ),
    TransactionCategory.TRANSPORT to listOf(
        "bolt", "uber", "yango", "lyft", "indrive", "grab", "careem", "trotro", "taxi", "top oil", "goil",
        "star oil", "total", "totalenergies", "shell", "puma energy", "allied oil", "frimps oil", "zen petroleum",
        "petrosol", "engen", "vivo energy", "stc", "vip jeoun", "africa world airlines",
    ),
    TransactionCategory.BILLS to listOf(
        "ecg", "gwcl", "dstv", "gotv", "startimes", "airtel", "surfline", "etranzact", "mtnbundle", "mtn bundle",
        "vodafone", "spotify", "netflix", "youtube", "google one", "showmax", "apple", "icloud", "disney",
        "hulu", "hbo", "deezer", "audiomack", "boomplay", "tidal", "openai", "chatgpt", "canva", "adobe",
        "microsoft", "dropbox", "starlink", "busy internet",
    ),
    TransactionCategory.AIRTIME to listOf("mtn airtime"),
    TransactionCategory.TRANSFER to listOf(
        "wallet2bank", "bank2wallet", "momo → bank", "bank → momo", "zenith momo pos", "bigpay",
        "interoperability push", "interoperability pull", "itc ltd", "taptapsend", "send wave",
    ),
    TransactionCategory.EDUCATION to listOf("zen online"),
    TransactionCategory.OTHER to listOf("atm"),
    TransactionCategory.HEALTH to listOf(
        "greenville hospital", "hospital", "pharmacy", "clinic", "chemist", "mpharma", "ernest chemists",
        "boots", "cvs", "walgreens", "nyaho", "lister", "medlab", "synlab",
    ),
    TransactionCategory.CHURCH to listOf("icgc", "life centre", "cornerstone chapel", "dayspring"),
)

/**
 * What a payment was for, matched against its reference and the user's note. The hand-written list
 * wins over the one generated from WordNet; see the files' headers.
 */
object PurposeKeywords {
    val curated: Map<String, TransactionCategory> by lazy { readSections("purpose-keywords.tsv") }

    val generated: Map<String, TransactionCategory> by lazy { readPairs("purpose-keywords-wordnet.tsv") }

    private fun lines(file: String): List<String> {
        // An absolute path: release builds may move this class to another package.
        val stream = checkNotNull(PurposeKeywords::class.java.getResourceAsStream("$RESOURCE_DIR/$file")) { "$file is missing" }
        return stream.bufferedReader().use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        }
    }

    /** "[CATEGORY]" lines, each followed by comma-separated keywords. */
    private fun readSections(file: String): Map<String, TransactionCategory> {
        val groups = mutableListOf<Pair<TransactionCategory, MutableList<String>>>()
        for (line in lines(file)) {
            if (line.startsWith("[")) {
                groups += TransactionCategory.valueOf(line.removeSurrounding("[", "]")) to mutableListOf()
            } else {
                check(groups.isNotEmpty()) { "$file: keywords before the first [CATEGORY]" }
                groups.last().second += line.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return keywords(*groups.toTypedArray())
    }

    private const val RESOURCE_DIR = "/com/ayko/trackitt/service/categorization"

    /** "keyword<TAB>CATEGORY" lines. */
    private fun readPairs(file: String): Map<String, TransactionCategory> =
        lines(file).associate { line ->
            val (keyword, category) = line.split('\t')
            keyword to TransactionCategory.valueOf(category)
        }
}

/** Builds a keyword map, refusing a keyword listed under two categories. */
private fun keywords(vararg groups: Pair<TransactionCategory, List<String>>): Map<String, TransactionCategory> {
    val map = LinkedHashMap<String, TransactionCategory>()
    for ((category, words) in groups) {
        for (word in words) {
            val previous = map.put(word, category)
            require(previous == null) { "\"$word\" is listed under both $previous and $category" }
        }
    }
    return map
}
