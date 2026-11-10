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
import com.ayko.trackitt.data.local.dao.PayeeNameDao
import com.ayko.trackitt.data.local.dao.SmsReceiptDao
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.entity.BudgetEntity
import com.ayko.trackitt.data.local.entity.CustomCategoryEntity
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.PayeeNameEntity
import com.ayko.trackitt.data.local.entity.SmsReceiptEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.service.accounts.PayeeNumber

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN needsClassification INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN balance REAL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isOwnTransfer INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN ownTransferDismissed INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sms_receipts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sender` TEXT NOT NULL, `amount` REAL NOT NULL, `party` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                "`rawSmsHash` TEXT NOT NULL, `matchedTransactionId` INTEGER)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_receipts_rawSmsHash` ON `sms_receipts` (`rawSmsHash`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_receipts_date` ON `sms_receipts` (`date`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `own_account_rules` (`merchant` TEXT NOT NULL, " +
                "`isIncome` INTEGER NOT NULL, PRIMARY KEY(`merchant`, `isIncome`))"
        )
    }
}

/** "Own money" rules were replaced by marking a transaction as a transfer; never part of a release. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `own_account_rules`")
    }
}

/**
 * Names for payees that SMS show only as a phone number. Categories are no longer remembered for a
 * phone number (usually a person, paid for different things), so those remembered before go.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN payeeNumber TEXT")
        db.execSQL("CREATE TABLE IF NOT EXISTS `payee_names` (`number` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`number`))")
        val numbers = mutableListOf<Pair<Long, String>>()
        db.query("SELECT id, merchant FROM transactions").use { cursor ->
            while (cursor.moveToNext()) {
                PayeeNumber.of(cursor.getString(1))?.let { numbers += cursor.getLong(0) to it }
            }
        }
        numbers.forEach { (id, number) ->
            db.execSQL("UPDATE transactions SET payeeNumber = ? WHERE id = ?", arrayOf<Any>(number, id))
        }
        val numberMappings = mutableListOf<String>()
        db.query("SELECT merchant FROM merchant_mappings").use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getString(0).takeIf { PayeeNumber.of(it) != null }?.let { numberMappings += it }
            }
        }
        numberMappings.forEach { db.execSQL("DELETE FROM merchant_mappings WHERE merchant = ?", arrayOf<Any>(it)) }
    }
}

/** Every migration, oldest first. A schema change without one here fails loudly instead of wiping data. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)

@Database(
    entities = [
        TransactionEntity::class, MerchantMappingEntity::class, CustomCategoryEntity::class, BudgetEntity::class,
        SmsReceiptEntity::class, PayeeNameEntity::class,
    ],
    version = TrackittDatabase.VERSION,
    exportSchema = true,
)
abstract class TrackittDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun smsReceiptDao(): SmsReceiptDao
    abstract fun payeeNameDao(): PayeeNameDao

    companion object {
        const val VERSION = 8

        /** Oldest schema with a migration path; anything older would be wiped on open. */
        const val MIN_MIGRATABLE_VERSION = 3

        @Volatile
        private var INSTANCE: TrackittDatabase? = null

        fun getInstance(context: Context): TrackittDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TrackittDatabase::class.java,
                    "trackitt.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Schemas 1 and 2 only existed in pre-release development builds and have no migration.
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** Closes the open database so its file can be replaced; the next getInstance reopens it. */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
