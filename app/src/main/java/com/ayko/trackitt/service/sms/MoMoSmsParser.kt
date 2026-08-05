package com.ayko.trackitt.service.sms

import java.text.SimpleDateFormat
import java.util.Locale

class MoMoSmsParser {

    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val body = smsBody.trim()
        return when {
            body.startsWith("Payment made for") -> parsePaymentMade(body, smsTimestamp)
            body.startsWith("Payment received for") -> parsePaymentReceived(body, smsTimestamp)
            body.startsWith("Y'ello. You have Paid") -> parseMerchantYello(body, smsTimestamp)
            body.startsWith("Your payment of") -> parseMerchantConfirmation(body, smsTimestamp)
            body.startsWith("Cash Out made for") -> parseCashOut(body, smsTimestamp)
            body.startsWith("Payment for GHS") && body.contains("Bills.") -> parseBillPayment(body, smsTimestamp)
            body.startsWith("Payment for GHS") -> parsePaymentFor(body, smsTimestamp)
            body.startsWith("You have received") -> parseBankTransfer(body, smsTimestamp)
            body.startsWith("Cash transfer recieved:") -> parseCashTransfer(body, smsTimestamp)
            else -> null
        }
    }

    private fun parsePaymentMade(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Payment made for", body) ?: return null
        val merchant = (extractBetween(body, "to ", ". Current Balance")
            ?: extractBetween(body, "to ", " Current Balance")) ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, reference, smsTimestamp, body)
    }

    private fun parsePaymentReceived(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Payment received for", body) ?: return null
        val merchant = (extractBetween(body, "from ", ". Current Balance")
            ?: extractBetween(body, "from ", "  Current Balance")
            ?: extractBetween(body, "from ", " Current Balance")) ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        return ParsedTransaction(amount, fee, true, merchant.trim(), txId, reference, smsTimestamp, body)
    }

    private fun parseMerchantYello(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Y'ello. You have Paid", body) ?: return null
        val merchantMsg = extractBetween(body, "Message from sender: ", ". Your") ?: extractBetween(body, "Message from sender: ", ".") ?: ""
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val date = extractDate(body, smsTimestamp)
        return ParsedTransaction(amount, fee, false, merchantMsg.trim(), txId, null, date, body)
    }

    private fun parseMerchantConfirmation(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Your payment of", body) ?: return null
        val merchant = extractBetween(body, "to ", " has been completed") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        val date = extractDate(body, smsTimestamp)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, reference, date, body)
    }

    private fun parseCashOut(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Cash Out made for", body) ?: return null
        val merchant = extractBetween(body, "to ", ". Current") ?: extractBetween(body, "to ", ".") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, null, smsTimestamp, body)
    }

    private fun parsePaymentFor(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Payment for", body) ?: return null
        val merchant = extractMerchantBeforeCurrentBalance(body) ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        return ParsedTransaction(amount, fee, false, merchant, txId, null, smsTimestamp, body)
    }

    private fun parseBillPayment(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("Payment for", body) ?: return null
        val merchant = extractMerchantBeforeCurrentBalance(body)
            ?: extractBetween(body, "to ", " ..")
            ?: extractBetween(body, "to ", "..")
            ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, null, smsTimestamp, body)
    }

    private fun parseBankTransfer(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amount = extractAmount("You have received", body) ?: return null
        val merchant = extractBetween(body, "from ", " on your") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val date = extractDate(body, smsTimestamp)
        return ParsedTransaction(amount, 0.0, true, merchant.trim(), txId, null, date, body)
    }

    private fun parseCashTransfer(body: String, smsTimestamp: Long): ParsedTransaction? {
        val amountMatch = Regex("""received\s+([\d,]+\.?\d*)\s*GHS""").find(body)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val agent = extractBetween(body, "through agent ", " - ") ?: extractBetween(body, "through agent ", " on your") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val date = extractDate(body, smsTimestamp)
        return ParsedTransaction(amount, fee, true, agent.trim(), txId, null, date, body)
    }

    private fun extractAmount(prefix: String, body: String): Double? {
        val escapedPrefix = Regex.escape(prefix)
        val patterns = listOf(
            Regex("""$escapedPrefix\s*GHS\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""$escapedPrefix\s*([\d,]+\.?\d*)\s*GHS""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractTransactionId(body: String): String? {
        val patterns = listOf(
            Regex("""Financial Transaction Id:\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Transaction ID:\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Transaction Id:\s*(\d+)""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractFee(body: String): Double {
        val patterns = listOf(
            Regex("""Fee charged:\s*GHS\s*(\d+\.?\d{0,2})""", RegexOption.IGNORE_CASE),
            Regex("""Fee was\s*GHS\s*(\d+\.?\d{0,2})""", RegexOption.IGNORE_CASE),
            Regex("""TRANSACTION FEE:\s*(\d+\.?\d{0,2})""", RegexOption.IGNORE_CASE),
            Regex("""Fee was\s*(\d+\.?\d{0,2})\s*GHS""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) return match.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractReference(body: String): String? {
        val match = Regex("""Reference:\s*([^.]+)""").find(body) ?: return null
        val ref = match.groupValues[1].trim()
        return ref.ifEmpty { null }
    }

    private fun extractDate(body: String, fallbackTimestamp: Long): Long {
        val match = Regex("""(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})""").find(body)
        if (match != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                return sdf.parse(match.groupValues[1])?.time ?: fallbackTimestamp
            } catch (_: Exception) { }
        }
        return fallbackTimestamp
    }

    private fun extractMerchantBeforeCurrentBalance(body: String): String? {
        val match = Regex("""\bto\s+(.+?)\s*\.{0,2}\s*Current Balance""", RegexOption.IGNORE_CASE).find(body)
        return match?.groupValues?.get(1)?.trim()?.trimEnd('.')
    }

    private fun extractBetween(body: String, start: String, end: String): String? {
        val startIdx = body.indexOf(start)
        if (startIdx == -1) return null
        val fromIdx = startIdx + start.length
        val endIdx = body.indexOf(end, fromIdx)
        if (endIdx == -1) return null
        return body.substring(fromIdx, endIdx)
    }
}
