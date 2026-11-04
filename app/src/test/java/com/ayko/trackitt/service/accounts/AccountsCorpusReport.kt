package com.ayko.trackitt.service.accounts

import com.ayko.trackitt.data.local.entity.SmsReceiptEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.service.sms.SmsTransactionParser
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Local-only harness: runs own-transfer pairing, receipt matching and balance reconciliation over an
 * exported inbox (same corpus format as SmsCorpusReport). Skipped unless TRACKITT_SMS_CORPUS is set.
 */
class AccountsCorpusReport {

    @Test
    fun writeReport() {
        val corpus = System.getenv("TRACKITT_SMS_CORPUS")
        assumeTrue(corpus != null && File(corpus).exists())
        val out = File(System.getenv("TRACKITT_ACCOUNTS_REPORT") ?: "$corpus.accounts.txt")
        val parser = SmsTransactionParser()
        val decoder = Base64.getDecoder()

        val transactions = linkedMapOf<String, TransactionEntity>()
        val receipts = mutableListOf<SmsReceiptEntity>()
        File(corpus!!).forEachLine { line ->
            val (_, sender64, date, body64) = line.split('\t')
            val sender = String(decoder.decode(sender64))
            val body = String(decoder.decode(body64))
            val parsed = parser.parse(sender, body, date.toLong())
            if (parsed != null) {
                if (parsed.transactionId !in transactions) {
                    transactions[parsed.transactionId] = TransactionEntity(
                        id = transactions.size + 1L, amount = parsed.amount, fee = parsed.fee,
                        isIncome = parsed.isIncome, category = "OTHER", merchant = parsed.merchant,
                        reference = parsed.reference, transactionId = parsed.transactionId,
                        source = parsed.source, note = null, date = parsed.date, isFromSms = true,
                        rawSms = parsed.rawSms, balance = parsed.balance,
                    )
                }
            } else {
                parser.parseReceipt(sender, body, date.toLong())?.let {
                    receipts += SmsReceiptEntity(
                        id = receipts.size + 1L, sender = it.sender, amount = it.amount, party = it.party,
                        date = it.date, rawSmsHash = it.rawSms.hashCode().toString(),
                    )
                }
            }
        }

        val all = transactions.values.toList()
        val byId = all.associateBy { it.id }
        out.bufferedWriter().use { w ->
            val pairs = OwnTransferMatcher.findPairs(all)
            w.write("OWN TRANSFERS: ${pairs.size}\n")
            pairs.forEach { (o, i) ->
                val a = byId.getValue(o); val b = byId.getValue(i)
                w.write("  ${a.amount}\t${a.source} -> ${b.source}\t'${a.merchant}' / '${b.merchant}'\tdt=${(b.date - a.date) / 1000}s\n")
            }
            w.write("\nRECEIPT MATCHES:\n")
            receipts.forEach { r ->
                val tx = ReceiptMatcher.bestTransaction(r, all)
                w.write("  ${r.sender}\t${r.amount}\t'${r.party}'\t-> ${tx?.let { "'${it.merchant}' ${it.amount} ${it.source}" } ?: "no match"}\n")
            }
            w.write("\nBALANCES:\n")
            BalanceReconciler.reconcile(all).forEach { acc ->
                w.write("  ${acc.source}\tbalance=${acc.balance}\tasOf=${acc.asOf}\tgaps=${acc.gaps.size}\n")
                acc.gaps.takeLast(8).forEach { g -> w.write("     gap ${g.unexplained}\t${g.after}..${g.before}\n") }
            }
        }
    }
}
