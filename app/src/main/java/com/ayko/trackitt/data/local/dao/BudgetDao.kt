package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY CASE WHEN category = 'OTHER' THEN 1 ELSE 0 END, amount DESC")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY CASE WHEN category = 'OTHER' THEN 1 ELSE 0 END, amount DESC")
    suspend fun getAllNow(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
