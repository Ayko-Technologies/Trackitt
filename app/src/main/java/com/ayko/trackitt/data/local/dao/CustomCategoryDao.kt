package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAll(): Flow<List<CustomCategoryEntity>>

    @Insert
    suspend fun insert(category: CustomCategoryEntity): Long

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CustomCategoryEntity?
}
