package com.ayko.trackitt.data.repository

import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.budget.BudgetAlertService
import com.ayko.trackitt.service.categorization.ClassificationResult
import com.ayko.trackitt.service.categorization.SmartCategorizationService
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
) {

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

    suspend fun insertFromSms(parsed: ParsedTransaction): Boolean {
        if (parsed.isIncome) {
            val entity = parsed.toEntity(TransactionCategory.INCOME, needsClassification = false)
            return transactionDao.insert(entity) != -1L
        }

        val result = smartCategorization.classify(parsed.merchant, parsed.reference, null)
        val category: TransactionCategory
        val needsClassification: Boolean
        when (result) {
            is ClassificationResult.Classified -> {
                category = result.category
                needsClassification = false
            }
            is ClassificationResult.Unavailable -> {
                category = TransactionCategory.OTHER
                needsClassification = true
            }
        }

        val entity = parsed.toEntity(category, needsClassification)
        val inserted = transactionDao.insert(entity) != -1L
        if (inserted) {
            budgetAlerts.onInserted(entity)
        }
        return inserted
    }

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

    suspend fun retryPendingClassifications() {
        val pending = transactionDao.getPendingClassification()
        for (entity in pending) {
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
        )
    }
}
