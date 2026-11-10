package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.PayeeNameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeNameDao {

    @Query("SELECT * FROM payee_names WHERE number = :number")
    suspend fun get(number: String): PayeeNameEntity?

    @Query("SELECT name FROM payee_names WHERE number = :number")
    fun observeName(number: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(name: PayeeNameEntity)

    @Query("DELETE FROM payee_names WHERE number = :number")
    suspend fun delete(number: String)
}
