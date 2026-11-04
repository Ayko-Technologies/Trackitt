package com.ayko.trackitt.service.accounts

import com.ayko.trackitt.data.local.entity.SmsReceiptEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import kotlin.math.abs

/**
 * Pairs a merchant receipt with the wallet transaction it describes, so the transaction can show
 * who was really paid ("Shell Ring Road") instead of the payment service ("VPOS", "JUNIPAY").
 */
object ReceiptMatcher {

    const val WINDOW_MILLIS = 20L * 60 * 1000

    /** Wallet names that stand for a payment service rather than the merchant itself. */
    private val INTERMEDIARY = Regex(
        """\b(?:ova|push|vpos|m?pos|merchant|interoperability|bills?|pay)\b|zeepay|junipay|slydepay|expresspay|express pay|""" +
            """hubtel|paystack|payswitch|blupay|bigpay|rpay|tingg|kowri|ecobank pay""",
        RegexOption.IGNORE_CASE,
    )

    fun isIntermediary(merchant: String) = INTERMEDIARY.containsMatchIn(merchant)

    /** Payment services add their charge on top (GHS 155 to the shop is GHS 156.55 from the wallet). */
    fun amountsMatch(walletAmount: Double, receiptAmount: Double): Boolean {
        val tolerance = maxOf(1.0, receiptAmount * 0.03)
        return abs(walletAmount - receiptAmount) <= tolerance
    }

    fun matches(transaction: TransactionEntity, receipt: SmsReceiptEntity): Boolean =
        transaction.isFromSms &&
            receipt.matchedTransactionId == null &&
            isIntermediary(transaction.merchant) &&
            amountsMatch(transaction.amount, receipt.amount) &&
            abs(transaction.date - receipt.date) <= WINDOW_MILLIS

    /**
     * The name to show. A payment service's receipt names the merchant ("Acme Unit Trust"); a merchant's
     * own receipt may name only its branch ("Osu" from CityPharm), so keep the merchant's name too.
     */
    fun displayName(receipt: SmsReceiptEntity): String {
        val sender = receipt.sender.trim()
        if (isIntermediary(sender) || receipt.party.contains(sender, ignoreCase = true)) return receipt.party
        return "$sender (${receipt.party})"
    }

    fun bestTransaction(receipt: SmsReceiptEntity, candidates: List<TransactionEntity>): TransactionEntity? =
        candidates.filter { matches(it, receipt) }.minByOrNull { abs(it.date - receipt.date) }

    fun bestReceipt(transaction: TransactionEntity, candidates: List<SmsReceiptEntity>): SmsReceiptEntity? =
        candidates.filter { matches(transaction, it) }.minByOrNull { abs(it.date - transaction.date) }
}
