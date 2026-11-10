package com.ayko.trackitt

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayko.trackitt.data.local.ALL_MIGRATIONS
import com.ayko.trackitt.data.local.MIGRATION_4_5
import com.ayko.trackitt.data.local.MIGRATION_5_6
import com.ayko.trackitt.data.local.MIGRATION_6_7
import com.ayko.trackitt.data.local.MIGRATION_7_8
import com.ayko.trackitt.data.local.TrackittDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every release shipped schema 4, so each later version must migrate from it without losing data.
 * When the schema changes, add the new migration to ALL_MIGRATIONS and a test here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackittDatabase::class.java,
    )

    @Test
    fun migrate_4_to_5_keeps_transactions_and_adds_new_fields() {
        helper.createDatabase(dbName, 4).apply {
            execSQL(
                "INSERT INTO transactions (amount, fee, isIncome, category, merchant, reference, transactionId, " +
                    "source, note, date, isFromSms, rawSms, needsClassification) VALUES " +
                    "(355.0, 2.66, 0, 'FOOD', 'AKOSUA BOATENG', NULL, '70000000001', 'MTN MoMo', 'lunch', " +
                    "1786000000000, 1, 'Payment made for GHS 355.00', 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, MIGRATION_4_5)

        db.query("SELECT amount, category, note, balance, isOwnTransfer, ownTransferDismissed FROM transactions").use {
            assertTrue(it.moveToFirst())
            assertEquals(355.0, it.getDouble(0), 0.001)
            assertEquals("FOOD", it.getString(1))
            assertEquals("lunch", it.getString(2))
            assertTrue(it.isNull(3))
            assertEquals(0, it.getInt(4))
            assertEquals(0, it.getInt(5))
        }
        db.query("SELECT COUNT(*) FROM sms_receipts").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate_5_to_6_adds_exclusion_and_own_account_rules() {
        helper.createDatabase(dbName, 5).apply {
            execSQL(
                "INSERT INTO transactions (amount, fee, isIncome, category, merchant, reference, transactionId, " +
                    "source, note, date, isFromSms, rawSms, needsClassification, balance, isOwnTransfer, " +
                    "ownTransferDismissed) VALUES (500.0, 0, 0, 'SHOPPING', 'AMAZON', NULL, 'eco_1', 'Ecobank', " +
                    "NULL, 1786000000000, 1, 'GHS 500.00 Debited', 0, 50.0, 0, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query("SELECT amount, balance, isExcluded FROM transactions").use {
            assertTrue(it.moveToFirst())
            assertEquals(500.0, it.getDouble(0), 0.001)
            assertEquals(50.0, it.getDouble(1), 0.001)
            assertEquals(0, it.getInt(2))
        }
        db.execSQL("INSERT INTO own_account_rules (merchant, isIncome) VALUES ('express pay ova', 1)")
    }

    @Test
    fun migrate_6_to_7_drops_own_account_rules_and_keeps_transactions() {
        helper.createDatabase(dbName, 6).apply {
            execSQL(
                "INSERT INTO transactions (amount, fee, isIncome, category, merchant, reference, transactionId, " +
                    "source, note, date, isFromSms, rawSms, needsClassification, balance, isOwnTransfer, " +
                    "ownTransferDismissed, isExcluded) VALUES (1750.0, 0, 1, 'TRANSFER', 'Express Pay ova', NULL, " +
                    "'93000000001', 'MTN MoMo', NULL, 1786000000000, 1, 'Payment received', 0, 1800.0, 1, 0, 1)"
            )
            execSQL("INSERT INTO own_account_rules (merchant, isIncome) VALUES ('express pay ova', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 7, true, MIGRATION_6_7)

        db.query("SELECT amount, isOwnTransfer, isExcluded FROM transactions").use {
            assertTrue(it.moveToFirst())
            assertEquals(1750.0, it.getDouble(0), 0.001)
            assertEquals(1, it.getInt(1))
            assertEquals(1, it.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM sqlite_master WHERE name = 'own_account_rules'").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate_7_to_8_records_the_payee_number_of_number_only_merchants() {
        helper.createDatabase(dbName, 7).apply {
            listOf("0240000001 (MTN MOBILE MONEY)" to "gp_1", "2335****150 , MTN MOBILE MONEY" to "gp_2", "SHOPRITE" to "mo_1")
                .forEach { (merchant, id) ->
                    execSQL(
                        "INSERT INTO transactions (amount, fee, isIncome, category, merchant, reference, transactionId, " +
                            "source, note, date, isFromSms, rawSms, needsClassification, balance, isOwnTransfer, " +
                            "ownTransferDismissed, isExcluded) VALUES (20.0, 0, 0, 'OTHER', '$merchant', NULL, " +
                            "'$id', 'GhanaPay', NULL, 1786000000000, 1, 'You have transferred', 0, NULL, 0, 0, 0)"
                    )
                }
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 8, true, MIGRATION_7_8)

        db.query("SELECT payeeNumber FROM transactions ORDER BY id").use {
            assertTrue(it.moveToNext())
            assertEquals("0240000001", it.getString(0))
            assertTrue(it.moveToNext())
            assertTrue(it.isNull(0))
            assertTrue(it.moveToNext())
            assertTrue(it.isNull(0))
        }
        db.execSQL("INSERT INTO payee_names (number, name) VALUES ('0240000001', 'Road Tech')")
    }

    @Test
    fun room_opens_a_release_database_with_all_migrations() {
        helper.createDatabase(dbName, 4).close()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db = Room.databaseBuilder(context, TrackittDatabase::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM transactions").close()
        db.close()
    }
}
