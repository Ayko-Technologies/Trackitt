package com.ayko.trackitt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MerchantMappingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TrackittDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao

    companion object {
        @Volatile
        private var INSTANCE: TrackittDatabase? = null

        fun getInstance(context: Context): TrackittDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TrackittDatabase::class.java,
                    "trackitt.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
