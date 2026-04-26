package com.ayko.trackitt.service.sms

import java.text.SimpleDateFormat
import java.util.Locale

class EcobankSmsParser {

    fun parse(smsBody: String): ParsedTransaction? {
        val body = smsBody.trim()
        if (!body.startsWith("GHS")) return null
        if (!body.contains("Debited") && !body.contains("Credited")) return null

        val isDebit = body.contains("Debited")
        val isIncome = !isDebit

        val amount = extractAmount(body) ?: return null
        val ref = extractRef(body) ?: return null
        val merchant = extractMerchant(body) ?: "Ecobank"
        val date = extractDate(body)

        return ParsedTransaction(
            amount = amount,
            fee = 0.0,
            isIncome = isIncome,
            merchant = merchant.trim(),
            transactionId = "eco_$ref",
            reference = null,
            date = date,
            rawSms = body,
            source = "Ecobank",
        )
    }

    private fun extractAmount(body: String): Double? {
        val match = Regex("""^GHS\s*([\d,]+\.?\d*)""").find(body) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractRef(body: String): String? {
        val match = Regex("""REF:(\S+)""").find(body) ?: return null
        return match.groupValues[1]
    }

    private fun extractMerchant(body: String): String? {
        val refMatch = Regex("""REF:\S+\s+(.+?)\s+on\s+\d""").find(body)
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
            else -> description.take(30).trim()
        }
    }

    private fun extractDate(body: String): Long {
        val match = Regex("""on\s+(\d{2}-[A-Z]{3}-\d{2})""").find(body)
        if (match != null) {
            try {
                val sdf = SimpleDateFormat("dd-MMM-yy", Locale.US)
                return sdf.parse(match.groupValues[1])?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
