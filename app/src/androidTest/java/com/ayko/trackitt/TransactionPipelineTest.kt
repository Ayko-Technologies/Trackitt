package com.ayko.trackitt

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.sms.MoMoSmsParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionPipelineTest {

    private lateinit var db: TrackittDatabase
    private lateinit var repository: TransactionRepository
    private val parser = MoMoSmsParser()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TrackittDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val categorizationService = DictionaryCategorizationService(
            userLookup = { merchant ->
                val mapping = db.merchantMappingDao().getMapping(merchant)
                mapping?.let {
                    try { TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
                }
            }
        )

        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            merchantMappingDao = db.merchantMappingDao(),
            categorizationService = categorizationService,
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun sms_parsed_categorized_and_stored() = runTest {
        val sms = "Payment made for GHS 355.00 to REBECCA NUETEY Current Balance: GHS 13.06 . Available Balance: GHS 13.06. Reference: . Transaction ID: 79917286657. Fee charged: GHS2.66 Tax charged: 0."
        val parsed = parser.parse(sms)!!
        repository.insertFromSms(parsed)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(355.00, tx.amount, 0.01)
        assertEquals(2.66, tx.fee, 0.01)
        assertEquals("REBECCA NUETEY", tx.merchant)
        assertEquals(TransactionCategory.OTHER, tx.category)
        assertFalse(tx.isIncome)
        assertTrue(tx.isFromSms)
    }

    @Test
    fun merchant_payment_auto_categorized_as_shopping() = runTest {
        val sms = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 79851134041."
        val parsed = parser.parse(sms)!!
        repository.insertFromSms(parsed)

        val tx = repository.getById(1).first()!!
        assertEquals(TransactionCategory.SHOPPING, tx.category)
        assertEquals("MELCOM COMMUNITY 25", tx.merchant)
    }

    @Test
    fun duplicate_sms_ignored() = runTest {
        val sms1 = "Y'ello. You have Paid GHS 85.47 to Merchant 032234 on your mobile money account at 202623110051. Message from sender: melcom. Your new balance: GHS 240.72 . Fee was GHS 0.85 . Financial Transaction Id: 79851134041."
        val sms2 = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 79851134041."

        repository.insertFromSms(parser.parse(sms1)!!)
        repository.insertFromSms(parser.parse(sms2)!!)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun income_transaction_parsed_correctly() = runTest {
        val sms = "You have received GHS 400.00 from Ecobank ova on your mobile money account at 2026-04-23 11:30:29. Message from sender: . Your new balance:GHS 408.72. Financial Transaction Id: 79852151833."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        assertTrue(tx.isIncome)
        assertEquals(400.00, tx.amount, 0.01)
        assertEquals(TransactionCategory.INCOME, tx.category)
    }

    @Test
    fun category_totals_sum_correctly() = runTest {
        val sms1 = "Payment made for GHS 100.00 to SHOPRITE ACCRA Current Balance: GHS 500.00 . Available Balance: GHS 500.00. Reference: . Transaction ID: 1001. Fee charged: GHS0.00 Tax charged: 0."
        val sms2 = "Payment made for GHS 50.00 to SHOPRITE WEST Current Balance: GHS 450.00 . Available Balance: GHS 450.00. Reference: . Transaction ID: 1002. Fee charged: GHS0.00 Tax charged: 0."
        val sms3 = "Payment made for GHS 30.00 to BOLT RIDE Current Balance: GHS 420.00 . Available Balance: GHS 420.00. Reference: . Transaction ID: 1003. Fee charged: GHS0.00 Tax charged: 0."

        repository.insertFromSms(parser.parse(sms1)!!)
        repository.insertFromSms(parser.parse(sms2)!!)
        repository.insertFromSms(parser.parse(sms3)!!)

        val now = LocalDate.now()
        val totals = repository.getCategoryTotals(now.year, now.monthValue).first()
        val groceriesTotal = totals.find { it.category == "GROCERIES" }?.total ?: 0.0
        val transportTotal = totals.find { it.category == "TRANSPORT" }?.total ?: 0.0

        assertEquals(150.00, groceriesTotal, 0.01)
        assertEquals(30.00, transportTotal, 0.01)
    }

    @Test
    fun user_category_override_persists() = runTest {
        val sms = "Payment made for GHS 40.00 to VIVIAN ANNOBAH Current Balance: GHS 327.04 . Available Balance: GHS 327.04. Reference: Baber. Transaction ID: 79849891508. Fee charged: GHS0.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        assertEquals(TransactionCategory.OTHER, tx.category)

        repository.updateCategory(tx.id, TransactionCategory.FOOD, tx.merchant)

        val updated = repository.getById(1).first()!!
        assertEquals(TransactionCategory.FOOD, updated.category)

        val mapping = db.merchantMappingDao().getMapping("vivian annobah")
        assertNotNull(mapping)
        assertEquals("FOOD", mapping!!.category)
    }

    @Test
    fun user_override_applies_to_future_transactions() = runTest {
        db.merchantMappingDao().upsert(MerchantMappingEntity("vivian annobah", "FOOD"))

        val sms = "Payment made for GHS 40.00 to VIVIAN ANNOBAH Current Balance: GHS 327.04 . Available Balance: GHS 327.04. Reference: . Transaction ID: 99999. Fee charged: GHS0.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        assertEquals(TransactionCategory.FOOD, tx.category)
    }

    @Test
    fun search_finds_transactions_by_merchant() = runTest {
        val sms1 = "Payment made for GHS 100.00 to SHOPRITE ACCRA Current Balance: GHS 500.00 . Available Balance: GHS 500.00. Reference: . Transaction ID: 2001. Fee charged: GHS0.00 Tax charged: 0."
        val sms2 = "Payment made for GHS 40.00 to VIVIAN ANNOBAH Current Balance: GHS 460.00 . Available Balance: GHS 460.00. Reference: . Transaction ID: 2002. Fee charged: GHS0.00 Tax charged: 0."

        repository.insertFromSms(parser.parse(sms1)!!)
        repository.insertFromSms(parser.parse(sms2)!!)

        val results = repository.searchByMerchant("SHOPRITE").first()
        assertEquals(1, results.size)
        assertEquals("SHOPRITE ACCRA", results[0].merchant)
    }

    @Test
    fun delete_removes_transaction() = runTest {
        val sms = "Payment made for GHS 100.00 to TEST MERCHANT Current Balance: GHS 500.00 . Available Balance: GHS 500.00. Reference: . Transaction ID: 3001. Fee charged: GHS0.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        repository.delete(tx.id)

        val deleted = repository.getById(1).first()
        assertNull(deleted)
    }

    @Test
    fun cash_out_and_bill_payment_parsed() = runTest {
        val cashOut = "Cash Out made for GHS200.00 to VICTORIOUS EUNILUV VENTURES. Current Balance: GHS38.72 Financial Transaction Id: 79851433208. Cash-out fee is charged automatically from your MTN MoMo wallet. Please do not pay any fees to the Agent. Thank you for using MTN MobileMoney. Fee charged: GHS2.00."
        val bill = "Payment for GHS4510.00 to Bills.INV ..Current Balance: GHS 22.96. Transaction Id: 79799129949. Fee charged: GHS0.00,Tax Charged 0."

        repository.insertFromSms(parser.parse(cashOut)!!)
        repository.insertFromSms(parser.parse(bill)!!)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()
        assertEquals(2, transactions.size)

        val cashOutTx = transactions.find { it.merchant == "VICTORIOUS EUNILUV VENTURES" }!!
        assertEquals(200.00, cashOutTx.amount, 0.01)
        assertEquals(2.00, cashOutTx.fee, 0.01)

        val billTx = transactions.find { it.merchant == "Bills.INV" }!!
        assertEquals(4510.00, billTx.amount, 0.01)
        assertEquals(TransactionCategory.BILLS, billTx.category)
    }
}
