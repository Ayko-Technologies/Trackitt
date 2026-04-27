package com.ayko.trackitt.data.repository

import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CategorizationService
import com.ayko.trackitt.service.sms.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val categorizationService: CategorizationService,
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
        val category = if (parsed.isIncome) TransactionCategory.INCOME
            else categorizationService.categorize(parsed.merchant, parsed.reference)
        val entity = TransactionEntity(
            amount = parsed.amount,
            fee = parsed.fee,
            isIncome = parsed.isIncome,
            category = category.name,
            merchant = parsed.merchant,
            reference = parsed.reference,
            transactionId = parsed.transactionId,
            source = parsed.source,
            note = null,
            date = parsed.date,
            isFromSms = true,
            rawSms = parsed.rawSms,
        )
        return transactionDao.insert(entity) != -1L
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
        transactionDao.insert(entity)
    }

    suspend fun updateCategory(id: Long, category: TransactionCategory, merchant: String) {
        transactionDao.updateCategory(id, category.name)
        merchantMappingDao.upsert(
            MerchantMappingEntity(
                merchant = merchant.lowercase().trim(),
                category = category.name,
            )
        )
    }

    suspend fun updateMerchant(id: Long, merchant: String) {
        transactionDao.updateMerchant(id, merchant)
    }

    suspend fun updateNote(id: Long, note: String?) {
        transactionDao.updateNote(id, note)
        if (!note.isNullOrBlank()) {
            val suggested = categorizationService.categorize("", note)
            if (suggested != TransactionCategory.OTHER) {
                transactionDao.updateCategory(id, suggested.name)
            }
        }
    }

    suspend fun updateAmount(id: Long, amount: Double) {
        transactionDao.updateAmount(id, amount)
    }

    suspend fun delete(id: Long) {
        transactionDao.delete(id)
    }

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        return start to end
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
