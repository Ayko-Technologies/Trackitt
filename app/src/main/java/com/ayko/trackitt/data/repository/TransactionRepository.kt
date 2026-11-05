package com.ayko.trackitt.data.repository

import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.dao.SmsReceiptDao
import com.ayko.trackitt.data.local.entity.SmsReceiptEntity
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.budget.BudgetAlertService
import com.ayko.trackitt.service.categorization.ClassificationResult
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.accounts.AccountBalance
import com.ayko.trackitt.service.accounts.BalanceReconciler
import com.ayko.trackitt.service.accounts.OwnTransferMatcher
import com.ayko.trackitt.service.accounts.ReceiptMatcher
import com.ayko.trackitt.service.sms.ParsedReceipt
import com.ayko.trackitt.service.sms.ParsedTransaction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val smartCategorization: SmartCategorizationService,
    private val budgetAlerts: BudgetAlertService,
    private val smsReceiptDao: SmsReceiptDao,
) {
    private val classificationLock = kotlinx.coroutines.sync.Mutex()

    fun getByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getByMonth(y, m).map { list -> list.map { it.toDomain() } }
    }

    fun getToday(): Flow<List<Transaction>> {
        val (start, end) = todayRange()
        return transactionDao.getByDateRange(start, end).map { list -> list.map { it.toDomain() } }
    }

    fun getCategoryTotals(year: Int, month: Int, includeFees: Boolean = false): Flow<List<CategoryTotal>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return if (includeFees) transactionDao.getCategoryTotalsWithFees(y, m)
        else transactionDao.getCategoryTotals(y, m)
    }

    fun getTodayTotal(): Flow<Double> {
        val (start, end) = todayRange()
        return transactionDao.getTotalForRange(start, end)
    }

    fun getCategoryTotalsForRange(startOfDay: Long, endOfDay: Long, includeFees: Boolean = false): Flow<List<CategoryTotal>> {
        return if (includeFees) transactionDao.getCategoryTotalsForRangeWithFees(startOfDay, endOfDay)
        else transactionDao.getCategoryTotalsForRange(startOfDay, endOfDay)
    }

    fun getDayTotal(startOfDay: Long, endOfDay: Long, includeFees: Boolean = false): Flow<Double> {
        return if (includeFees) transactionDao.getTotalForRangeWithFees(startOfDay, endOfDay)
        else transactionDao.getTotalForRange(startOfDay, endOfDay)
    }

    fun getMonthTotal(year: Int, month: Int, includeFees: Boolean = false): Flow<Double> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return if (includeFees) transactionDao.getMonthTotalWithFees(y, m)
        else transactionDao.getMonthTotal(y, m)
    }

    fun getByCategory(category: String, year: Int, month: Int): Flow<List<Transaction>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getByCategory(category, y, m).map { list -> list.map { it.toDomain() } }
    }

    fun getByCategoryForRange(category: String, startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> {
        return transactionDao.getByCategoryForRange(category, startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    fun getByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    fun searchByMerchant(query: String): Flow<List<Transaction>> {
        return transactionDao.searchByMerchant(query).map { list -> list.map { it.toDomain() } }
    }

    fun getById(id: Long): Flow<Transaction?> {
        return transactionDao.getById(id).map { it?.toDomain() }
    }

    /**
     * Stores an SMS transaction. In a [bulk] import only the instant dictionary categorises it; the rest
     * are queued for on-device AI afterwards, so importing years of SMS takes minutes rather than hours.
     */
    suspend fun insertFromSms(sms: ParsedTransaction, bulk: Boolean = false): Boolean {
        val parsed = withDistinctId(sms) ?: return false
        val entity = if (parsed.isIncome) {
            parsed.toEntity(TransactionCategory.INCOME, needsClassification = false)
        } else {
            when (val result = smartCategorization.classify(parsed.merchant, parsed.reference, null, allowAi = !bulk)) {
                is ClassificationResult.Classified -> parsed.toEntity(result.category, needsClassification = false)
                is ClassificationResult.Unavailable -> parsed.toEntity(TransactionCategory.OTHER, needsClassification = true)
            }
        }

        val rowId = transactionDao.insert(entity)
        if (rowId == -1L) return false
        val stored = entity.copy(id = rowId)
        if (!stored.isIncome) budgetAlerts.onInserted(stored)
        linkOwnTransfer(stored)
        matchPendingReceipt(stored)
        return true
    }

    /** Leaves one transaction out of every total, or puts it back. */
    suspend fun setExcluded(id: Long, excluded: Boolean) {
        val before = transactionDao.getByIdNow(id) ?: return
        transactionDao.updateExcluded(id, excluded)
        budgetAlerts.onUpdated(before, before.copy(isExcluded = excluded))
    }

    /**
     * The user says this is a transfer (money between their own accounts, or anything else that is
     * neither spending nor income). It stays in their history but out of every total.
     */
    suspend fun markAsTransfer(id: Long) {
        val before = transactionDao.getByIdNow(id) ?: return
        if (before.isOwnTransfer) return
        transactionDao.updateOwnTransfer(id, true, TransactionCategory.TRANSFER.name)
        budgetAlerts.onUpdated(before, before.copy(isOwnTransfer = true, category = TransactionCategory.TRANSFER.name))
    }

    private suspend fun restoredCategory(tx: TransactionEntity): TransactionCategory =
        if (tx.isIncome) {
            TransactionCategory.INCOME
        } else {
            when (val result = smartCategorization.classify(tx.merchant, tx.reference, tx.note)) {
                is ClassificationResult.Classified -> result.category
                is ClassificationResult.Unavailable -> TransactionCategory.OTHER
            }
        }


    /** Keeps a merchant receipt and, when its wallet transaction is already stored, renames that. */
    suspend fun recordReceipt(receipt: ParsedReceipt) {
        val entity = SmsReceiptEntity(
            sender = receipt.sender,
            amount = receipt.amount,
            party = receipt.party,
            date = receipt.date,
            rawSmsHash = sha1(receipt.rawSms.replace(Regex("""\s+"""), " ").trim()),
        )
        val rowId = smsReceiptDao.insert(entity)
        if (rowId == -1L) return
        val stored = entity.copy(id = rowId)
        val candidates = transactionDao.getBetween(
            stored.date - ReceiptMatcher.WINDOW_MILLIS,
            stored.date + ReceiptMatcher.WINDOW_MILLIS + 1,
        )
        val transaction = ReceiptMatcher.bestTransaction(stored, candidates) ?: return
        applyReceipt(transaction, stored)
    }

    /** Re-pairs own-account transfers across all history, e.g. after importing old SMS. */
    suspend fun relinkOwnTransfers() {
        val all = transactionDao.getAllChronological()
        OwnTransferMatcher.findPairs(all).forEach { (outgoing, incoming) ->
            transactionDao.updateOwnTransfer(outgoing, true, TransactionCategory.TRANSFER.name)
            transactionDao.updateOwnTransfer(incoming, true, TransactionCategory.TRANSFER.name)
        }
        all.filter { !it.isOwnTransfer && !it.ownTransferDismissed && OwnTransferMatcher.isLinkedAccountMove(it) }
            .forEach { transactionDao.updateOwnTransfer(it.id, true, TransactionCategory.TRANSFER.name) }
        smsReceiptDao.deleteUnmatchedBefore(System.currentTimeMillis() - RECEIPT_RETENTION_MILLIS)
    }

    /**
     * Older versions categorised payments to people as "Transfer" (and could hide them from spending).
     * Only money moved between the user's own accounts is a transfer now, so look at those payments
     * again by what they were for. Categories the user chose for a merchant are kept.
     */
    suspend fun reviewAutoTransfers() {
        val payments = transactionDao.getAllChronological().filter {
            !it.isIncome && !it.isOwnTransfer && it.category == TransactionCategory.TRANSFER.name
        }
        for (payment in payments) {
            if (merchantMappingDao.getMapping(payment.merchant.lowercase().trim()) != null) continue
            when (val result = smartCategorization.classify(payment.merchant, payment.reference, payment.note, allowAi = false)) {
                is ClassificationResult.Classified -> if (result.category != TransactionCategory.TRANSFER) {
                    transactionDao.updateCategory(payment.id, result.category.name)
                }
                // The on-device AI looks at the rest in the background.
                is ClassificationResult.Unavailable -> {
                    transactionDao.updateCategory(payment.id, TransactionCategory.OTHER.name)
                    transactionDao.updateNeedsClassification(payment.id, true)
                }
            }
        }
    }

    /**
     * Runs the keyword dictionary again over imported payments, so a larger dictionary also fixes
     * older guesses. Only a payment whose reference or note names what it was for is changed; payments
     * to a merchant the user has put in a category, and transactions added by hand, are left as they are.
     */
    suspend fun recategorizeFromDictionary() {
        val payments = transactionDao.getAllChronological().filter {
            it.isFromSms && !it.isIncome && !it.isOwnTransfer && it.category != TransactionCategory.TRANSFER.name
        }
        for (payment in payments) {
            if (merchantMappingDao.getMapping(payment.merchant.lowercase().trim()) != null) continue
            val category = smartCategorization.purposeOf(payment.reference, payment.note) ?: continue
            if (category.name != payment.category) transactionDao.updateCategory(payment.id, category.name)
            if (payment.needsClassification) transactionDao.updateNeedsClassification(payment.id, false)
        }
    }

    /** The user says this isn't money moved between their own accounts: restore both sides. */
    suspend fun unlinkOwnTransfer(id: Long) {
        val transaction = transactionDao.getByIdNow(id) ?: return
        if (!transaction.isOwnTransfer) return
        val nearby = transactionDao.getBetween(
            transaction.date - OwnTransferMatcher.WINDOW_MILLIS,
            transaction.date + OwnTransferMatcher.WINDOW_MILLIS + 1,
        )
        val counterpart = nearby
            .filter { it.id != id && it.isOwnTransfer && it.isIncome != transaction.isIncome && it.source != transaction.source }
            .filter { kotlin.math.abs(it.amount - transaction.amount) < 0.005 }
            .minByOrNull { kotlin.math.abs(it.date - transaction.date) }

        listOfNotNull(transaction, counterpart).forEach { side ->
            val category = restoredCategory(side)
            transactionDao.updateOwnTransfer(side.id, false, category.name)
            transactionDao.dismissOwnTransfer(side.id)
            budgetAlerts.onUpdated(side, side.copy(isOwnTransfer = false, category = category.name))
        }
    }

    fun observeAccountBalances(): Flow<List<AccountBalance>> =
        transactionDao.observeWithBalanceSince(0L).map { BalanceReconciler.reconcile(it) }

    private suspend fun linkOwnTransfer(transaction: TransactionEntity) {
        val nearby = transactionDao.getBetween(
            transaction.date - OwnTransferMatcher.WINDOW_MILLIS,
            transaction.date + OwnTransferMatcher.WINDOW_MILLIS + 1,
        )
        val counterpart = OwnTransferMatcher.counterpart(transaction, nearby)
        val sides = when {
            counterpart != null -> listOf(transaction, counterpart)
            OwnTransferMatcher.isLinkedAccountMove(transaction) -> listOf(transaction)
            else -> return
        }
        sides.forEach { side ->
            transactionDao.updateOwnTransfer(side.id, true, TransactionCategory.TRANSFER.name)
            budgetAlerts.onUpdated(side, side.copy(isOwnTransfer = true, category = TransactionCategory.TRANSFER.name))
        }
    }

    private suspend fun matchPendingReceipt(transaction: TransactionEntity) {
        if (!ReceiptMatcher.isIntermediary(transaction.merchant)) return
        val receipts = smsReceiptDao.getUnmatchedBetween(
            transaction.date - ReceiptMatcher.WINDOW_MILLIS,
            transaction.date + ReceiptMatcher.WINDOW_MILLIS + 1,
        )
        val receipt = ReceiptMatcher.bestReceipt(transaction, receipts) ?: return
        applyReceipt(transaction, receipt)
    }

    private suspend fun applyReceipt(transaction: TransactionEntity, receipt: SmsReceiptEntity) {
        val name = ReceiptMatcher.displayName(receipt)
        transactionDao.updateMerchant(transaction.id, name)
        smsReceiptDao.markMatched(receipt.id, transaction.id)
        val unsure = transaction.needsClassification || transaction.category == TransactionCategory.OTHER.name
        if (transaction.isIncome || transaction.isOwnTransfer || !unsure) return
        val result = smartCategorization.classify(name, transaction.reference, transaction.note)
        if (result is ClassificationResult.Classified) {
            transactionDao.updateCategory(transaction.id, result.category.name)
            transactionDao.updateNeedsClassification(transaction.id, false)
            budgetAlerts.onUpdated(transaction, transaction.copy(category = result.category.name, needsClassification = false))
        }
    }

    private fun sha1(text: String): String =
        java.security.MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    suspend fun insertManual(transaction: Transaction) {
        val entity = TransactionEntity(
            amount = transaction.amount,
            fee = 0.0,
            isIncome = transaction.isIncome,
            category = transaction.category.name,
            merchant = transaction.merchant,
            reference = null,
            transactionId = "manual_${System.currentTimeMillis()}",
            source = transaction.source,
            note = transaction.note,
            date = transaction.date,
            isFromSms = false,
            rawSms = null,
        )
        val inserted = transactionDao.insert(entity)
        if (inserted != -1L) {
            budgetAlerts.onInserted(entity)
        }
    }

    suspend fun updateCategory(id: Long, category: TransactionCategory, merchant: String) {
        val before = transactionDao.getByIdNow(id) ?: return
        transactionDao.updateCategory(id, category.name)
        transactionDao.updateNeedsClassification(id, false)
        merchantMappingDao.upsert(
            MerchantMappingEntity(
                merchant = merchant.lowercase().trim(),
                category = category.name,
            )
        )
        budgetAlerts.onUpdated(before, before.copy(category = category.name, needsClassification = false))
    }

    suspend fun updateMerchant(id: Long, merchant: String) {
        transactionDao.updateMerchant(id, merchant)
    }

    suspend fun updateNote(id: Long, note: String?, merchant: String = "", reference: String? = null) {
        val before = transactionDao.getByIdNow(id) ?: return
        transactionDao.updateNote(id, note)
        if (!note.isNullOrBlank()) {
            withContext(NonCancellable) {
                val result = smartCategorization.classify(merchant, reference, note)
                when (result) {
                    is ClassificationResult.Classified -> {
                        if (result.category != TransactionCategory.OTHER) {
                            transactionDao.updateCategory(id, result.category.name)
                        }
                        transactionDao.updateNeedsClassification(id, false)
                    }
                    is ClassificationResult.Unavailable -> {}
                }
            }
        }
        budgetAlerts.onUpdated(before, transactionDao.getByIdNow(id))
    }

    suspend fun updateAmount(id: Long, amount: Double) {
        val before = transactionDao.getByIdNow(id) ?: return
        transactionDao.updateAmount(id, amount)
        budgetAlerts.onUpdated(before, before.copy(amount = amount))
    }

    suspend fun delete(id: Long) {
        val before = transactionDao.getByIdNow(id) ?: return
        transactionDao.delete(id)
        budgetAlerts.onUpdated(before, null)
    }

    suspend fun retryPendingClassifications(onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }) {
        // The Home screen and the SMS import both ask for this; one pass at a time is enough.
        if (!classificationLock.tryLock()) return
        try {
            classifyPending(onProgress)
        } finally {
            classificationLock.unlock()
        }
    }

    private suspend fun classifyPending(onProgress: suspend (done: Int, total: Int) -> Unit) {
        val pending = transactionDao.getPendingClassification()
        for ((index, entity) in pending.withIndex()) {
            onProgress(index, pending.size)
            val result = smartCategorization.classify(entity.merchant, entity.reference, entity.note)
            if (result is ClassificationResult.Classified) {
                transactionDao.updateCategory(entity.id, result.category.name)
                transactionDao.updateNeedsClassification(entity.id, false)
                if (result.category.name != entity.category) {
                    budgetAlerts.onUpdated(entity, entity.copy(category = result.category.name, needsClassification = false))
                }
            }
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Some providers reuse one ID for different transactions (Ecobank sends the same REF with every
     * monthly card fee). Returns null when this SMS is already stored, otherwise a transaction whose
     * ID doesn't collide with a different stored one.
     */
    private suspend fun withDistinctId(parsed: ParsedTransaction): ParsedTransaction? {
        val existing = transactionDao.getByTransactionId(parsed.transactionId) ?: return parsed
        if (existing.isSameAs(parsed)) {
            fillMissingBalance(existing, parsed)
            return null
        }

        val day = java.time.Instant.ofEpochMilli(parsed.date).atZone(ZoneId.systemDefault()).toLocalDate()
        val distinct = parsed.copy(transactionId = "${parsed.transactionId}~$day")
        val existingDistinct = transactionDao.getByTransactionId(distinct.transactionId) ?: return distinct
        fillMissingBalance(existingDistinct, distinct)
        return null
    }

    /** Transactions stored before balances were recorded pick theirs up when their SMS is re-scanned. */
    private suspend fun fillMissingBalance(existing: TransactionEntity, parsed: ParsedTransaction) {
        if (existing.balance == null && parsed.balance != null) {
            transactionDao.updateBalance(existing.id, parsed.balance)
        }
    }

    private fun TransactionEntity.isSameAs(parsed: ParsedTransaction): Boolean {
        val whitespace = Regex("""\s+""")
        val sameSms = rawSms != null && rawSms.replace(whitespace, " ").trim() == parsed.rawSms.replace(whitespace, " ").trim()
        val sameMovement = kotlin.math.abs(amount - parsed.amount) < 0.005 &&
            kotlin.math.abs(date - parsed.date) < ONE_DAY_MILLIS
        return sameSms || sameMovement
    }

    private fun ParsedTransaction.toEntity(
        category: TransactionCategory,
        needsClassification: Boolean,
    ): TransactionEntity {
        return TransactionEntity(
            amount = amount,
            fee = fee,
            isIncome = isIncome,
            category = category.name,
            merchant = merchant,
            reference = reference,
            transactionId = transactionId,
            source = source,
            note = null,
            date = date,
            isFromSms = true,
            rawSms = rawSms,
            needsClassification = needsClassification,
            balance = balance,
        )
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            fee = fee,
            isIncome = isIncome,
            category = try { TransactionCategory.valueOf(category) } catch (_: Exception) { TransactionCategory.OTHER },
            merchant = merchant,
            reference = reference,
            transactionId = transactionId,
            source = source,
            note = note,
            date = date,
            isFromSms = isFromSms,
            rawSms = rawSms,
            balance = balance,
            isOwnTransfer = isOwnTransfer,
            isExcluded = isExcluded,
        )
    }

    private companion object {
        const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
        const val RECEIPT_RETENTION_MILLIS = 2L * 24 * 60 * 60 * 1000
    }
}
