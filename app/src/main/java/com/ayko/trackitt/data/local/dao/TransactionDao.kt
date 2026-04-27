package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val total: Double,
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("""
        SELECT * FROM transactions
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        ORDER BY date DESC
    """)
    fun getByMonth(year: String, month: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startOfDay AND date < :endOfDay
        ORDER BY date DESC
    """)
    fun getByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT category, SUM(amount) as total FROM transactions
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(year: String, month: String): Flow<List<CategoryTotal>>

    @Query("""
        SELECT category, SUM(amount + fee) as total FROM transactions
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsWithFees(year: String, month: String): Flow<List<CategoryTotal>>

    @Query("""
        SELECT category, SUM(amount) as total FROM transactions
        WHERE date >= :startOfDay AND date < :endOfDay
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsForRange(startOfDay: Long, endOfDay: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT category, SUM(amount + fee) as total FROM transactions
        WHERE date >= :startOfDay AND date < :endOfDay
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsForRangeWithFees(startOfDay: Long, endOfDay: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalForRange(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount + fee), 0.0) FROM transactions
        WHERE isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalForRangeWithFees(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getMonthTotal(year: String, month: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount + fee), 0.0) FROM transactions
        WHERE isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getMonthTotalWithFees(year: String, month: String): Flow<Double>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchByMerchant(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE category = :category
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        ORDER BY date DESC
    """)
    fun getByCategory(category: String, year: String, month: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE category = :category
        AND date >= :startOfDay AND date < :endOfDay
        ORDER BY date DESC
    """)
    fun getByCategoryForRange(category: String, startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): Flow<TransactionEntity?>

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE transactions SET merchant = :merchant WHERE id = :id")
    suspend fun updateMerchant(id: Long, merchant: String)

    @Query("UPDATE transactions SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("UPDATE transactions SET amount = :amount WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Double)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions WHERE needsClassification = 1")
    suspend fun getPendingClassification(): List<TransactionEntity>

    @Query("UPDATE transactions SET needsClassification = :needs WHERE id = :id")
    suspend fun updateNeedsClassification(id: Long, needs: Boolean)
}
