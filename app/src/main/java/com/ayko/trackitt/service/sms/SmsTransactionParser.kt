package com.ayko.trackitt.service.sms

/**
 * Routes an SMS to a provider-specific parser when one exists, then falls back to the generic
 * parser so new providers and new wordings still produce transactions.
 */
class SmsTransactionParser(
    private val momo: MoMoSmsParser = MoMoSmsParser(),
    private val telecel: TelecelCashParser = TelecelCashParser(),
    private val ecobank: EcobankSmsParser = EcobankSmsParser(),
    private val generic: GenericSmsParser = GenericSmsParser(),
) {

    fun parse(sender: String?, body: String, smsTimestamp: Long): ParsedTransaction? {
        val provider = SmsProviders.forSender(sender)
        if (provider == null && isPersonalNumber(sender)) return null

        val specific = when (provider) {
            SmsProviders.MOMO -> momo.parse(body, smsTimestamp)
            SmsProviders.TELECEL -> telecel.parse(body, smsTimestamp)
            SmsProviders.ECOBANK -> ecobank.parse(body, smsTimestamp)
            else -> null
        }
        if (specific != null) {
            return specific.copy(source = provider!!.name, balance = specific.balance ?: generic.balanceOf(body))
        }

        return generic.parse(body, smsTimestamp, provider ?: SmsProviders.unknown(sender))
    }

    /** For an SMS that [parse] rejected: a merchant receipt worth matching to a wallet transaction. */
    fun parseReceipt(sender: String?, body: String, smsTimestamp: Long): ParsedReceipt? {
        if (sender.isNullOrBlank() || isPersonalNumber(sender)) return null
        return generic.parseReceipt(body, smsTimestamp, sender)
    }

    /** Providers text from names or short codes; a full phone number is a person, or a scam. */
    private fun isPersonalNumber(sender: String?): Boolean {
        val digits = sender?.trim()?.removePrefix("+") ?: return false
        return digits.length >= 9 && digits.all { it.isDigit() }
    }
}
