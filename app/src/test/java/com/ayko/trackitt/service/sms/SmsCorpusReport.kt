package com.ayko.trackitt.service.sms

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Local-only harness: runs the SMS parser over an exported inbox and writes what it extracted.
 * Skipped unless TRACKITT_SMS_CORPUS points at a corpus file. Never commit a corpus or report.
 *
 * Corpus lines: id \t base64(sender) \t timestampMillis \t base64(body)
 * Report lines: id \t sender \t source \t income \t amount \t fee \t merchant \t txId \t reference \t date \t balance
 * Receipt lines: R \t id \t sender \t amount \t party \t date
 */
class SmsCorpusReport {

    @Test
    fun writeReport() {
        val corpus = System.getenv("TRACKITT_SMS_CORPUS")
        assumeTrue(corpus != null && File(corpus).exists())
        val out = File(System.getenv("TRACKITT_SMS_REPORT") ?: "$corpus.report.tsv")
        val parser = SmsTransactionParser()
        val decoder = Base64.getDecoder()

        out.bufferedWriter().use { writer ->
            File(corpus!!).forEachLine { line ->
                val (id, sender64, date, body64) = line.split('\t')
                val sender = String(decoder.decode(sender64))
                val body = String(decoder.decode(body64))
                val parsed = parser.parse(sender, body, date.toLong())
                if (parsed == null) {
                    val receipt = parser.parseReceipt(sender, body, date.toLong()) ?: return@forEachLine
                    val fields = listOf("R", id, sender, receipt.amount.toString(), receipt.party, receipt.date.toString())
                    writer.write(fields.joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') })
                    writer.newLine()
                    return@forEachLine
                }
                val fields = listOf(
                    id, sender, parsed.source, parsed.isIncome.toString(), parsed.amount.toString(),
                    parsed.fee.toString(), parsed.merchant, parsed.transactionId, parsed.reference.orEmpty(),
                    parsed.date.toString(), parsed.balance?.toString().orEmpty(),
                )
                writer.write(fields.joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') })
                writer.newLine()
            }
        }
    }
}
