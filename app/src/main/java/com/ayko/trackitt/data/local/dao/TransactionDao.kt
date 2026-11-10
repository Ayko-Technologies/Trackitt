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

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: String): TransactionEntity?

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

    /**
     * Spending and income by category, leaving out money moved between the user's own accounts. With
     * [includeFees], every fee paid is added up under FEES rather than in its payment's category.
     */
    @Query("""
        SELECT category, SUM(total) AS total FROM (
            SELECT category, amount AS total FROM transactions
            WHERE isExcluded = 0
            AND isOwnTransfer = 0
            AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
            AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
            UNION ALL
            SELECT 'FEES', fee FROM transactions
            WHERE :includeFees
            AND fee > 0
            AND isExcluded = 0
            AND isIncome = 0
            AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
            AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        )
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(year: String, month: String, includeFees: Boolean): Flow<List<CategoryTotal>>

    @Query("""
        SELECT category, SUM(total) AS total FROM (
            SELECT category, amount AS total FROM transactions
            WHERE isExcluded = 0
            AND isOwnTransfer = 0
            AND date >= :startOfDay AND date < :endOfDay
            UNION ALL
            SELECT 'FEES', fee FROM transactions
            WHERE :includeFees
            AND fee > 0
            AND isExcluded = 0
            AND isIncome = 0
            AND date >= :startOfDay AND date < :endOfDay
        )
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsForRange(startOfDay: Long, endOfDay: Long, includeFees: Boolean): Flow<List<CategoryTotal>>

    /**
     * Money moved between the user's own accounts: each debit, plus each credit whose debit came from
     * an account that doesn't send SMS, so a move texted by both sides counts once.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0.0) FROM transactions t
        WHERE t.isOwnTransfer = 1
        AND t.isExcluded = 0
        AND strftime('%Y', t.date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', t.date / 1000, 'unixepoch', 'localtime') = :month
        AND (t.isIncome = 0 OR NOT EXISTS (
            SELECT 1 FROM transactions o
            WHERE o.isOwnTransfer = 1 AND o.isIncome = 0 AND o.source != t.source
            AND ABS(o.amount - t.amount) < 0.005 AND ABS(o.date - t.date) <= 3600000
        ))
    """)
    fun getMovedBetweenOwnAccounts(year: String, month: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0.0) FROM transactions t
        WHERE t.isOwnTransfer = 1
        AND t.isExcluded = 0
        AND t.date >= :startOfDay AND t.date < :endOfDay
        AND (t.isIncome = 0 OR NOT EXISTS (
            SELECT 1 FROM transactions o
            WHERE o.isOwnTransfer = 1 AND o.isIncome = 0 AND o.source != t.source
            AND ABS(o.amount - t.amount) < 0.005 AND ABS(o.date - t.date) <= 3600000
        ))
    """)
    fun getMovedBetweenOwnAccountsForRange(startOfDay: Long, endOfDay: Long): Flow<Double>

    /** Payments that charged a fee, for the FEES list. */
    @Query("""
        SELECT * FROM transactions
        WHERE fee > 0
        AND isExcluded = 0
        AND isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        ORDER BY date DESC
    """)
    fun getWithFees(year: String, month: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE fee > 0
        AND isExcluded = 0
        AND isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
        ORDER BY date DESC
    """)
    fun getWithFeesForRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isOwnTransfer = 0
        AND isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalForRange(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN isOwnTransfer = 1 THEN fee ELSE amount + fee END), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalForRangeWithFees(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isOwnTransfer = 0
        AND isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getMonthTotal(year: String, month: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN isOwnTransfer = 1 THEN fee ELSE amount + fee END), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getMonthTotalWithFees(year: String, month: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isOwnTransfer = 0
        AND isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    suspend fun getMonthTotalNow(year: String, month: String): Double

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN isOwnTransfer = 1 THEN fee ELSE amount + fee END), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    suspend fun getMonthTotalWithFeesNow(year: String, month: String): Double

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' OR payeeNumber LIKE '%' || :query || '%' ORDER BY date DESC")
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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getByIdNow(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE isExcluded = 0
        AND isOwnTransfer = 0
        AND isIncome = 0
        AND date < :beforeDate
        ORDER BY date DESC
    """)
    suspend fun getExpensesBefore(beforeDate: Long): List<TransactionEntity>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isExcluded = 0
        AND isOwnTransfer = 0
        AND isIncome = 0
        AND category = :category
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    suspend fun getCategoryMonthTotal(category: String, year: String, month: String): Double

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE transactions SET merchant = :merchant WHERE id = :id")
    suspend fun updateMerchant(id: Long, merchant: String)

    /** The categories of the latest payments to [merchant] (lowercase), newest first. */
    @Query("""
        SELECT category FROM transactions
        WHERE LOWER(TRIM(merchant)) = :merchant AND isIncome = 0 AND isOwnTransfer = 0
        ORDER BY date DESC
        LIMIT :count
    """)
    suspend fun recentCategoriesOf(merchant: String, count: Int): List<String>

    @Query("UPDATE transactions SET merchant = :merchant WHERE payeeNumber = :number")
    suspend fun renamePayee(number: String, merchant: String)

    /** Payees of payments (other than to [number]) whose reference or note is one of [purposes]. */
    @Query("""
        SELECT merchant FROM transactions
        WHERE isOwnTransfer = 0 AND (payeeNumber IS NULL OR payeeNumber != :number)
          AND (LOWER(TRIM(reference)) IN (:purposes) OR LOWER(TRIM(note)) IN (:purposes))
        ORDER BY date DESC
        LIMIT 50
    """)
    suspend fun payeesWithPurpose(purposes: List<String>, number: String): List<String>

    @Query("UPDATE transactions SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("UPDATE transactions SET amount = :amount WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Double)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions WHERE needsClassification = 1 AND isOwnTransfer = 0")
    suspend fun getPendingClassification(): List<TransactionEntity>

    @Query("UPDATE transactions SET needsClassification = :needs WHERE id = :id")
    suspend fun updateNeedsClassification(id: Long, needs: Boolean)

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :start AND date < :end
        ORDER BY date
    """)
    suspend fun getBetween(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date")
    suspend fun getAllChronological(): List<TransactionEntity>

    // A transfer isn't waiting for a category any more, so the on-device AI leaves it alone.
    @Query("UPDATE transactions SET isOwnTransfer = :isOwnTransfer, category = :category, needsClassification = 0 WHERE id = :id")
    suspend fun updateOwnTransfer(id: Long, isOwnTransfer: Boolean, category: String)

    /** Puts back the Transfer category on transfers that an earlier version let the AI recategorise. */
    @Query("UPDATE transactions SET category = 'TRANSFER', needsClassification = 0 WHERE isOwnTransfer = 1 AND (category != 'TRANSFER' OR needsClassification = 1)")
    suspend fun repairOwnTransferCategories()

    @Query("UPDATE transactions SET ownTransferDismissed = 1 WHERE id = :id")
    suspend fun dismissOwnTransfer(id: Long)

    @Query("UPDATE transactions SET balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Double)

    @Query("""
        SELECT * FROM transactions
        WHERE isFromSms = 1 AND balance IS NOT NULL AND date >= :since
        ORDER BY source, date
    """)
    fun observeWithBalanceSince(since: Long): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET isExcluded = :excluded WHERE id = :id")
    suspend fun updateExcluded(id: Long, excluded: Boolean)

}
