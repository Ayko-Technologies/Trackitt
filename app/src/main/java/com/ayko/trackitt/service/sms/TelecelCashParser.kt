package com.ayko.trackitt.service.sms

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

class TelecelCashParser {

    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val body = smsBody.trim()
        if (!body.contains("Confirmed.")) return null

        val cleaned = body.substringAfter("Confirmed. ").trim()
        return when {
            cleaned.startsWith("GHS") && cleaned.contains("sent to") -> parseSent(cleaned, body, smsTimestamp)
            cleaned.startsWith("GHS") && cleaned.contains("paid to") -> parsePaid(cleaned, body, smsTimestamp)
            cleaned.startsWith("You have received") -> parseReceived(cleaned, body, smsTimestamp)
            cleaned.startsWith("You have withdrawn") -> parseWithdrawn(cleaned, body, smsTimestamp)
            cleaned.startsWith("On ") && cleaned.contains("deposit") -> parseDeposit(cleaned, body, smsTimestamp)
            else -> null
        }
    }

    private fun parseSent(cleaned: String, raw: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount(cleaned) ?: return null
        val nameMatch = Regex("""\d+ - ([A-Z][A-Z ]+)""").find(cleaned)
            ?: Regex("""\d+ ([A-Z][A-Z ]+)""").find(cleaned)
        val merchant = nameMatch?.groupValues?.get(1)?.trim() ?: return null
        val txId = extractTxId(raw)
        val fee = extractFee(cleaned)
        val date = extractDate(cleaned, smsTimestamp)
        val reference = extractReference(raw)
        return ParsedTransaction(amount, fee, false, merchant, txId, reference, date, raw, "Telecel Cash")
    }

    private fun parsePaid(cleaned: String, raw: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount(cleaned) ?: return null
        val merchantMatch = Regex("""paid to \S+ - (.+?) on """).find(cleaned)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: return null
        val txId = extractTxId(raw)
        val date = extractDate(cleaned, smsTimestamp)
        val reference = extractReference(raw)
        return ParsedTransaction(amount, 0.0, false, merchant, txId, reference, date, raw, "Telecel Cash")
    }

    private fun parseReceived(cleaned: String, raw: String, smsTimestamp: Long): ParsedTransaction? {
        val amountMatch = Regex("""received GHS([\d,]+\.?\d*)""").find(cleaned)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchantMatch = Regex("""from [\d*]+-(.+?)\s+on """).find(cleaned)
            ?: Regex("""from (.+?) with transaction""").find(cleaned)
            ?: Regex("""from (.+?)\s+on \d""").find(cleaned)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Telecel Cash"
        val txId = extractTxId(raw)
        val date = extractDate(cleaned, smsTimestamp)
        return ParsedTransaction(amount, 0.0, true, merchant, txId, null, date, raw, "Telecel Cash")
    }

    private fun parseWithdrawn(cleaned: String, raw: String, smsTimestamp: Long): ParsedTransaction? {
        val amountMatch = Regex("""withdrawn GHS([\d,]+\.?\d*)""").find(cleaned)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchantMatch = Regex("""from \S+ - (.+?) on """).find(cleaned)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Cash withdrawal"
        val txId = extractTxId(raw)
        val date = extractDate(cleaned, smsTimestamp)
        return ParsedTransaction(amount, 0.0, false, merchant, txId, null, date, raw, "Telecel Cash")
    }

    private fun parseDeposit(cleaned: String, raw: String, smsTimestamp: Long): ParsedTransaction? {
        val amountMatch = Regex("""deposit of GHS([\d,]+\.?\d*)""").find(cleaned)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchantMatch = Regex("""from (.+?)\s*\.""").find(cleaned)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Agent deposit"
        val txId = extractTxId(raw)
        val date = extractDate(cleaned, smsTimestamp)
        return ParsedTransaction(amount, 0.0, true, merchant, txId, null, date, raw, "Telecel Cash")
    }

    private fun extractAmount(text: String): Double? {
        val match = Regex("""GHS([\d,]+\.?\d*)""").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractTxId(raw: String): String {
        val match = Regex("""^(\d{13,})""").find(raw.trim())
        return "tc_${match?.groupValues?.get(1) ?: hash(raw.trim())}"
    }

    private fun extractFee(text: String): Double {
        val match = Regex("""charged GHS(\d+(?:\.\d+)?)""").find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractDate(text: String, smsTimestamp: Long): Long {
        val match = Regex("""on (\d{4}-\d{2}-\d{2}) at (\d{2}:\d{2}:\d{2})""").find(text)
        if (match != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                return sdf.parse("${match.groupValues[1]} ${match.groupValues[2]}")?.time
                    ?: smsTimestamp
            } catch (_: Exception) {}
        }
        return smsTimestamp
    }

    private fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun extractReference(raw: String): String? {
        val match = Regex("""Reference:\s*(.+?)\.""").find(raw)
        val ref = match?.groupValues?.get(1)?.trim()
        return if (ref.isNullOrBlank() || ref == " ") null else ref
    }
}
