package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity

@Dao
interface MerchantMappingDao {

    @Query("SELECT * FROM merchant_mappings WHERE merchant = :merchant")
    suspend fun getMapping(merchant: String): MerchantMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantMappingEntity)
}
