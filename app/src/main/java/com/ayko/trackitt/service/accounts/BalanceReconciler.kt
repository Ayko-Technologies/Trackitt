package com.ayko.trackitt.service.accounts

import com.ayko.trackitt.data.local.entity.TransactionEntity
import kotlin.math.abs

data class AccountBalance(
    val source: String,
    val balance: Double,
    val asOf: Long,
    /** Places where the reported balance doesn't follow from the previous one: likely a missed SMS. */
    val gaps: List<BalanceGap>,
)

data class BalanceGap(
    val after: Long,
    val before: Long,
    /** Positive: money arrived that no SMS mentioned. Negative: money left that no SMS mentioned. */
    val unexplained: Double,
)

/**
 * Every wallet and bank SMS states the balance afterwards, so consecutive messages must add up:
 * previous balance ± this amount (and its fees) = this balance. When they don't, a transaction
 * happened without an SMS reaching the phone.
 */
object BalanceReconciler {

    fun reconcile(transactions: List<TransactionEntity>): List<AccountBalance> =
        transactions
            .filter { it.isFromSms && it.balance != null }
            .groupBy { it.source }
            .map { (source, items) -> reconcileAccount(source, items.sortedWith(compareBy({ it.date }, { it.id }))) }
            .sortedByDescending { it.asOf }

    private fun reconcileAccount(source: String, items: List<TransactionEntity>): AccountBalance {
        val gaps = items.zipWithNext().mapNotNull { (previous, current) ->
            val unexplained = unexplained(previous.balance!!, current)
            if (abs(unexplained) <= tolerance(current)) null
            else BalanceGap(after = previous.date, before = current.date, unexplained = unexplained)
        }
        val latest = items.last()
        return AccountBalance(source, latest.balance!!, latest.date, gaps)
    }

    private fun unexplained(previousBalance: Double, current: TransactionEntity): Double {
        val change = if (current.isIncome) current.amount else -current.amount
        val withoutFee = current.balance!! - (previousBalance + change)
        val withFee = withoutFee + current.fee
        return if (abs(withFee) < abs(withoutFee)) withFee else withoutFee
    }

    /**
     * Levies and charges aren't always itemised (the 1.5% E-levy of 2022-2024 rarely was), so small
     * differences in proportion to the amount aren't treated as a missing SMS.
     */
    private fun tolerance(transaction: TransactionEntity) = maxOf(0.05, transaction.amount * 0.02)
}
