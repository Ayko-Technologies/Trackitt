package com.ayko.trackitt.service.accounts

import com.ayko.trackitt.data.local.entity.TransactionEntity
import java.util.Locale
import kotlin.math.abs

/**
 * Finds money moved between two of the user's own accounts: a debit on one wallet or bank and a
 * credit of the same amount on another, minutes apart. Those are neither spending nor income.
 *
 * Timing alone gives false pairs (paying someone GHS 50 while a friend sends you GHS 50), so a pair
 * also needs to be near-simultaneous, or one side has to name the other side's network or bank.
 */
object OwnTransferMatcher {

    const val WINDOW_MILLIS = 10L * 60 * 1000
    private const val SIMULTANEOUS_MILLIS = 2L * 60 * 1000

    fun isPair(outgoing: TransactionEntity, incoming: TransactionEntity): Boolean {
        if (outgoing.isIncome || !incoming.isIncome) return false
        if (!outgoing.isFromSms || !incoming.isFromSms) return false
        if (outgoing.isOwnTransfer || incoming.isOwnTransfer) return false
        if (outgoing.ownTransferDismissed || incoming.ownTransferDismissed) return false
        if (outgoing.source == incoming.source) return false
        if (abs(outgoing.amount - incoming.amount) >= 0.005) return false
        val gap = abs(outgoing.date - incoming.date)
        if (gap > WINDOW_MILLIS) return false
        return gap <= SIMULTANEOUS_MILLIS || referencesEachOther(outgoing, incoming)
    }

    /** The closest unpaired counterpart for [transaction] among [candidates], if any. */
    fun counterpart(transaction: TransactionEntity, candidates: List<TransactionEntity>): TransactionEntity? =
        candidates
            .filter { it.id != transaction.id }
            .filter { if (transaction.isIncome) isPair(it, transaction) else isPair(transaction, it) }
            .minByOrNull { abs(it.date - transaction.date) }

    /** Pairs across a whole history, as (outgoing id, incoming id). Each transaction is used once. */
    fun findPairs(transactions: List<TransactionEntity>): List<Pair<Long, Long>> {
        val sorted = transactions.sortedBy { it.date }
        val used = mutableSetOf<Long>()
        val pairs = mutableListOf<Pair<Long, Long>>()
        for ((index, outgoing) in sorted.withIndex()) {
            if (outgoing.isIncome || outgoing.id in used) continue
            val nearby = buildList {
                var i = index - 1
                while (i >= 0 && outgoing.date - sorted[i].date <= WINDOW_MILLIS) add(sorted[i--])
                i = index + 1
                while (i < sorted.size && sorted[i].date - outgoing.date <= WINDOW_MILLIS) add(sorted[i++])
            }.filter { it.id !in used }
            val incoming = counterpart(outgoing, nearby) ?: continue
            used += outgoing.id
            used += incoming.id
            pairs += outgoing.id to incoming.id
        }
        return pairs
    }

    private fun referencesEachOther(outgoing: TransactionEntity, incoming: TransactionEntity): Boolean {
        val outText = describe(outgoing)
        val inText = describe(incoming)
        if (networkWords(incoming.source).any { it in outText }) return true
        if (networkWords(outgoing.source).any { it in inText }) return true
        if (TRANSFER_MARKERS.any { it in outText || it in inText }) return true
        return sameName(outgoing.merchant, incoming.merchant)
    }

    private fun describe(t: TransactionEntity) = "${t.merchant} ${t.reference.orEmpty()}".lowercase(Locale.US)

    private fun networkWords(source: String): List<String> = when (source) {
        "MTN MoMo" -> listOf("mtn", "momo", "mobile money")
        "Telecel Cash" -> listOf("telecel", "vodafone", "t-cash")
        "AT Money" -> listOf("airteltigo", "at money")
        else -> listOf(source.lowercase(Locale.US).removeSuffix(" bank"))
    }

    /** "KOFI ADU MENSAH" and "MENSAH KOFI ADU": the user's own name, spelled by two providers. */
    private fun sameName(a: String, b: String): Boolean {
        val first = nameTokens(a)
        val second = nameTokens(b)
        return first.size >= 2 && second.size >= 2 && first.intersect(second).size >= 2
    }

    private fun nameTokens(name: String) = name.lowercase(Locale.US)
        .split(Regex("""[^a-z]+"""))
        .filter { it.length >= 3 }
        .toSet()

    private val TRANSFER_MARKERS = listOf("push", "→", "wallet2bank", "bank2wallet", " ova")
}
