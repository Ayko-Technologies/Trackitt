package com.ayko.trackitt.service.sms

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Sender-agnostic parser for financial SMS. It does not depend on any one provider's wording:
 * it finds cedi amounts written either way round ("GHS 50.00", "50.00 GHS", "GH¢50", "Ghc 5"),
 * labels each one as the transaction amount, a balance or a fee from the words just before it,
 * and reads the direction from how the message talks about the user's money.
 *
 * Only account statements are accepted: the message must show a balance or name the user's
 * account or wallet. Merchant receipts and promos don't, which keeps a payment that the wallet
 * and the merchant both text about from being counted twice.
 */
class GenericSmsParser {

    fun parse(smsBody: String, smsTimestamp: Long, provider: SmsProvider): ParsedTransaction? {
        val text = smsBody.trim().replace(WHITESPACE, " ")
        if (REJECT.any { it.containsMatchIn(text) }) return null
        if (FAILED.containsMatchIn(text) && !REVERSAL.containsMatchIn(text)) return null
        if (!ACCOUNT_EVIDENCE.containsMatchIn(text.replace(BALANCE_ENQUIRY_HINT, ""))) return null
        if (mentionsOtherWallet(text, provider)) return null

        val isIncome = isIncome(text) ?: return null
        val amounts = findAmounts(text)
        val main = amounts.firstOrNull { it.role == Role.MAIN } ?: return null
        if (main.value <= 0.0) return null

        val isReversal = REVERSAL.containsMatchIn(text)
        val merchant = when {
            isReversal -> if (REFUND.containsMatchIn(text)) "Refund" else "Reversal"
            INTEREST.containsMatchIn(text) -> "Interest"
            else -> counterparty(text, isIncome, main.start, main.end) ?: productName(text) ?: provider.name
        }
        val id = transactionId(text) ?: "h${hash(text)}"

        return ParsedTransaction(
            amount = main.value,
            fee = fee(text, amounts),
            isIncome = isIncome,
            merchant = merchant,
            transactionId = "${provider.key.lowercase(Locale.US)}_${if (isReversal) "rev_" else ""}$id",
            reference = reference(text),
            date = date(text, smsTimestamp),
            rawSms = smsBody.trim(),
            source = provider.name,
            balance = balance(text, amounts),
        )
    }

    /** The account balance an SMS reports, preferring the available balance when it gives two. */
    fun balanceOf(smsBody: String): Double? {
        val text = smsBody.trim().replace(WHITESPACE, " ")
        return balance(text, findAmounts(text))
    }

    /**
     * A merchant or payment-service receipt: names who was paid, but isn't an account statement.
     * Used only to give a matching wallet transaction a better name, never as a transaction itself.
     */
    fun parseReceipt(smsBody: String, smsTimestamp: Long, sender: String): ParsedReceipt? {
        val text = smsBody.trim().replace(WHITESPACE, " ")
        if (REJECT.any { it.containsMatchIn(text) } || FAILED.containsMatchIn(text)) return null
        if (!PAYMENT_WORD.containsMatchIn(text) || NOT_A_PURCHASE_RECEIPT.containsMatchIn(text)) return null
        val main = findAmounts(text).firstOrNull { it.role == Role.MAIN } ?: return null
        if (main.value <= 0.0) return null
        val party = counterparty(text, isIncome(text) ?: false, main.start, main.end)
            ?.substringBefore(" : ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() && (it.first().isUpperCase() || it.first().isDigit()) && '@' !in it }
            ?: return null
        return ParsedReceipt(
            amount = main.value,
            party = party,
            date = date(text, smsTimestamp),
            sender = sender.trim(),
            rawSms = smsBody.trim(),
        )
    }

    private fun balance(text: String, amounts: List<Amount>): Double? {
        val balances = amounts.filter { it.role == Role.BALANCE }
        val available = balances.firstOrNull { AVAILABLE.containsMatchIn(text.substring(maxOf(0, it.start - ROLE_WINDOW), it.start)) }
        (available ?: balances.firstOrNull())?.let { return it.signedValue }
        return BARE_BALANCE.findAll(text).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private enum class Role { MAIN, BALANCE, FEE }

    private data class Amount(val value: Double, val start: Int, val end: Int, val role: Role, val negative: Boolean) {
        val signedValue get() = if (negative) -value else value
    }

    private fun findAmounts(text: String): List<Amount> {
        val matches = (PREFIX_AMOUNT.findAll(text) + POSTFIX_AMOUNT.findAll(text))
            .sortedBy { it.range.first }
            .toList()
        val accepted = mutableListOf<MatchResult>()
        for (match in matches) {
            if (accepted.none { it.range.last >= match.range.first }) accepted += match
        }

        var previousEnd = 0
        return accepted.mapNotNull { match ->
            val value = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            val windowStart = maxOf(previousEnd, match.range.first - ROLE_WINDOW)
            val window = text.substring(windowStart, match.range.first)
            previousEnd = match.range.last + 1
            val role = when {
                BALANCE_WORD.containsMatchIn(window) -> Role.BALANCE
                FEE_WORD.containsMatchIn(window) -> Role.FEE
                else -> Role.MAIN
            }
            Amount(value, match.range.first, match.range.last + 1, role, NEGATIVE.containsMatchIn(match.value))
        }
    }

    private fun isIncome(text: String): Boolean? {
        val marker = earliest(text, CR_MARKER, DR_MARKER)
        if (marker != null) return marker
        val strong = earliest(text, STRONG_CREDIT, STRONG_DEBIT)
        if (strong != null) return strong
        return earliest(text, CREDIT_WORD, DEBIT_WORD)
    }

    private fun earliest(text: String, credit: Regex, debit: Regex): Boolean? {
        val c = credit.find(text)?.range?.first
        val d = debit.find(text)?.range?.first
        return when {
            c == null && d == null -> null
            d == null -> true
            c == null -> false
            else -> c < d
        }
    }

    private fun counterparty(text: String, isIncome: Boolean, amountStart: Int, amountEnd: Int): String? {
        DESCRIPTION.find(text)?.groupValues?.get(1)?.let(::cleanParty)?.let { return it }

        val sentenceStart = SENTENCE_END.findAll(text.substring(0, amountStart)).lastOrNull()?.range?.last?.plus(1) ?: 0
        val sentenceEnd = SENTENCE_END.find(text, amountEnd)?.range?.first ?: text.length
        val markers = if (isIncome) INCOME_PARTY else EXPENSE_PARTY
        for (scope in listOf(text.substring(amountEnd, sentenceEnd), text.substring(sentenceStart, sentenceEnd))) {
            for (marker in markers) {
                val candidate = marker.find(scope)?.groupValues?.get(1)?.let(::cleanParty)
                if (candidate != null) return candidate
            }
        }
        return null
    }

    private fun mentionsOtherWallet(text: String, provider: SmsProvider): Boolean {
        val brand = OTHER_WALLET.find(text)?.groupValues?.get(1)?.lowercase(Locale.US) ?: return false
        val owner = when (brand) {
            "mtn", "momo" -> SmsProviders.MOMO
            "vodafone", "telecel" -> SmsProviders.TELECEL
            else -> SmsProviders.AT_MONEY
        }
        return owner != provider
    }

    private fun cleanParty(raw: String): String? {
        var name = raw.trim()
        val number = LEADING_NUMBER.find(name)?.value?.trim(' ', '-', ',')
        repeat(3) { name = name.replace(LEADING_NUMBER, "").trim() }
        if (number != null && NETWORK_ONLY.matches(name.trim(' ', '.', ','))) {
            return "$number (${name.trim(' ', '.', ',')})"
        }
        name = name.replace(MERCHANT_PREFIX, "")
            .replace(TRAILING_ACCOUNT, "")
            .replace(TRAILING_NUMBER, "")
            .trim(' ', '.', ',', ':', ';', '-')
            .replace(WHITESPACE, " ")
        if (name.isEmpty() || name.none { it.isLetter() }) return null
        if (name.startsWith("your ", ignoreCase = true)) return null
        return name.take(60).trim()
    }

    private fun productName(text: String): String? {
        val product = PRODUCT.find(text)?.groupValues?.get(1)?.lowercase(Locale.US) ?: return null
        return product.replaceFirstChar { it.titlecase(Locale.US) }
    }

    private fun fee(text: String, amounts: List<Amount>): Double {
        val fees = amounts.filter { it.role == Role.FEE }
        if (fees.isNotEmpty()) return fees.sumOf { it.value }
        return BARE_FEE.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun transactionId(text: String): String? {
        for (pattern in ID_PATTERNS) {
            val id = pattern.find(text)?.groupValues?.get(1)?.trimEnd('.', ',')
            if (!id.isNullOrBlank() && id.any { it.isDigit() }) return id.replace(" ", "")
        }
        return null
    }

    private fun reference(text: String): String? {
        val ref = REFERENCE.find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.') ?: return null
        if (ref.isBlank() || ref.equals("none", ignoreCase = true) || ref.all { !it.isLetterOrDigit() }) return null
        return ref.take(80)
    }

    private fun date(text: String, smsTimestamp: Long): Long {
        for ((pattern, format, hasTime) in DATE_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues.drop(1).filter { it.isNotEmpty() }.joinToString(" ")
            val parsed = try {
                SimpleDateFormat(format, Locale.US).apply { isLenient = false }.parse(raw)?.time
            } catch (_: Exception) {
                null
            } ?: continue
            val diff = abs(parsed - smsTimestamp)
            if (diff > MAX_BODY_DATE_DRIFT) continue
            if (hasTime) return parsed
            // A date-only value later than the SMS is a value date, not when the money moved.
            return if (diff > ONE_AND_HALF_DAYS && parsed < smsTimestamp) parsed + NOON else smsTimestamp
        }
        return smsTimestamp
    }

    private fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val WHITESPACE = Regex("""\s+""")

        const val CURRENCY = """(?:GH\s?[S¢₵C]|¢|₵)"""
        const val NUMBER = """(\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d+(?:\.\d+)?|\.\d+)"""
        val PREFIX_AMOUNT = Regex("""(?<![A-Za-z])$CURRENCY(?:\s*$CURRENCY)?\s*:?\s*-?\s*$NUMBER""", RegexOption.IGNORE_CASE)
        val POSTFIX_AMOUNT = Regex("""(?<![\w.,])$NUMBER\s*(?:$CURRENCY|cedis?)(?![A-Za-z])""", RegexOption.IGNORE_CASE)
        const val ROLE_WINDOW = 28

        val BALANCE_WORD = Regex("""\b(bal|balance|bal\.)\b""", RegexOption.IGNORE_CASE)
        val FEE_WORD = Regex("""\b(fee|fees|charge|charged|e-?levy|levy|tax|commission)\b""", RegexOption.IGNORE_CASE)
        val AVAILABLE = Regex("""\bavail""", RegexOption.IGNORE_CASE)
        val NEGATIVE = Regex("""-\s*\.?\d""")
        val BARE_BALANCE = Regex("""\bbal(?:ance)?\s*(?:is|:)?\s*(?:now\s*)?(\d[\d,]*\.\d+)\b""", RegexOption.IGNORE_CASE)
        val PAYMENT_WORD = Regex("""\b(?:paid|debited|received|recieved|purchased|sent|successful(?:ly)?)\b""", RegexOption.IGNORE_CASE)
        val NOT_A_PURCHASE_RECEIPT = Regex("""\b(?:bundle|mashup|airtime|data|loan)\b""", RegexOption.IGNORE_CASE)
        val BARE_FEE = Regex("""\b(?:fee|e-?levy)(?:\s+(?:charged|was|is))?\s*:?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

        val REJECT = listOf(
            Regex("""\b(otp|one[- ]time (?:password|pin|code)|verification code|security code|token)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(unsuccessful|not successful|declined|insufficient|not have enough|rejected|cancell?ed|withheld|pending)\b""", RegexOption.IGNORE_CASE),
            Regex("""\bwill be (?:deducted|collected|debited|charged|credited)\b|\bto be paid\b""", RegexOption.IGNORE_CASE),
            Regex("""\benter (?:code|pin)\b|\bto confirm payment\b|\bto approve\b""", RegexOption.IGNORE_CASE),
        )
        val FAILED = Regex("""\b(failed|failure)\b""", RegexOption.IGNORE_CASE)
        val BALANCE_ENQUIRY_HINT = Regex("""\bcheck (?:your )?(?:\w+ )?balance\b""", RegexOption.IGNORE_CASE)
        val OTHER_WALLET = Regex("""\byour (mtn|momo|vodafone|telecel|airteltigo)\b(?: \w+)? (?:account|wallet|a/c)\b""", RegexOption.IGNORE_CASE)
        val ACCOUNT_EVIDENCE = Regex(
            """\b(?:bal|balance)\b|\byour (?:new )?(?:\w+ )?(?:acc|account|a/c|wallet)\b|\bac(?:c|ct)?[:.]?\s*[x*\d]{3,}""",
            RegexOption.IGNORE_CASE,
        )

        val STRONG_CREDIT = Regex(
            """\bcredited\b|\b(?:made|deposited|sent|paid|transferred)\s+(?:in)?to\s+your\s+(?:\w+\s+)?(?:acc|account|a/c|wallet|mobile money)""",
            RegexOption.IGNORE_CASE,
        )
        val CR_MARKER = Regex("""\d\s*CR\b|\bcredit alert\b|\bCR\s*:|^\s*credit\b(?!\s+(?:card|limit))""", RegexOption.IGNORE_CASE)
        val DR_MARKER = Regex("""\d\s*DR\b|\bdebit alert\b|\bDR\s*:|^\s*debit\b(?!\s+(?:card|limit))""", RegexOption.IGNORE_CASE)
        val STRONG_DEBIT = Regex("""\bdebited\b|\b(?:deducted|withdrawn|taken)\s+from\s+your\b""", RegexOption.IGNORE_CASE)
        val CREDIT_WORD = Regex(
            """\b(?:received|recieved|deposit of|cash in|refund(?:ed)?|reversed|reversal|interest)\b""",
            RegexOption.IGNORE_CASE,
        )
        val DEBIT_WORD = Regex(
            """\b(?:sent|paid|payment made|payment of|payment for|your payment|transferred|withdrawn|withdrawal of|cash out|purchased|bought|purchase request)\b""",
            RegexOption.IGNORE_CASE,
        )
        val REVERSAL = Regex("""\b(?:reversed|reversal|refund(?:ed)?)\b""", RegexOption.IGNORE_CASE)
        val REFUND = Regex("""\brefund(?:ed)?\b""", RegexOption.IGNORE_CASE)
        val INTEREST = Regex("""\binterest\b""", RegexOption.IGNORE_CASE)

        const val PARTY_STOP = """(?=\s+on\s+(?:\d|(?:your|the|mon|tue|wed|thu|fri|sat|sun|mtn|telecel|vodafone|airteltigo)\b)|\s+with\b|\s+via\b|\s*\(|\.(?=\s|$|[A-Z])|\s+current\b|\s+has\s+been|\s+ref(?:erence)?\b|\s+at\s+\d|\s+as\s|\s+through\s|\s+in\s+reference\b|,\s+(?:mobile money|wallet|account|acc)\b|\s+in\s+a\s|\s+(?:your|available|new)\s+bal|$)"""
        val INCOME_PARTY = listOf(
            Regex("""\b(?:from|b/o)\s*:?\s*(.+?)$PARTY_STOP""", RegexOption.IGNORE_CASE),
            Regex("""\bthrough agent\s+(.+?)$PARTY_STOP""", RegexOption.IGNORE_CASE),
        )
        val EXPENSE_PARTY = listOf(
            Regex("""\bto\s*:?\s*(?!your\b)(.+?)$PARTY_STOP""", RegexOption.IGNORE_CASE),
            Regex("""\bat\s+(?![\d:])(.+?)$PARTY_STOP""", RegexOption.IGNORE_CASE),
            Regex("""\bfrom\s*:?\s*(?!your\b)(.+?)$PARTY_STOP""", RegexOption.IGNORE_CASE),
        )
        val LEADING_NUMBER = Regex("""^[A-Z]?[+\d*xX]{4,}\s*[-,]?\s*""")
        val NETWORK_ONLY = Regex("""(?:mtn mobile money|mtn momo|mobile money|vodafone cash|telecel cash|airteltigo money|at money)""", RegexOption.IGNORE_CASE)
        val SENTENCE_END = Regex("""\.(?:\s|$)""")
        val DESCRIPTION = Regex("""\b(?:desc(?:ription)?|narration|details)\s*:\s*(.+?)(?=\s+(?:avail|bal|date|ref)|\s*,|$)""", RegexOption.IGNORE_CASE)
        val MERCHANT_PREFIX = Regex("""^merchant\s+""", RegexOption.IGNORE_CASE)
        val TRAILING_NUMBER = Regex("""\s+[+\d*xX]{6,}$""")
        val TRAILING_ACCOUNT = Regex("""\s+(?:account|acc|a/c)(?:\s+no\.?)?\s*[\d*xX]+$""", RegexOption.IGNORE_CASE)
        val PRODUCT = Regex("""\b(airtime|data bundle|voice bundle|bundle)\b""", RegexOption.IGNORE_CASE)

        val ID_PATTERNS = listOf(
            Regex("""\b(?:financial\s+)?trans(?:action)?\.?\s*id\s*(?:is)?\s*[:#]?\s*([A-Za-z0-9-]{5,}(?:\.\s?[A-Z]?\d+)*)""", RegexOption.IGNORE_CASE),
            Regex("""\btrans(?:action)?\.?\s*(?:ref|no)\.?\s*[:#]\s*([A-Za-z0-9-]{5,})""", RegexOption.IGNORE_CASE),
            Regex("""\bwith ID\s+([A-Za-z0-9]{5,})""", RegexOption.IGNORE_CASE),
            Regex("""\bREF:\s*([A-Za-z0-9]{6,})"""),
            Regex("""\bref(?:erence)?\s*(?:id|no\.?|#)\s*[:#]?\s*([A-Za-z0-9]{5,})""", RegexOption.IGNORE_CASE),
            Regex("""^(\d{10,})\s+confirmed""", RegexOption.IGNORE_CASE),
        )
        val REFERENCE = Regex(
            """\b(?:(?:reference|ref|message from sender|message)\s*:|in reference to\b)\s*(.*?)(?=\.\s|\.$|\s+on\s+\d|\s+(?:transaction|financial|your|balance|date)\b|$)""",
            RegexOption.IGNORE_CASE,
        )

        val DATE_PATTERNS = listOf(
            Triple(Regex("""(\d{4}-\d{2}-\d{2})(?:\s+at)?\s+(\d{2}:\d{2}:\d{2})"""), "yyyy-MM-dd HH:mm:ss", true),
            Triple(Regex("""(\d{2}-\d{2}-\d{4})\s+(\d{2}:\d{2}:\d{2})"""), "dd-MM-yyyy HH:mm:ss", true),
            Triple(Regex("""(\d{4}-\d{2}-\d{2})"""), "yyyy-MM-dd", false),
            Triple(Regex("""\b(\d{1,2}-[A-Za-z]{3}-\d{4})\b"""), "dd-MMM-yyyy", false),
            Triple(Regex("""\b(\d{1,2}-[A-Za-z]{3}-\d{2})\b"""), "dd-MMM-yy", false),
            Triple(Regex("""\b(\d{2}/\d{2}/\d{4})\b"""), "dd/MM/yyyy", false),
        )
        const val MAX_BODY_DATE_DRIFT = 30L * 24 * 60 * 60 * 1000
        const val ONE_AND_HALF_DAYS = 36L * 60 * 60 * 1000
        const val NOON = 12L * 60 * 60 * 1000
    }
}
