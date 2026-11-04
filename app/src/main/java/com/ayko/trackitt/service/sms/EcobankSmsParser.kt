package com.ayko.trackitt.service.sms

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EcobankSmsParser {

    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val body = smsBody.trim()
        if (!body.startsWith("GHS")) return null
        val direction = Regex("""\b(Debited|Credited)\b""", RegexOption.IGNORE_CASE).find(body) ?: return null
        // "GHS -4.00 debited" is a card refund: a negative debit puts money back.
        val negative = Regex("""^GHS\s*-""").containsMatchIn(body)
        val isIncome = direction.value.equals("Credited", ignoreCase = true) != negative

        val amount = extractAmount(body) ?: return null
        val id = extractRef(body) ?: "h${hash(body)}"
        val merchant = extractMerchant(body) ?: "Ecobank"
        val date = extractDate(body, smsTimestamp)

        return ParsedTransaction(
            amount = amount,
            fee = 0.0,
            isIncome = isIncome,
            merchant = merchant.trim(),
            transactionId = "eco_$id",
            reference = null,
            date = date,
            rawSms = body,
            source = "Ecobank",
        )
    }

    private fun extractAmount(body: String): Double? {
        val match = Regex("""^GHS\s*-?\s*([\d,]*\.?\d+)""").find(body) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun extractRef(body: String): String? {
        val match = Regex("""REF:(\S+)""").find(body) ?: return null
        return match.groupValues[1]
    }

    private fun extractMerchant(body: String): String? {
        val refMatch = Regex("""REF:\S+\s+(.+?)\s+on\s+\d""").find(body)
            ?: Regex("""\bac\s+\S+\s+(.+?)\s+on\s+\d{2}-""", RegexOption.IGNORE_CASE).find(body)
        if (refMatch != null) {
            val description = refMatch.groupValues[1].trim()
            return cleanMerchant(description)
        }
        return null
    }

    private fun cleanMerchant(description: String): String {
        return when {
            description.contains("POS PURCHASE") -> {
                val after = description.substringAfter(")-", "").trim()
                val name = after.split("\\s{2,}".toRegex()).firstOrNull() ?: after
                name.trim().ifEmpty { "Card purchase" }
            }
            description.contains("Wallet2Bank") -> "MoMo → Bank"
            description.contains("Bank2Wallet") -> "Bank → MoMo"
            description.contains("ATM CASH WITHDRAWAL") -> {
                val after = description.substringAfter(")-", "").trim()
                val name = after.split("\\s{2,}".toRegex()).firstOrNull() ?: "ATM Withdrawal"
                "ATM: $name".trim()
            }
            description.contains("GIP INCOMING") -> {
                val boMatch = Regex("""B/O\s+([A-Z ]+?)\s{2,}""").find(description)
                    ?: Regex("""B/O\s+([A-Z ]+)""").find(description)
                boMatch?.groupValues?.get(1)?.trim() ?: "Bank transfer"
            }
            description.contains("MOBILE_MONEY") || description.contains("VODAFONE_CASH") -> "Mobile Money"
            description.contains("SAVINGS CREDIT INTEREST") -> "Interest"
            else -> description.replace(Regex("""\s+nbsp\b"""), "").take(30).trim()
        }
    }

    private fun extractDate(body: String, smsTimestamp: Long): Long {
        val match = Regex("""on\s+(\d{2}-[A-Za-z]{3}-\d{2})""").find(body) ?: return smsTimestamp
        val parsed = try {
            SimpleDateFormat("dd-MMM-yy", Locale.US).parse(match.groupValues[1])?.time
        } catch (_: Exception) {
            null
        } ?: return smsTimestamp
        // The alert carries only the day: keep the SMS time unless the alert is for an earlier day.
        return if (sameDay(parsed, smsTimestamp) || parsed > smsTimestamp) smsTimestamp else parsed
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val first = Calendar.getInstance().apply { timeInMillis = a }
        val second = Calendar.getInstance().apply { timeInMillis = b }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    private fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
