package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.SmsReceiptEntity

@Dao
interface SmsReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: SmsReceiptEntity): Long

    @Query("""
        SELECT * FROM sms_receipts
        WHERE matchedTransactionId IS NULL
        AND date >= :start AND date < :end
        ORDER BY date
    """)
    suspend fun getUnmatchedBetween(start: Long, end: Long): List<SmsReceiptEntity>

    @Query("UPDATE sms_receipts SET matchedTransactionId = :transactionId WHERE id = :id")
    suspend fun markMatched(id: Long, transactionId: Long)

    @Query("DELETE FROM sms_receipts WHERE matchedTransactionId IS NULL AND date < :before")
    suspend fun deleteUnmatchedBefore(before: Long)
}
