package com.ayko.trackitt

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.budget.BudgetAlertService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.categorization.NanoCategorizationService
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.sms.SmsTransactionParser
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

        val dictionary = DictionaryCategorizationService(
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
            smartCategorization = SmartCategorizationService(dictionary, NanoCategorizationService(), useOnDeviceAi = { false }),
            budgetAlerts = BudgetAlertService(context, db.budgetDao(), db.transactionDao()),
            smsReceiptDao = db.smsReceiptDao(),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun sms_parsed_categorized_and_stored() = runTest {
        val sms = "Payment made for GHS 355.00 to AKOSUA BOATENG Current Balance: GHS 13.06 . Available Balance: GHS 13.06. Reference: . Transaction ID: 70000000001. Fee charged: GHS2.66 Tax charged: 0."
        val parsed = parser.parse(sms)!!
        repository.insertFromSms(parsed)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(355.00, tx.amount, 0.01)
        assertEquals(2.66, tx.fee, 0.01)
        assertEquals("AKOSUA BOATENG", tx.merchant)
        assertEquals(TransactionCategory.OTHER, tx.category)
        assertFalse(tx.isIncome)
        assertTrue(tx.isFromSms)
    }

    @Test
    fun merchant_payment_auto_categorized_as_shopping() = runTest {
        val sms = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 70000000002."
        val parsed = parser.parse(sms)!!
        repository.insertFromSms(parsed)

        val tx = repository.getById(1).first()!!
        assertEquals(TransactionCategory.SHOPPING, tx.category)
        assertEquals("MELCOM COMMUNITY 25", tx.merchant)
    }

    @Test
    fun duplicate_sms_ignored() = runTest {
        val sms1 = "Y'ello. You have Paid GHS 85.47 to Merchant 032234 on your mobile money account at 200000000014. Message from sender: melcom. Your new balance: GHS 240.72 . Fee was GHS 0.85 . Financial Transaction Id: 70000000002."
        val sms2 = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 70000000002."

        repository.insertFromSms(parser.parse(sms1)!!)
        repository.insertFromSms(parser.parse(sms2)!!)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun income_transaction_parsed_correctly() = runTest {
        val sms = "You have received GHS 400.00 from Ecobank ova on your mobile money account at 2026-04-23 11:30:29. Message from sender: . Your new balance:GHS 408.72. Financial Transaction Id: 70000000006."
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
        val sms = "Payment made for GHS 40.00 to EFUA SAM Current Balance: GHS 327.04 . Available Balance: GHS 327.04. Reference: Baber. Transaction ID: 70000000004. Fee charged: GHS0.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        // The reference "Baber" makes the dictionary guess Beauty; the user then overrides it.
        assertEquals(TransactionCategory.BEAUTY, tx.category)

        repository.updateCategory(tx.id, TransactionCategory.FOOD, tx.merchant)

        val updated = repository.getById(1).first()!!
        assertEquals(TransactionCategory.FOOD, updated.category)

        val mapping = db.merchantMappingDao().getMapping("efua sam")
        assertNotNull(mapping)
        assertEquals("FOOD", mapping!!.category)
    }

    @Test
    fun user_override_applies_to_future_transactions() = runTest {
        db.merchantMappingDao().upsert(MerchantMappingEntity("efua sam", "FOOD"))

        val sms = "Payment made for GHS 40.00 to EFUA SAM Current Balance: GHS 327.04 . Available Balance: GHS 327.04. Reference: . Transaction ID: 99999. Fee charged: GHS0.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)

        val tx = repository.getById(1).first()!!
        assertEquals(TransactionCategory.FOOD, tx.category)
    }

    @Test
    fun search_finds_transactions_by_merchant() = runTest {
        val sms1 = "Payment made for GHS 100.00 to SHOPRITE ACCRA Current Balance: GHS 500.00 . Available Balance: GHS 500.00. Reference: . Transaction ID: 2001. Fee charged: GHS0.00 Tax charged: 0."
        val sms2 = "Payment made for GHS 40.00 to EFUA SAM Current Balance: GHS 460.00 . Available Balance: GHS 460.00. Reference: . Transaction ID: 2002. Fee charged: GHS0.00 Tax charged: 0."

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
        val cashOut = "Cash Out made for GHS200.00 to ACME AGENT VENTURES. Current Balance: GHS38.72 Financial Transaction Id: 70000000005. Cash-out fee is charged automatically from your MTN MoMo wallet. Please do not pay any fees to the Agent. Thank you for using MTN MobileMoney. Fee charged: GHS2.00."
        val bill = "Payment for GHS4510.00 to Bills.INV ..Current Balance: GHS 22.96. Transaction Id: 70000000003. Fee charged: GHS0.00,Tax Charged 0."

        repository.insertFromSms(parser.parse(cashOut)!!)
        repository.insertFromSms(parser.parse(bill)!!)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()
        assertEquals(2, transactions.size)

        val cashOutTx = transactions.find { it.merchant == "ACME AGENT VENTURES" }!!
        assertEquals(200.00, cashOutTx.amount, 0.01)
        assertEquals(2.00, cashOutTx.fee, 0.01)

        val billTx = transactions.find { it.merchant == "Bills.INV" }!!
        assertEquals(4510.00, billTx.amount, 0.01)
        // "Bills.INV" is a generic MoMo merchant code, deliberately not in the dictionary.
        assertEquals(TransactionCategory.OTHER, billTx.category)
    }

    @Test
    fun payment_made_with_period_before_current_balance() = runTest {
        val sms = "Payment made for GHS 10.00 to YAW OWUSU. Current Balance: GHS 4.81. Available Balance: GHS 4.81. Reference: motor. Transaction ID: 80000000009. Fee charged: GHS 0.00 TAX charged: GHS 0.00."
        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(10.00, parsed!!.amount, 0.01)
        assertEquals("YAW OWUSU", parsed.merchant)
        assertEquals("80000000009", parsed.transactionId)
        repository.insertFromSms(parsed)
        val tx = repository.getById(1).first()!!
        // The reference "motor" is a transport keyword in the dictionary.
        assertEquals(TransactionCategory.TRANSPORT, tx.category)
    }

    @Test
    fun payment_made_without_period_before_current_balance() = runTest {
        val sms = "Payment made for GHS 27.00 to ABENA MENSAH Current Balance: GHS 14.81 . Available Balance: GHS 14.81. Reference: soqp. Transaction ID: 80000000008. Fee charged: GHS0.00 Tax charged: 0."
        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(27.00, parsed!!.amount, 0.01)
        assertEquals("ABENA MENSAH", parsed.merchant)
    }

    @Test
    fun payment_received_with_double_space() = runTest {
        val sms = "Payment received for GHS 30.00 from KWAME ASANTE  Current Balance: GHS 41.81 . Available Balance: GHS 41.81. Reference: 1. Transaction ID: 80000000007. TRANSACTION FEE: 0.00"
        val parsed = parser.parse(sms)
        assertNotNull(parsed)
        assertEquals(30.00, parsed!!.amount, 0.01)
        assertEquals("KWAME ASANTE", parsed.merchant)
        assertTrue(parsed.isIncome)
    }

    @Test
    fun duplicate_sms_variants_same_transaction_id() = runTest {
        val sms1 = "Payment made for GHS 10.00 to YAW OWUSU. Current Balance: GHS 4.81. Available Balance: GHS 4.81. Reference: motor. Transaction ID: 80000000009. Fee charged: GHS 0.00 TAX charged: GHS 0.00."
        val sms2 = "Payment made for GHS 10.00 to YAW OWUSU Current Balance: GHS 4.81 . Available Balance: GHS 4.81. Reference: motor. Transaction ID: 80000000009. Fee charged: GHS0.00 Tax charged: 0. Download the MoMo App for a Faster & Easier Experience. Click here: https://mtnmymomo.onelink.me/XJOt/MoMo"

        repository.insertFromSms(parser.parse(sms1)!!)
        repository.insertFromSms(parser.parse(sms2)!!)

        val now = LocalDate.now()
        val transactions = repository.getByMonth(now.year, now.monthValue).first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun transfer_between_own_wallets_is_paired_and_left_out_of_totals() = runTest {
        val router = SmsTransactionParser()
        val now = System.currentTimeMillis()
        val out = "You have transferred GHS 600.00 to 0240000001 MTN MOBILE MONEY. Ref: Me. Transaction ID: 621300000001. Balance: GHS 390.00."
        val into = "Payment received for GHS 600.00 from GHANAPAY PUSH Current Balance: GHS 610.00 . Available Balance: GHS 610.00. Reference: Me. Transaction ID: 88800000001. TRANSACTION FEE: 0.00"

        repository.insertFromSms(router.parse("GhanaPay", out, now)!!)
        repository.insertFromSms(router.parse("MobileMoney", into, now + 60_000)!!)

        val month = LocalDate.now()
        val stored = repository.getByMonth(month.year, month.monthValue).first()
        assertEquals(2, stored.size)
        assertTrue(stored.all { it.isOwnTransfer && it.category == TransactionCategory.TRANSFER })
        assertEquals(0.0, repository.getMonthTotal(month.year, month.monthValue).first(), 0.001)
        assertTrue(repository.getCategoryTotals(month.year, month.monthValue).first().isEmpty())

        repository.unlinkOwnTransfer(stored.first().id)
        val unlinked = repository.getByMonth(month.year, month.monthValue).first()
        assertTrue(unlinked.none { it.isOwnTransfer })
        assertEquals(600.0, repository.getMonthTotal(month.year, month.monthValue).first(), 0.001)

        repository.relinkOwnTransfers()
        assertTrue(repository.getByMonth(month.year, month.monthValue).first().none { it.isOwnTransfer })
    }

    @Test
    fun merchant_receipt_renames_the_payment_service_on_the_wallet_transaction() = runTest {
        val router = SmsTransactionParser()
        val now = System.currentTimeMillis()
        val wallet = "Payment for GHS156.55 to JUNIPAY  .Current Balance: GHS 95.80. Transaction Id: 87500000001. Fee charged: GHS0.00,Tax Charged 0."
        val receipt = "GHS155 has been debited from your mtn account as payment to Acme Store Osu as Acme . Charges GHS 1.55. Reference: COL100000000001."

        repository.insertFromSms(router.parse("MobileMoney", wallet, now)!!)
        assertNull(router.parse("JuniPay", receipt, now + 5 * 60_000))
        repository.recordReceipt(router.parseReceipt("JuniPay", receipt, now + 5 * 60_000)!!)

        val tx = repository.getById(1).first()!!
        assertEquals("Acme Store Osu", tx.merchant)
        assertEquals(95.80, tx.balance!!, 0.001)
    }

    @Test
    fun excluded_purchase_drops_out_of_totals_and_comes_back() = runTest {
        val sms = "Payment made for GHS 100.00 to SOMEONE ELSE Current Balance: GHS 500.00 . Available Balance: GHS 500.00. Reference: . Transaction ID: 4001. Fee charged: GHS1.00 Tax charged: 0."
        repository.insertFromSms(parser.parse(sms)!!)
        val month = LocalDate.now()

        repository.setExcluded(1, true)
        assertEquals(0.0, repository.getMonthTotal(month.year, month.monthValue, includeFees = true).first(), 0.001)

        repository.setExcluded(1, false)
        assertEquals(101.0, repository.getMonthTotal(month.year, month.monthValue, includeFees = true).first(), 0.001)
    }

    @Test
    fun marking_a_transaction_as_a_transfer_takes_it_out_of_income_and_back() = runTest {
        val sms = "Payment received for GHS 1750.00 from Express Pay ova  Current Balance: GHS 1800.00 . Available Balance: GHS 1800.00. Reference: expressPay. Transaction ID: 93000000001. TRANSACTION FEE: 0.00"
        repository.insertFromSms(parser.parse(sms)!!)
        val month = LocalDate.now()
        fun income() = repository.getCategoryTotals(month.year, month.monthValue)

        assertEquals(1750.0, income().first().single { it.category == "INCOME" }.total, 0.001)

        repository.markAsTransfer(1)
        assertTrue(repository.getById(1).first()!!.isOwnTransfer)
        assertTrue(income().first().isEmpty())

        repository.unlinkOwnTransfer(1)
        val restored = repository.getById(1).first()!!
        assertFalse(restored.isOwnTransfer)
        assertEquals(TransactionCategory.INCOME, restored.category)
        assertEquals(1750.0, income().first().single { it.category == "INCOME" }.total, 0.001)
    }
}
