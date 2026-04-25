# Trackitt Logic Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all hardcoded UI data with Room persistence, MTN MoMo SMS parsing, auto-categorization, and Google Drive backup.

**Architecture:** ViewModel + Repository pattern. One `TransactionRepository` owns all data access. ViewModels expose Kotlin Flows to Compose screens. Manual DI via `AppContainer` in a custom Application class. No Hilt.

**Tech Stack:** Room 2.6.1, KSP, WorkManager 2.10.0, Kotlin Coroutines/Flow, Jetpack Compose, Google Drive API, Google Identity Services.

**Spec:** `docs/superpowers/specs/2026-04-25-logic-layer-design.md`

---

## File Map

### New Files

| File | Responsibility |
|---|---|
| `data/local/entity/TransactionEntity.kt` | Room entity for transactions |
| `data/local/entity/MerchantMappingEntity.kt` | Room entity for user category overrides |
| `data/local/dao/TransactionDao.kt` | Room DAO — all transaction queries |
| `data/local/dao/MerchantMappingDao.kt` | Room DAO — merchant mapping CRUD |
| `data/local/TrackittDatabase.kt` | Room database singleton |
| `data/repository/TransactionRepository.kt` | Single source of truth for data access |
| `service/sms/MoMoSmsParser.kt` | Pure function: SMS text → ParsedTransaction |
| `service/sms/ParsedTransaction.kt` | Data class for parser output |
| `service/sms/SmsBroadcastReceiver.kt` | Listens for incoming MoMo SMS |
| `service/sms/SmsBackfillWorker.kt` | WorkManager job: import last 90 days |
| `service/categorization/CategorizationService.kt` | Interface + dictionary implementation |
| `service/categorization/MerchantDictionary.kt` | Built-in Ghanaian merchant map |
| `ui/screen/home/HomeViewModel.kt` | Home screen state management |
| `ui/screen/add/AddViewModel.kt` | Add expense state management |
| `ui/screen/detail/DetailViewModel.kt` | Transaction detail state management |
| `ui/screen/search/SearchViewModel.kt` | Search state management |
| `TrackittApplication.kt` | Custom Application class with AppContainer |
| `AppContainer.kt` | Manual DI container |
| `data/backup/DriveBackupManager.kt` | Google Drive export/import |

### Modified Files

| File | Changes |
|---|---|
| `domain/model/Transaction.kt` | Change `id` to Long, `date` to Long (epoch millis), add `fee`, `isIncome`, `rawSms` |
| `app/build.gradle.kts` | Add test dependencies, Google Drive/Auth deps |
| `gradle/libs.versions.toml` | Add test library versions, Google deps |
| `AndroidManifest.xml` | Register BroadcastReceiver, Application class |
| `ui/navigation/NavRoutes.kt` | Change Detail id to Long |
| `ui/navigation/TrackittNavGraph.kt` | Pass Long id, create ViewModels |
| `ui/screen/home/HomeScreen.kt` | Wire to HomeViewModel, remove fake data |
| `ui/screen/add/AddScreen.kt` | Wire to AddViewModel |
| `ui/screen/detail/DetailScreen.kt` | Wire to DetailViewModel, real data |
| `ui/screen/search/SearchScreen.kt` | Wire to SearchViewModel, real data |
| `ui/screen/settings/SettingsScreen.kt` | Add backup/restore buttons |
| `ui/component/MonthChart.kt` | Month name from parameter, not hardcoded |

### Deleted Files

| File | Reason |
|---|---|
| `ui/screen/home/FakeHomeData.kt` | Replaced by real data from Room |
| All `.gitkeep` files in data/, service/, domain/usecase/ | Replaced by real files |

---

### Task 1: Room Entities and Database

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt`
- Create: `app/src/main/java/com/ayko/trackitt/data/local/entity/MerchantMappingEntity.kt`
- Create: `app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt`
- Create: `app/src/main/java/com/ayko/trackitt/data/local/dao/MerchantMappingDao.kt`
- Create: `app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/domain/model/Transaction.kt`

- [ ] **Step 1: Update the domain Transaction model**

The current model uses `String` id and `LocalDateTime`. Change to `Long` id and epoch millis for Room compatibility:

```kotlin
// app/src/main/java/com/ayko/trackitt/domain/model/Transaction.kt
package com.ayko.trackitt.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val fee: Double = 0.0,
    val isIncome: Boolean = false,
    val category: TransactionCategory,
    val merchant: String,
    val reference: String? = null,
    val transactionId: String = "",
    val source: String = "MTN MoMo",
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val isFromSms: Boolean = false,
    val rawSms: String? = null,
)
```

- [ ] **Step 2: Create TransactionEntity**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt
package com.ayko.trackitt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val fee: Double,
    val isIncome: Boolean,
    val category: String,
    val merchant: String,
    val reference: String?,
    val transactionId: String,
    val source: String,
    val note: String?,
    val date: Long,
    val isFromSms: Boolean,
    val rawSms: String?,
)
```

- [ ] **Step 3: Create MerchantMappingEntity**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/local/entity/MerchantMappingEntity.kt
package com.ayko.trackitt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey
    val merchant: String,
    val category: String,
)
```

- [ ] **Step 4: Create TransactionDao**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt
package com.ayko.trackitt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayko.trackitt.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val total: Double,
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year 
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month 
        ORDER BY date DESC
    """)
    fun getByMonth(year: String, month: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startOfDay AND date < :endOfDay
        ORDER BY date DESC
    """)
    fun getByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT category, SUM(amount) as total FROM transactions
        WHERE isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(year: String, month: String): Flow<List<CategoryTotal>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isIncome = 0
        AND date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalForRange(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE isIncome = 0
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getMonthTotal(year: String, month: String): Flow<Double>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchByMerchant(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): Flow<TransactionEntity?>

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
```

- [ ] **Step 5: Create MerchantMappingDao**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/local/dao/MerchantMappingDao.kt
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
```

- [ ] **Step 6: Create TrackittDatabase**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt
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
```

- [ ] **Step 7: Build to verify Room compiles**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -10`

Expected: BUILD SUCCESSFUL (KSP generates Room implementation)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/data/local/ app/src/main/java/com/ayko/trackitt/domain/model/Transaction.kt
git commit -m "add Room database with transaction and merchant mapping entities"
```

---

### Task 2: MoMo SMS Parser

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/sms/ParsedTransaction.kt`
- Create: `app/src/main/java/com/ayko/trackitt/service/sms/MoMoSmsParser.kt`
- Create: `app/src/test/java/com/ayko/trackitt/service/sms/MoMoSmsParserTest.kt`
- Modify: `app/build.gradle.kts` (add test dependencies)

- [ ] **Step 1: Add test dependencies to build.gradle.kts**

Add these lines inside the `dependencies` block in `app/build.gradle.kts`, after the existing WorkManager line:

```kotlin
    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

- [ ] **Step 2: Create ParsedTransaction data class**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/sms/ParsedTransaction.kt
package com.ayko.trackitt.service.sms

data class ParsedTransaction(
    val amount: Double,
    val fee: Double,
    val isIncome: Boolean,
    val merchant: String,
    val transactionId: String,
    val reference: String?,
    val date: Long,
    val rawSms: String,
)
```

- [ ] **Step 3: Write failing parser tests**

```kotlin
// app/src/test/java/com/ayko/trackitt/service/sms/MoMoSmsParserTest.kt
package com.ayko.trackitt.service.sms

import org.junit.Assert.*
import org.junit.Test

class MoMoSmsParserTest {

    private val parser = MoMoSmsParser()

    @Test
    fun `parse payment made P2P`() {
        val sms = "Payment made for GHS 355.00 to AKOSUA BOATENG Current Balance: GHS 13.06 . Available Balance: GHS 13.06. Reference: . Transaction ID: 70000000001. Fee charged: GHS2.66 Tax charged: 0. Download the MoMo App for a Faster & Easier Experience. Click here: https://mtnmymomo.onelink.me/XJOt/MoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(355.00, result!!.amount, 0.01)
        assertEquals(2.66, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("AKOSUA BOATENG", result.merchant)
        assertEquals("70000000001", result.transactionId)
    }

    @Test
    fun `parse payment received`() {
        val sms = "Payment received for GHS 365.00 from Express Pay ova  Current Balance: GHS 370.72 . Available Balance: GHS 370.72. Reference: expressPay. Transaction ID: 70000000013. TRANSACTION FEE: 0.00"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(365.00, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Express Pay ova", result.merchant)
        assertEquals("70000000013", result.transactionId)
        assertEquals("expressPay", result.reference)
    }

    @Test
    fun `parse merchant payment yello`() {
        val sms = "Y'ello. You have Paid GHS 85.47 to Merchant 032234 on your mobile money account at 200000000014. Message from sender: melcom. Your new balance: GHS 240.72 . Fee was GHS 0.85 . Financial Transaction Id: 70000000002.Download the MoMo App for a Faster & Easier Experience Click here: http://mtnghana.app.link/nsBnhItDoob"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(85.47, result!!.amount, 0.01)
        assertEquals(0.85, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("melcom", result.merchant)
        assertEquals("70000000002", result.transactionId)
    }

    @Test
    fun `parse merchant payment confirmation`() {
        val sms = "Your payment of GHS 85.47 to MELCOM COMMUNITY 25 has been completed at 2026-04-23 11:00:51. Reference: melcom. Your new balance: GHS 240.72. Fee was GHS 0.85 Tax charged: GHS0. Financial Transaction Id: 70000000002. External Transaction Id: -.Download the MoMo App for a Faster & Easier Experience Click here: https://bit.ly/downloadMyMoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(85.47, result!!.amount, 0.01)
        assertEquals("MELCOM COMMUNITY 25", result.merchant)
        assertEquals("70000000002", result.transactionId)
        assertEquals("melcom", result.reference)
    }

    @Test
    fun `parse cash out`() {
        val sms = "Cash Out made for GHS200.00 to ACME AGENT VENTURES. Current Balance: GHS38.72 Financial Transaction Id: 70000000005. Cash-out fee is charged automatically from your MTN MoMo wallet. Please do not pay any fees to the Agent. Thank you for using MTN MobileMoney. Fee charged: GHS2.00."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(200.00, result!!.amount, 0.01)
        assertEquals(2.00, result.fee, 0.01)
        assertFalse(result.isIncome)
        assertEquals("ACME AGENT VENTURES", result.merchant)
        assertEquals("70000000005", result.transactionId)
    }

    @Test
    fun `parse bank transfer received`() {
        val sms = "You have received GHS 400.00 from Ecobank ova on your mobile money account at 2026-04-23 11:30:29. Message from sender: . Your new balance:GHS 408.72. Financial Transaction Id: 70000000006."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(400.00, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Ecobank ova", result.merchant)
        assertEquals("70000000006", result.transactionId)
    }

    @Test
    fun `parse cash transfer received`() {
        val sms = "Cash transfer recieved: You have received 344.08 GHS in a cash transfer from 17000000003 17000000003 through agent Bigpay Ghana Limited - 200000000015 on your mobile money account at 2026-04-23 10:38:07. Message from sender: TransactionId - 2242353910809367129-n8NrjBgl. Your new balance: 367.04 GHS. Fee was 0.00 GHS. Financial Transaction Id: 70000000012."
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(344.08, result!!.amount, 0.01)
        assertTrue(result.isIncome)
        assertEquals("Bigpay Ghana Limited", result.merchant)
        assertEquals("70000000012", result.transactionId)
    }

    @Test
    fun `parse bill payment`() {
        val sms = "Payment for GHS4510.00 to Bills.INV ..Current Balance: GHS 22.96. Transaction Id: 70000000003. Fee charged: GHS0.00,Tax Charged 0.Download the MoMo App for a Faster & Easier Experience. Click here: https://bit.ly/downloadMyMoMo"
        val result = parser.parse(sms)
        assertNotNull(result)
        assertEquals(4510.00, result!!.amount, 0.01)
        assertFalse(result.isIncome)
        assertEquals("Bills.INV", result.merchant)
        assertEquals("70000000003", result.transactionId)
    }

    @Test
    fun `parse returns null for non-MoMo SMS`() {
        val sms = "Your OTP is 123456. Do not share this code."
        val result = parser.parse(sms)
        assertNull(result)
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew :app:testDebugUnitTest --tests "com.ayko.trackitt.service.sms.MoMoSmsParserTest" 2>&1 | tail -5`

Expected: FAIL — `MoMoSmsParser` class doesn't exist yet

- [ ] **Step 5: Implement MoMoSmsParser**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/sms/MoMoSmsParser.kt
package com.ayko.trackitt.service.sms

import java.text.SimpleDateFormat
import java.util.Locale

class MoMoSmsParser {

    fun parse(smsBody: String): ParsedTransaction? {
        val body = smsBody.trim()
        return when {
            body.startsWith("Payment made for") -> parsePaymentMade(body)
            body.startsWith("Payment received for") -> parsePaymentReceived(body)
            body.startsWith("Y'ello. You have Paid") -> parseMerchantYello(body)
            body.startsWith("Your payment of") -> parseMerchantConfirmation(body)
            body.startsWith("Cash Out made for") -> parseCashOut(body)
            body.startsWith("Payment for GHS") && body.contains("Bills.") -> parseBillPayment(body)
            body.startsWith("You have received") -> parseBankTransfer(body)
            body.startsWith("Cash transfer recieved:") -> parseCashTransfer(body)
            else -> null
        }
    }

    private fun parsePaymentMade(body: String): ParsedTransaction? {
        val amount = extractAmount("Payment made for", body) ?: return null
        val merchant = extractBetween(body, "to ", " Current Balance") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, reference, System.currentTimeMillis(), body)
    }

    private fun parsePaymentReceived(body: String): ParsedTransaction? {
        val amount = extractAmount("Payment received for", body) ?: return null
        val merchant = extractBetween(body, "from ", " Current Balance") ?: extractBetween(body, "from ", "  Current") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        return ParsedTransaction(amount, fee, true, merchant.trim(), txId, reference, System.currentTimeMillis(), body)
    }

    private fun parseMerchantYello(body: String): ParsedTransaction? {
        val amount = extractAmount("Y'ello. You have Paid", body) ?: return null
        val merchantMsg = extractBetween(body, "Message from sender: ", ".") ?: extractBetween(body, "Message from sender: ", ". Your") ?: ""
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val date = extractDate(body)
        return ParsedTransaction(amount, fee, false, merchantMsg.trim(), txId, null, date, body)
    }

    private fun parseMerchantConfirmation(body: String): ParsedTransaction? {
        val amount = extractAmount("Your payment of", body) ?: return null
        val merchant = extractBetween(body, "to ", " has been completed") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val reference = extractReference(body)
        val date = extractDate(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, reference, date, body)
    }

    private fun parseCashOut(body: String): ParsedTransaction? {
        val amount = extractAmount("Cash Out made for", body) ?: return null
        val merchant = extractBetween(body, "to ", ".") ?: extractBetween(body, "to ", ". Current") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, null, System.currentTimeMillis(), body)
    }

    private fun parseBillPayment(body: String): ParsedTransaction? {
        val amount = extractAmount("Payment for", body) ?: return null
        val merchant = extractBetween(body, "to ", " ..") ?: extractBetween(body, "to ", "..") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        return ParsedTransaction(amount, fee, false, merchant.trim(), txId, null, System.currentTimeMillis(), body)
    }

    private fun parseBankTransfer(body: String): ParsedTransaction? {
        val amount = extractAmount("You have received", body) ?: return null
        val merchant = extractBetween(body, "from ", " on your") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val date = extractDate(body)
        return ParsedTransaction(amount, 0.0, true, merchant.trim(), txId, null, date, body)
    }

    private fun parseCashTransfer(body: String): ParsedTransaction? {
        val amountMatch = Regex("""received\s+([\d,]+\.?\d*)\s*GHS""").find(body)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val agent = extractBetween(body, "through agent ", " - ") ?: extractBetween(body, "through agent ", " on your") ?: return null
        val txId = extractTransactionId(body) ?: return null
        val fee = extractFee(body)
        val date = extractDate(body)
        return ParsedTransaction(amount, fee, true, agent.trim(), txId, null, date, body)
    }

    private fun extractAmount(prefix: String, body: String): Double? {
        val patterns = listOf(
            Regex("""$prefix\s*GHS\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""$prefix\s*([\d,]+\.?\d*)\s*GHS""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractTransactionId(body: String): String? {
        val patterns = listOf(
            Regex("""Financial Transaction Id:\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Transaction ID:\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Transaction Id:\s*(\d+)""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractFee(body: String): Double {
        val patterns = listOf(
            Regex("""Fee charged:\s*GHS\s*([\d.]+)""", RegexOption.IGNORE_CASE),
            Regex("""Fee was\s*GHS\s*([\d.]+)""", RegexOption.IGNORE_CASE),
            Regex("""TRANSACTION FEE:\s*([\d.]+)""", RegexOption.IGNORE_CASE),
            Regex("""Fee was\s*([\d.]+)\s*GHS""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) return match.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractReference(body: String): String? {
        val match = Regex("""Reference:\s*([^.]+)""").find(body) ?: return null
        val ref = match.groupValues[1].trim()
        return ref.ifEmpty { null }
    }

    private fun extractDate(body: String): Long {
        val match = Regex("""(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})""").find(body)
        if (match != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                return sdf.parse(match.groupValues[1])?.time ?: System.currentTimeMillis()
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private fun extractBetween(body: String, start: String, end: String): String? {
        val startIdx = body.indexOf(start)
        if (startIdx == -1) return null
        val fromIdx = startIdx + start.length
        val endIdx = body.indexOf(end, fromIdx)
        if (endIdx == -1) return null
        return body.substring(fromIdx, endIdx)
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew :app:testDebugUnitTest --tests "com.ayko.trackitt.service.sms.MoMoSmsParserTest" 2>&1 | tail -10`

Expected: All 9 tests PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/sms/ParsedTransaction.kt app/src/main/java/com/ayko/trackitt/service/sms/MoMoSmsParser.kt app/src/test/java/com/ayko/trackitt/service/sms/MoMoSmsParserTest.kt app/build.gradle.kts
git commit -m "add MoMo SMS parser with tests for all 8 transaction types"
```

---

### Task 3: Categorization Engine

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/MerchantDictionary.kt`
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt`
- Create: `app/src/test/java/com/ayko/trackitt/service/categorization/CategorizationServiceTest.kt`

- [ ] **Step 1: Write failing categorization tests**

```kotlin
// app/src/test/java/com/ayko/trackitt/service/categorization/CategorizationServiceTest.kt
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationServiceTest {

    private val service = DictionaryCategorizationService(userLookup = { null })

    @Test
    fun `melcom maps to SHOPPING`() {
        assertEquals(TransactionCategory.SHOPPING, service.categorize("MELCOM COMMUNITY 25"))
    }

    @Test
    fun `bolt maps to TRANSPORT`() {
        assertEquals(TransactionCategory.TRANSPORT, service.categorize("Bolt ride"))
    }

    @Test
    fun `ecg maps to BILLS`() {
        assertEquals(TransactionCategory.BILLS, service.categorize("ECG Prepaid"))
    }

    @Test
    fun `papaye maps to FOOD`() {
        assertEquals(TransactionCategory.FOOD, service.categorize("Papaye Fast Food"))
    }

    @Test
    fun `shoprite maps to GROCERIES`() {
        assertEquals(TransactionCategory.GROCERIES, service.categorize("Shoprite Accra Mall"))
    }

    @Test
    fun `unknown merchant maps to OTHER`() {
        assertEquals(TransactionCategory.OTHER, service.categorize("AKOSUA BOATENG"))
    }

    @Test
    fun `user override takes priority`() {
        val serviceWithOverride = DictionaryCategorizationService(
            userLookup = { merchant ->
                if (merchant == "akosua boateng") TransactionCategory.FOOD else null
            }
        )
        assertEquals(TransactionCategory.FOOD, serviceWithOverride.categorize("AKOSUA BOATENG"))
    }

    @Test
    fun `expressPay maps to INCOME`() {
        assertEquals(TransactionCategory.INCOME, service.categorize("Express Pay ova"))
    }

    @Test
    fun `bills inv maps to BILLS`() {
        assertEquals(TransactionCategory.BILLS, service.categorize("Bills.INV"))
    }

    @Test
    fun `case insensitive matching`() {
        assertEquals(TransactionCategory.SHOPPING, service.categorize("melcom"))
        assertEquals(TransactionCategory.SHOPPING, service.categorize("MELCOM"))
        assertEquals(TransactionCategory.SHOPPING, service.categorize("Melcom"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew :app:testDebugUnitTest --tests "com.ayko.trackitt.service.categorization.CategorizationServiceTest" 2>&1 | tail -5`

Expected: FAIL — classes don't exist

- [ ] **Step 3: Create MerchantDictionary**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/categorization/MerchantDictionary.kt
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

val MERCHANT_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    // Shopping
    "melcom" to TransactionCategory.SHOPPING,
    "game" to TransactionCategory.SHOPPING,
    "palace" to TransactionCategory.SHOPPING,
    "koala" to TransactionCategory.SHOPPING,

    // Groceries
    "shoprite" to TransactionCategory.GROCERIES,
    "maxmart" to TransactionCategory.GROCERIES,
    "marina mall" to TransactionCategory.GROCERIES,
    "market" to TransactionCategory.GROCERIES,

    // Food
    "papaye" to TransactionCategory.FOOD,
    "kfc" to TransactionCategory.FOOD,
    "chicken inn" to TransactionCategory.FOOD,
    "pizza inn" to TransactionCategory.FOOD,
    "marwako" to TransactionCategory.FOOD,
    "burger king" to TransactionCategory.FOOD,
    "frankies" to TransactionCategory.FOOD,
    "food" to TransactionCategory.FOOD,

    // Transport
    "bolt" to TransactionCategory.TRANSPORT,
    "uber" to TransactionCategory.TRANSPORT,
    "yango" to TransactionCategory.TRANSPORT,
    "trotro" to TransactionCategory.TRANSPORT,
    "taxi" to TransactionCategory.TRANSPORT,

    // Bills
    "ecg" to TransactionCategory.BILLS,
    "gwcl" to TransactionCategory.BILLS,
    "dstv" to TransactionCategory.BILLS,
    "gotv" to TransactionCategory.BILLS,
    "vodafone" to TransactionCategory.BILLS,
    "airtel" to TransactionCategory.BILLS,
    "bills.inv" to TransactionCategory.BILLS,
    "surfline" to TransactionCategory.BILLS,

    // Income / banks
    "expresspay" to TransactionCategory.INCOME,
    "ecobank" to TransactionCategory.INCOME,
    "gcb" to TransactionCategory.INCOME,
    "stanbic" to TransactionCategory.INCOME,
    "fidelity" to TransactionCategory.INCOME,
    "calbank" to TransactionCategory.INCOME,
    "absa" to TransactionCategory.INCOME,
    "uba" to TransactionCategory.INCOME,

    // Transfer
    "bigpay" to TransactionCategory.TRANSFER,
)
```

- [ ] **Step 4: Create CategorizationService**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    fun categorize(merchant: String): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override fun categorize(merchant: String): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        // User override — run blocking since this is called during insert
        val userMapping = kotlinx.coroutines.runBlocking { userLookup(normalized) }
        if (userMapping != null) return userMapping

        // Built-in dictionary — substring match
        for ((keyword, category) in MERCHANT_DICTIONARY) {
            if (normalized.contains(keyword)) return category
        }

        return TransactionCategory.OTHER
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew :app:testDebugUnitTest --tests "com.ayko.trackitt.service.categorization.CategorizationServiceTest" 2>&1 | tail -10`

Expected: All 10 tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/ app/src/test/java/com/ayko/trackitt/service/categorization/
git commit -m "add categorization engine with merchant dictionary and user override support"
```

---

### Task 4: Repository + AppContainer

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt`
- Create: `app/src/main/java/com/ayko/trackitt/AppContainer.kt`
- Create: `app/src/main/java/com/ayko/trackitt/TrackittApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create TransactionRepository**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt
package com.ayko.trackitt.data.repository

import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CategorizationService
import com.ayko.trackitt.service.sms.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val categorizationService: CategorizationService,
) {

    fun getByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getByMonth(y, m).map { list -> list.map { it.toDomain() } }
    }

    fun getToday(): Flow<List<Transaction>> {
        val (start, end) = todayRange()
        return transactionDao.getByDateRange(start, end).map { list -> list.map { it.toDomain() } }
    }

    fun getCategoryTotals(year: Int, month: Int): Flow<List<CategoryTotal>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getCategoryTotals(y, m)
    }

    fun getTodayTotal(): Flow<Double> {
        val (start, end) = todayRange()
        return transactionDao.getTotalForRange(start, end)
    }

    fun getMonthTotal(year: Int, month: Int): Flow<Double> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getMonthTotal(y, m)
    }

    fun searchByMerchant(query: String): Flow<List<Transaction>> {
        return transactionDao.searchByMerchant(query).map { list -> list.map { it.toDomain() } }
    }

    fun getById(id: Long): Flow<Transaction?> {
        return transactionDao.getById(id).map { it?.toDomain() }
    }

    suspend fun insertFromSms(parsed: ParsedTransaction) {
        val category = categorizationService.categorize(parsed.merchant)
        val entity = TransactionEntity(
            amount = parsed.amount,
            fee = parsed.fee,
            isIncome = parsed.isIncome,
            category = category.name,
            merchant = parsed.merchant,
            reference = parsed.reference,
            transactionId = parsed.transactionId,
            source = "MTN MoMo",
            note = null,
            date = parsed.date,
            isFromSms = true,
            rawSms = parsed.rawSms,
        )
        transactionDao.insert(entity)
    }

    suspend fun insertManual(transaction: Transaction) {
        val entity = TransactionEntity(
            amount = transaction.amount,
            fee = 0.0,
            isIncome = transaction.isIncome,
            category = transaction.category.name,
            merchant = transaction.merchant,
            reference = null,
            transactionId = "manual_${System.currentTimeMillis()}",
            source = transaction.source,
            note = transaction.note,
            date = transaction.date,
            isFromSms = false,
            rawSms = null,
        )
        transactionDao.insert(entity)
    }

    suspend fun updateCategory(id: Long, category: TransactionCategory, merchant: String) {
        transactionDao.updateCategory(id, category.name)
        merchantMappingDao.upsert(
            MerchantMappingEntity(
                merchant = merchant.lowercase().trim(),
                category = category.name,
            )
        )
    }

    suspend fun delete(id: Long) {
        transactionDao.delete(id)
    }

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            fee = fee,
            isIncome = isIncome,
            category = try { TransactionCategory.valueOf(category) } catch (_: Exception) { TransactionCategory.OTHER },
            merchant = merchant,
            reference = reference,
            transactionId = transactionId,
            source = source,
            note = note,
            date = date,
            isFromSms = isFromSms,
            rawSms = rawSms,
        )
    }
}
```

- [ ] **Step 2: Create AppContainer**

```kotlin
// app/src/main/java/com/ayko/trackitt/AppContainer.kt
package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.service.categorization.CategorizationService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.sms.MoMoSmsParser

class AppContainer(context: Context) {
    val database = TrackittDatabase.getInstance(context)

    val categorizationService: CategorizationService = DictionaryCategorizationService(
        userLookup = { merchant ->
            val mapping = database.merchantMappingDao().getMapping(merchant)
            mapping?.let {
                try { com.ayko.trackitt.domain.model.TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
            }
        }
    )

    val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        merchantMappingDao = database.merchantMappingDao(),
        categorizationService = categorizationService,
    )

    val smsParser = MoMoSmsParser()
}
```

- [ ] **Step 3: Create TrackittApplication**

```kotlin
// app/src/main/java/com/ayko/trackitt/TrackittApplication.kt
package com.ayko.trackitt

import android.app.Application

class TrackittApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 4: Register Application class in AndroidManifest**

In `app/src/main/AndroidManifest.xml`, add `android:name=".TrackittApplication"` to the `<application>` tag:

Change:
```xml
    <application
        android:allowBackup="true"
```
To:
```xml
    <application
        android:name=".TrackittApplication"
        android:allowBackup="true"
```

- [ ] **Step 5: Build to verify**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/AppContainer.kt app/src/main/java/com/ayko/trackitt/TrackittApplication.kt app/src/main/java/com/ayko/trackitt/data/repository/ app/src/main/AndroidManifest.xml
git commit -m "add repository, AppContainer, and Application class for manual DI"
```

---

### Task 5: ViewModels

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/ayko/trackitt/ui/screen/add/AddViewModel.kt`
- Create: `app/src/main/java/com/ayko/trackitt/ui/screen/detail/DetailViewModel.kt`
- Create: `app/src/main/java/com/ayko/trackitt/ui/screen/search/SearchViewModel.kt`

- [ ] **Step 1: Create HomeViewModel**

```kotlin
// app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt
package com.ayko.trackitt.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.ui.component.ChartCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class Period { TODAY, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod

    private val now = LocalDate.now()
    val currentMonthName: String = now.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
    val currentYear: Int = now.year

    val total: Flow<Double> = _selectedPeriod.flatMapLatest { period ->
        when (period) {
            Period.TODAY -> repository.getTodayTotal()
            Period.MONTH -> repository.getMonthTotal(now.year, now.monthValue)
        }
    }

    val chartCategories: Flow<List<ChartCategory>> =
        repository.getCategoryTotals(now.year, now.monthValue).map { totals ->
            totals.mapNotNull { ct ->
                val cat = try { TransactionCategory.valueOf(ct.category) } catch (_: Exception) { null }
                cat?.let { ChartCategory(it, ct.total) }
            }
        }

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
    }
}
```

- [ ] **Step 2: Create AddViewModel**

```kotlin
// app/src/main/java/com/ayko/trackitt/ui/screen/add/AddViewModel.kt
package com.ayko.trackitt.ui.screen.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    val amount = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<TransactionCategory?>(null)
    val note = MutableStateFlow("")
    val source = MutableStateFlow("MTN MoMo")

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun save() {
        val amountValue = amount.value.toDoubleOrNull() ?: return
        val category = selectedCategory.value ?: return
        if (amountValue <= 0) return

        viewModelScope.launch {
            repository.insertManual(
                Transaction(
                    amount = amountValue,
                    category = category,
                    merchant = note.value.ifEmpty { "Manual entry" },
                    source = source.value,
                    note = note.value.ifEmpty { null },
                    date = System.currentTimeMillis(),
                )
            )
            _saved.value = true
        }
    }
}
```

- [ ] **Step 3: Create DetailViewModel**

```kotlin
// app/src/main/java/com/ayko/trackitt/ui/screen/detail/DetailViewModel.kt
package com.ayko.trackitt.ui.screen.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    application: Application,
    private val transactionId: Long,
) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository

    val transaction: Flow<Transaction?> = repository.getById(transactionId)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun updateCategory(category: TransactionCategory, merchant: String) {
        viewModelScope.launch {
            repository.updateCategory(transactionId, category, merchant)
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(transactionId)
            _deleted.value = true
        }
    }
}
```

- [ ] **Step 4: Create SearchViewModel**

```kotlin
// app/src/main/java/com/ayko/trackitt/ui/screen/search/SearchViewModel.kt
package com.ayko.trackitt.ui.screen.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.ayko.trackitt.TrackittApplication
import com.ayko.trackitt.domain.model.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository
    private val prefs = application.getSharedPreferences("search", Context.MODE_PRIVATE)

    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: Flow<List<Transaction>> = query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.length < 2) flowOf(emptyList())
            else repository.searchByMerchant(q)
        }

    val recentSearches: Flow<List<String>> = MutableStateFlow(loadRecent())

    fun saveSearch(term: String) {
        if (term.isBlank()) return
        val recent = loadRecent().toMutableList()
        recent.remove(term)
        recent.add(0, term)
        val trimmed = recent.take(5)
        prefs.edit().putStringSet("recent", trimmed.toSet()).apply()
        (recentSearches as MutableStateFlow).value = trimmed
    }

    private fun loadRecent(): List<String> {
        return prefs.getStringSet("recent", emptySet())?.toList() ?: emptyList()
    }
}
```

- [ ] **Step 5: Build to verify**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt app/src/main/java/com/ayko/trackitt/ui/screen/add/AddViewModel.kt app/src/main/java/com/ayko/trackitt/ui/screen/detail/DetailViewModel.kt app/src/main/java/com/ayko/trackitt/ui/screen/search/SearchViewModel.kt
git commit -m "add ViewModels for all screens backed by repository"
```

---

### Task 6: Wire UI Screens to ViewModels

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/add/AddScreen.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/detail/DetailScreen.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/navigation/TrackittNavGraph.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/component/MonthChart.kt`
- Delete: `app/src/main/java/com/ayko/trackitt/ui/screen/home/FakeHomeData.kt`

This is the largest task — all hardcoded data gets replaced. Follow these sub-steps:

- [ ] **Step 1: Update NavRoutes to use Long id**

```kotlin
// app/src/main/java/com/ayko/trackitt/ui/navigation/NavRoutes.kt
package com.ayko.trackitt.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object Add : NavRoute("add")
    data object Settings : NavRoute("settings")
    data object Search : NavRoute("search")
    data object Detail : NavRoute("detail/{transactionId}") {
        fun withId(id: Long) = "detail/$id"
    }
}
```

- [ ] **Step 2: Update MonthChart to accept month name as parameter**

In `MonthChart.kt`, change the `ChartHeader` to accept a `monthLabel` parameter instead of hardcoded "APRIL":

Add `monthLabel: String = "APRIL"` parameter to the `MonthChart` composable signature, pass it to `ChartHeader`, and replace the hardcoded `"TRACKITT · APRIL"` with `"TRACKITT · ${monthLabel.uppercase()}"`.

- [ ] **Step 3: Wire HomeScreen to HomeViewModel**

Replace all references to `fakeTransactions`, `fakeChartCategories`, `fakeTotalSpent` with ViewModel flows. Use `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose`. Add this dependency to build.gradle.kts:

```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```

The HomeScreen composable should:
- Accept a `viewModel: HomeViewModel` parameter (default created via `viewModel()`)
- Collect `total`, `chartCategories`, `selectedPeriod` as state
- Derive `empty` from `chartCategories.isEmpty()`
- Pass `currentMonthName` to MonthChart's `monthLabel`
- Format total for the chart pill (e.g. "₵2.3k")
- Switch period chips between TODAY and MONTH

- [ ] **Step 4: Wire AddScreen to AddViewModel**

The AddScreen composable should:
- Accept a `viewModel: AddViewModel` parameter
- Bind `amount` state to the DisplayNumber
- Bind `selectedCategory` to the category grid
- Call `viewModel.save()` on save button tap
- Observe `saved` flow and call `onSave()` callback when true

- [ ] **Step 5: Wire DetailScreen to DetailViewModel**

The DetailScreen composable should:
- Accept `transactionId: Long` and create `DetailViewModel` with it
- Collect `transaction` flow as state
- Show loading state if null
- Display real data: amount, merchant, category, reference, date, source
- Wire category card tap to update category
- Wire delete button to `viewModel.delete()`
- Observe `deleted` and call `onBack()` when true

- [ ] **Step 6: Wire SearchScreen to SearchViewModel**

The SearchScreen composable should:
- Accept a `viewModel: SearchViewModel` parameter
- Bind `query` to the search text field
- Collect `results` and `recentSearches` as state
- Replace hardcoded results with real Flow data
- Call `viewModel.saveSearch()` when user submits a search

- [ ] **Step 7: Update TrackittNavGraph**

Wire ViewModels into the nav graph. For DetailScreen, extract the `transactionId` argument from the backstack entry:

```kotlin
composable(
    NavRoute.Detail.route,
    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
) { backStackEntry ->
    val txId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
    DetailScreen(
        transactionId = txId,
        onBack = { navController.popBackStack() },
        onConfirm = { navController.popBackStack() },
    )
}
```

- [ ] **Step 8: Delete FakeHomeData.kt**

```bash
rm app/src/main/java/com/ayko/trackitt/ui/screen/home/FakeHomeData.kt
```

- [ ] **Step 9: Build and verify**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -10`

Expected: BUILD SUCCESSFUL with no references to fake data

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "wire all screens to ViewModels, remove hardcoded data"
```

---

### Task 7: SMS BroadcastReceiver + Backfill Worker

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/sms/SmsBroadcastReceiver.kt`
- Create: `app/src/main/java/com/ayko/trackitt/service/sms/SmsBackfillWorker.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create SmsBroadcastReceiver**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/sms/SmsBroadcastReceiver.kt
package com.ayko.trackitt.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ayko.trackitt.TrackittApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages
            .groupBy { it.originatingAddress }
            .map { (_, parts) -> parts.joinToString("") { it.messageBody ?: "" } }

        val app = context.applicationContext as? TrackittApplication ?: return
        val parser = app.container.smsParser
        val repository = app.container.repository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fullBody.forEach { body ->
                    val parsed = parser.parse(body)
                    if (parsed != null) {
                        repository.insertFromSms(parsed)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 2: Create SmsBackfillWorker**

```kotlin
// app/src/main/java/com/ayko/trackitt/service/sms/SmsBackfillWorker.kt
package com.ayko.trackitt.service.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ayko.trackitt.TrackittApplication

class SmsBackfillWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackittApplication ?: return Result.failure()
        val parser = app.container.smsParser
        val repository = app.container.repository

        val prefs = applicationContext.getSharedPreferences("trackitt", Context.MODE_PRIVATE)
        if (prefs.getBoolean("backfill_completed", false)) return Result.success()

        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)

        val cursor = applicationContext.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("body", "date"),
            "date > ?",
            arrayOf(ninetyDaysAgo.toString()),
            "date ASC"
        ) ?: return Result.failure()

        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val parsed = parser.parse(body) ?: continue
                repository.insertFromSms(parsed)
            }
        }

        prefs.edit().putBoolean("backfill_completed", true).apply()
        return Result.success()
    }
}
```

- [ ] **Step 3: Register receiver in AndroidManifest**

Add inside the `<application>` tag in `AndroidManifest.xml`:

```xml
        <receiver
            android:name=".service.sms.SmsBroadcastReceiver"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 4: Build to verify**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/sms/SmsBroadcastReceiver.kt app/src/main/java/com/ayko/trackitt/service/sms/SmsBackfillWorker.kt app/src/main/AndroidManifest.xml
git commit -m "add SMS BroadcastReceiver and backfill worker for MoMo messages"
```

---

### Task 8: First Launch Permission Flow

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt`

- [ ] **Step 1: Add SMS permission state to HomeViewModel**

Add to `HomeViewModel`:

```kotlin
val smsPermissionGranted = MutableStateFlow(false)

fun onSmsPermissionResult(granted: Boolean, context: Context) {
    smsPermissionGranted.value = granted
    if (granted) {
        val request = OneTimeWorkRequestBuilder<SmsBackfillWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_backfill",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
```

Add these imports:
```kotlin
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ayko.trackitt.service.sms.SmsBackfillWorker
```

- [ ] **Step 2: Add permission request to HomeScreen**

In `HomeScreen`, add an SMS permission prompt card that shows when the permission isn't granted. Use `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission`. When the user grants permission, call `viewModel.onSmsPermissionResult(true, context)`.

The card should say: "Allow SMS access to automatically track your MoMo transactions" with a "Grant access" button.

Check permission status on composition using `ContextCompat.checkSelfPermission`.

- [ ] **Step 3: Build and test on device**

Run the app, verify:
1. Empty state shows on first launch
2. Permission card appears
3. Granting permission triggers backfill
4. Transactions populate from SMS history

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/ui/screen/home/
git commit -m "add first-launch SMS permission flow with backfill trigger"
```

---

### Task 9: Google Drive Backup

**Files:**
- Modify: `app/build.gradle.kts` (add Google Drive/Auth dependencies)
- Modify: `gradle/libs.versions.toml` (add versions)
- Create: `app/src/main/java/com/ayko/trackitt/data/backup/DriveBackupManager.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/settings/SettingsScreen.kt`

- [ ] **Step 1: Add Google Drive dependencies**

Add to `gradle/libs.versions.toml` under `[versions]`:
```toml
playServicesAuth = "21.3.0"
googleApiClient = "2.7.0"
googleDriveApi = "v3-rev20241206-2.0.0"
```

Add to `[libraries]`:
```toml
google-play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version.ref = "playServicesAuth" }
google-api-client-android = { group = "com.google.api-client", name = "google-api-client-android", version.ref = "googleApiClient" }
google-api-services-drive = { group = "com.google.apis", name = "google-api-services-drive", version.ref = "googleDriveApi" }
```

Add to `app/build.gradle.kts` dependencies:
```kotlin
    // Google Drive backup
    implementation(libs.google.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
```

- [ ] **Step 2: Create DriveBackupManager**

```kotlin
// app/src/main/java/com/ayko/trackitt/data/backup/DriveBackupManager.kt
package com.ayko.trackitt.data.backup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.ayko.trackitt.data.local.TrackittDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriveBackupManager(private val context: Context) {

    private fun getDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccountName = accountName }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Trackitt").build()
    }

    suspend fun backup(accountName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("trackitt.db")
            if (!dbFile.exists()) return@withContext false

            // Close database checkpoint before copying
            TrackittDatabase.getInstance(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)").close()

            val timestamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
            val fileName = "trackitt-backup-$timestamp.db"

            val drive = getDriveService(accountName)
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }
            val mediaContent = FileContent("application/octet-stream", dbFile)
            drive.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listBackups(accountName: String): List<BackupFile> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(accountName)
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, size, createdTime)")
                .setOrderBy("createdTime desc")
                .execute()
            result.files?.map { file ->
                BackupFile(file.id, file.name, file.getSize()?.toLong() ?: 0, file.createdTime?.value ?: 0)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun restore(accountName: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(accountName)
            val dbFile = context.getDatabasePath("trackitt.db")

            // Close current database
            TrackittDatabase.getInstance(context).close()

            // Download backup
            val tempFile = File(context.cacheDir, "restore.db")
            FileOutputStream(tempFile).use { out ->
                drive.files().get(fileId).executeMediaAndDownloadTo(out)
            }

            // Replace database file
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            // Delete WAL/SHM files
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            true
        } catch (_: Exception) {
            false
        }
    }
}

data class BackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: Long,
)
```

- [ ] **Step 3: Update SettingsScreen with backup/restore buttons**

Add "Back up now" and "Restore from backup" buttons to `SettingsScreen`. On tap, launch Google Sign-In using `ActivityResultContracts.StartActivityForResult` with `GoogleSignIn.getSignInIntent()`. On successful sign-in, call `DriveBackupManager.backup()` or show a backup list for restore.

Show a `Snackbar` on success/failure.

- [ ] **Step 4: Build to verify**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -10`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "add Google Drive manual backup and restore"
```

---

### Task 10: Cleanup

**Files:**
- Delete: all `.gitkeep` files in data/, service/, domain/usecase/, ui/ directories
- Delete: `app/src/main/res/values/font_certs.xml` (Google Fonts certs no longer needed)

- [ ] **Step 1: Remove .gitkeep files and unused resources**

```bash
find app/src/main/java/com/ayko/trackitt -name ".gitkeep" -delete
rm -f app/src/main/res/values/font_certs.xml
```

- [ ] **Step 2: Final build**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export ANDROID_HOME=~/Library/Android/sdk && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests**

Run: `cd "/Users/zap/Code/Other Personal Repos/trackitt" && export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./gradlew :app:testDebugUnitTest 2>&1 | tail -10`

Expected: All tests PASS

- [ ] **Step 4: Commit and push**

```bash
git add -A
git commit -m "cleanup: remove .gitkeep placeholders and unused font certs"
git push origin main
```
