package com.ayko.trackitt.service.accounts

/**
 * The phone number a wallet SMS names instead of a person, such as GhanaPay's
 * "0240000001 (MTN MOBILE MONEY)". Ghana numbers are written the local way (0XXXXXXXXX) so both
 * spellings find the same name. Masked numbers ("2335****150") and short merchant codes have none.
 */
object PayeeNumber {
    private val LEADING_NUMBER = Regex("""^\s*\+?(\d{9,15})(?![\d*])""")

    fun of(merchant: String): String? {
        val digits = LEADING_NUMBER.find(merchant)?.groupValues?.get(1) ?: return null
        return if (digits.length == 12 && digits.startsWith("233")) "0" + digits.drop(3) else digits
    }
}
