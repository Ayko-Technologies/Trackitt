package com.ayko.trackitt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ayko.trackitt.data.local.dao.CustomCategoryDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.entity.CustomCategoryEntity
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MerchantMappingEntity::class, CustomCategoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TrackittDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TrackittDatabase? = null

        fun getInstance(context: Context): TrackittDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TrackittDatabase::class.java,
                    "trackitt.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
