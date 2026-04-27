package com.ayko.trackitt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayko.trackitt.data.local.dao.BudgetDao
import com.ayko.trackitt.data.local.dao.CustomCategoryDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.entity.BudgetEntity
import com.ayko.trackitt.data.local.entity.CustomCategoryEntity
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN needsClassification INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [TransactionEntity::class, MerchantMappingEntity::class, CustomCategoryEntity::class, BudgetEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class TrackittDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: TrackittDatabase? = null

        fun getInstance(context: Context): TrackittDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TrackittDatabase::class.java,
                    "trackitt.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
