# LLM-Powered Transaction Categorization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add LLM-based transaction categorization as a fallback behind the existing dictionary, preferring on-device Gemini Nano with cloud Gemini Flash-Lite as fallback.

**Architecture:** The existing `CategorizationService` interface gains a `note` parameter and becomes `suspend`. A new `SmartCategorizationService` orchestrates the chain: dictionary → Gemini Nano (on-device) → Gemini Flash-Lite (cloud). Transactions that can't reach any LLM get flagged for retry on app launch or connectivity restore.

**Tech Stack:** ML Kit GenAI Prompt API (Gemini Nano), Firebase AI Logic (Gemini Flash-Lite), Room (migration 3→4), ConnectivityManager

**Spec:** `docs/superpowers/specs/2026-04-27-llm-categorization-design.md`

---

## File Map

**New files:**
- `app/src/main/java/com/ayko/trackitt/service/categorization/ClassificationResult.kt` — sealed class for LLM results
- `app/src/main/java/com/ayko/trackitt/service/categorization/NanoCategorizationService.kt` — ML Kit Prompt API wrapper
- `app/src/main/java/com/ayko/trackitt/service/categorization/CloudCategorizationService.kt` — Firebase AI Logic wrapper
- `app/src/main/java/com/ayko/trackitt/service/categorization/SmartCategorizationService.kt` — orchestrates Dictionary → Nano → Cloud
- `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationPrompt.kt` — shared prompt builder
- `app/src/main/java/com/ayko/trackitt/service/network/NetworkMonitor.kt` — connectivity observer

**Modified files:**
- `gradle/libs.versions.toml` — add ML Kit + Firebase versions
- `build.gradle.kts` (project-level) — add google-services plugin
- `app/build.gradle.kts` — add dependencies + apply google-services plugin
- `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt` — add `note` param, make `suspend`
- `app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt` — add `needsClassification` column
- `app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt` — add pending classification queries
- `app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt` — migration 3→4, bump version
- `app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt` — use SmartCategorizationService, handle needsClassification, add retry
- `app/src/main/java/com/ayko/trackitt/AppContainer.kt` — wire up new services + NetworkMonitor
- `app/src/main/java/com/ayko/trackitt/TrackittApplication.kt` — expose NetworkMonitor
- `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt` — trigger retries on launch + network restore
- `app/src/main/java/com/ayko/trackitt/ui/screen/settings/SettingsScreen.kt` — add "Use on-device AI" toggle

---

### Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (project-level)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add versions and libraries to version catalog**

In `gradle/libs.versions.toml`, add the following entries:

```toml
# Under [versions], add:
firebaseBom = "33.7.0"
mlkitGenai = "1.0.0-beta2"
googleServices = "4.4.2"

# Under [libraries], add:
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenai" }

# Under [plugins], add:
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

- [ ] **Step 2: Add google-services plugin to project-level build.gradle.kts**

In `build.gradle.kts` (project root), add to the plugins block:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
```

- [ ] **Step 3: Apply plugin and add dependencies in app/build.gradle.kts**

In `app/build.gradle.kts`, add the plugin:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}
```

And add dependencies in the dependencies block, after the Google Drive backup section:

```kotlin
// ML Kit GenAI (on-device Gemini Nano)
implementation(libs.mlkit.genai.prompt)

// Firebase AI Logic (cloud Gemini Flash-Lite)
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.ai)
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`

Note: This will fail until `google-services.json` is placed in `app/`. If building without Firebase setup, temporarily comment out the `google-services` plugin line. The user will set up Firebase separately.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "add ML Kit GenAI and Firebase AI Logic dependencies"
```

---

### Task 2: Database Migration — Add needsClassification Column

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt`

- [ ] **Step 1: Add needsClassification field to TransactionEntity**

In `app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt`, add the new field at the end of the data class:

```kotlin
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
    val needsClassification: Boolean = false,
)
```

- [ ] **Step 2: Add DAO queries for pending classifications**

In `app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt`, add these queries after the `delete` function:

```kotlin
@Query("SELECT * FROM transactions WHERE needsClassification = 1")
suspend fun getPendingClassification(): List<TransactionEntity>

@Query("UPDATE transactions SET needsClassification = :needs WHERE id = :id")
suspend fun updateNeedsClassification(id: Long, needs: Boolean)
```

- [ ] **Step 3: Add migration and bump database version**

In `app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt`, replace the entire file contents:

```kotlin
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
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/data/local/entity/TransactionEntity.kt \
       app/src/main/java/com/ayko/trackitt/data/local/dao/TransactionDao.kt \
       app/src/main/java/com/ayko/trackitt/data/local/TrackittDatabase.kt
git commit -m "add needsClassification column with migration 3 to 4"
```

---

### Task 3: Update CategorizationService Interface

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt`

- [ ] **Step 1: Update the interface and DictionaryCategorizationService**

Replace the entire contents of `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

interface CategorizationService {
    suspend fun categorize(
        merchant: String,
        reference: String? = null,
        note: String? = null,
    ): TransactionCategory
}

class DictionaryCategorizationService(
    private val userLookup: suspend (String) -> TransactionCategory?,
) : CategorizationService {

    override suspend fun categorize(
        merchant: String,
        reference: String?,
        note: String?,
    ): TransactionCategory {
        val normalized = merchant.lowercase().trim()

        val userMapping = userLookup(normalized)
        if (userMapping != null) return userMapping

        if (note != null) {
            val normalizedNote = note.lowercase().trim()
            for ((keyword, category) in REFERENCE_DICTIONARY) {
                if (normalizedNote.contains(keyword)) return category
            }
        }

        if (reference != null) {
            val normalizedRef = reference.lowercase().trim()
            for ((keyword, category) in REFERENCE_DICTIONARY) {
                if (normalizedRef.contains(keyword)) return category
            }
        }

        for ((keyword, category) in MERCHANT_DICTIONARY) {
            if (normalized.contains(keyword)) return category
        }

        return TransactionCategory.OTHER
    }
}
```

Key changes: interface now has `note` parameter and is `suspend`. Dictionary checks note against the reference dictionary too (before checking reference itself). Removed `runBlocking` — `userLookup` is called directly since the function is now `suspend`.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationService.kt
git commit -m "update CategorizationService interface with note param and suspend"
```

---

### Task 4: Create ClassificationResult and CategorizationPrompt

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/ClassificationResult.kt`
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationPrompt.kt`

- [ ] **Step 1: Create ClassificationResult sealed class**

Create `app/src/main/java/com/ayko/trackitt/service/categorization/ClassificationResult.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

sealed class ClassificationResult {
    data class Classified(val category: TransactionCategory) : ClassificationResult()
    data object Unavailable : ClassificationResult()
}
```

- [ ] **Step 2: Create CategorizationPrompt**

Create `app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationPrompt.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

object CategorizationPrompt {

    private val CATEGORY_NAMES = TransactionCategory.entries
        .filter { it != TransactionCategory.INCOME }
        .joinToString(", ") { it.name }

    fun build(merchant: String, reference: String?, note: String?): String {
        return buildString {
            appendLine("Classify this transaction into exactly one category.")
            appendLine()
            appendLine("Categories: $CATEGORY_NAMES")
            appendLine()
            appendLine("Transaction:")
            appendLine("- Merchant: $merchant")
            appendLine("- Note: ${note ?: "none"}")
            appendLine("- Reference: ${reference ?: "none"}")
            appendLine()
            appendLine("Respond with only the category name, nothing else.")
        }
    }

    fun parseResponse(response: String): TransactionCategory? {
        return try {
            val cleaned = response.trim().uppercase()
            TransactionCategory.valueOf(cleaned)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/ClassificationResult.kt \
       app/src/main/java/com/ayko/trackitt/service/categorization/CategorizationPrompt.kt
git commit -m "add ClassificationResult sealed class and shared prompt builder"
```

---

### Task 5: Create NanoCategorizationService

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/NanoCategorizationService.kt`

- [ ] **Step 1: Create NanoCategorizationService**

Create `app/src/main/java/com/ayko/trackitt/service/categorization/NanoCategorizationService.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import android.util.Log
import com.google.mlkit.genai.prompt.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

class NanoCategorizationService {

    private var model: GenerativeModel? = null

    suspend fun isAvailable(): Boolean {
        return try {
            val m = Generation.getClient()
            val status = m.checkStatus()
            if (status == FeatureStatus.AVAILABLE) {
                model = m
                true
            } else {
                m.close()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano availability check failed", e)
            false
        }
    }

    suspend fun classify(
        merchant: String,
        reference: String?,
        note: String?,
    ): ClassificationResult {
        val m = model ?: return ClassificationResult.Unavailable
        return try {
            val prompt = CategorizationPrompt.build(merchant, reference, note)
            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.1f
                topK = 1
                maxOutputTokens = 10
            }
            val response = m.generateContent(request)
            val text = response.candidates.firstOrNull()?.text
            if (text == null) {
                Log.w(TAG, "Nano returned no candidates")
                return ClassificationResult.Unavailable
            }
            val category = CategorizationPrompt.parseResponse(text)
            if (category != null) {
                ClassificationResult.Classified(category)
            } else {
                Log.w(TAG, "Nano returned unparseable: $text")
                ClassificationResult.Unavailable
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nano classification failed", e)
            ClassificationResult.Unavailable
        }
    }

    fun close() {
        model?.close()
        model = null
    }

    companion object {
        private const val TAG = "NanoCategorization"
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (may have warnings about ML Kit beta)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/NanoCategorizationService.kt
git commit -m "add NanoCategorizationService for on-device Gemini Nano"
```

---

### Task 6: Create CloudCategorizationService

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/CloudCategorizationService.kt`

- [ ] **Step 1: Create CloudCategorizationService**

Create `app/src/main/java/com/ayko/trackitt/service/categorization/CloudCategorizationService.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class CloudCategorizationService {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash-lite")
    }

    suspend fun classify(
        merchant: String,
        reference: String?,
        note: String?,
    ): ClassificationResult {
        return try {
            val prompt = CategorizationPrompt.build(merchant, reference, note)
            val response = model.generateContent(prompt)
            val text = response.text
            if (text == null) {
                Log.w(TAG, "Cloud returned no text")
                return ClassificationResult.Unavailable
            }
            val category = CategorizationPrompt.parseResponse(text)
            if (category != null) {
                ClassificationResult.Classified(category)
            } else {
                Log.w(TAG, "Cloud returned unparseable: $text")
                ClassificationResult.Classified(com.ayko.trackitt.domain.model.TransactionCategory.OTHER)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud classification failed", e)
            ClassificationResult.Unavailable
        }
    }

    companion object {
        private const val TAG = "CloudCategorization"
    }
}
```

Note: If the cloud LLM returns something unparseable, we treat it as a genuine OTHER classification (not Unavailable), because the model did respond — it just didn't map cleanly. Only network/auth failures count as Unavailable.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (Firebase will fail at runtime without google-services.json, but compiles fine)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/CloudCategorizationService.kt
git commit -m "add CloudCategorizationService for Firebase AI Gemini Flash-Lite"
```

---

### Task 7: Create SmartCategorizationService

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/categorization/SmartCategorizationService.kt`

- [ ] **Step 1: Create SmartCategorizationService**

Create `app/src/main/java/com/ayko/trackitt/service/categorization/SmartCategorizationService.kt`:

```kotlin
package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

class SmartCategorizationService(
    private val dictionary: DictionaryCategorizationService,
    private val nano: NanoCategorizationService,
    private val cloud: CloudCategorizationService,
    private val useOnDeviceAi: () -> Boolean,
) {

    suspend fun classify(
        merchant: String,
        reference: String? = null,
        note: String? = null,
    ): ClassificationResult {
        val dictionaryResult = dictionary.categorize(merchant, reference, note)
        if (dictionaryResult != TransactionCategory.OTHER) {
            return ClassificationResult.Classified(dictionaryResult)
        }

        if (useOnDeviceAi() && nano.isAvailable()) {
            val nanoResult = nano.classify(merchant, reference, note)
            if (nanoResult is ClassificationResult.Classified) {
                return nanoResult
            }
        }

        return cloud.classify(merchant, reference, note)
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/categorization/SmartCategorizationService.kt
git commit -m "add SmartCategorizationService orchestrating dictionary, nano, and cloud"
```

---

### Task 8: Create NetworkMonitor

**Files:**
- Create: `app/src/main/java/com/ayko/trackitt/service/network/NetworkMonitor.kt`

- [ ] **Step 1: Create NetworkMonitor**

Create `app/src/main/java/com/ayko/trackitt/service/network/NetworkMonitor.kt`:

```kotlin
package com.ayko.trackitt.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(connected)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        val currentNetwork = connectivityManager.activeNetwork
        val currentCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
        val initialState = currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(initialState)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/service/network/NetworkMonitor.kt
git commit -m "add NetworkMonitor for connectivity-based retry triggers"
```

---

### Task 9: Update TransactionRepository

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt`

- [ ] **Step 1: Update TransactionRepository to use SmartCategorizationService**

Replace the entire contents of `app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt`:

```kotlin
package com.ayko.trackitt.data.repository

import com.ayko.trackitt.data.local.dao.CategoryTotal
import com.ayko.trackitt.data.local.dao.TransactionDao
import com.ayko.trackitt.data.local.dao.MerchantMappingDao
import com.ayko.trackitt.data.local.entity.MerchantMappingEntity
import com.ayko.trackitt.data.local.entity.TransactionEntity
import com.ayko.trackitt.domain.model.Transaction
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.ClassificationResult
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.sms.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val smartCategorization: SmartCategorizationService,
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

    fun getCategoryTotals(year: Int, month: Int, includeFees: Boolean = false): Flow<List<CategoryTotal>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return if (includeFees) transactionDao.getCategoryTotalsWithFees(y, m)
        else transactionDao.getCategoryTotals(y, m)
    }

    fun getTodayTotal(): Flow<Double> {
        val (start, end) = todayRange()
        return transactionDao.getTotalForRange(start, end)
    }

    fun getCategoryTotalsForRange(startOfDay: Long, endOfDay: Long, includeFees: Boolean = false): Flow<List<CategoryTotal>> {
        return if (includeFees) transactionDao.getCategoryTotalsForRangeWithFees(startOfDay, endOfDay)
        else transactionDao.getCategoryTotalsForRange(startOfDay, endOfDay)
    }

    fun getDayTotal(startOfDay: Long, endOfDay: Long, includeFees: Boolean = false): Flow<Double> {
        return if (includeFees) transactionDao.getTotalForRangeWithFees(startOfDay, endOfDay)
        else transactionDao.getTotalForRange(startOfDay, endOfDay)
    }

    fun getMonthTotal(year: Int, month: Int, includeFees: Boolean = false): Flow<Double> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return if (includeFees) transactionDao.getMonthTotalWithFees(y, m)
        else transactionDao.getMonthTotal(y, m)
    }

    fun getByCategory(category: String, year: Int, month: Int): Flow<List<Transaction>> {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        return transactionDao.getByCategory(category, y, m).map { list -> list.map { it.toDomain() } }
    }

    fun getByCategoryForRange(category: String, startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> {
        return transactionDao.getByCategoryForRange(category, startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    fun getByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    fun searchByMerchant(query: String): Flow<List<Transaction>> {
        return transactionDao.searchByMerchant(query).map { list -> list.map { it.toDomain() } }
    }

    fun getById(id: Long): Flow<Transaction?> {
        return transactionDao.getById(id).map { it?.toDomain() }
    }

    suspend fun insertFromSms(parsed: ParsedTransaction): Boolean {
        if (parsed.isIncome) {
            val entity = parsed.toEntity(TransactionCategory.INCOME, needsClassification = false)
            return transactionDao.insert(entity) != -1L
        }

        val result = smartCategorization.classify(parsed.merchant, parsed.reference, null)
        val category: TransactionCategory
        val needsClassification: Boolean
        when (result) {
            is ClassificationResult.Classified -> {
                category = result.category
                needsClassification = false
            }
            is ClassificationResult.Unavailable -> {
                category = TransactionCategory.OTHER
                needsClassification = true
            }
        }

        val entity = parsed.toEntity(category, needsClassification)
        return transactionDao.insert(entity) != -1L
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
        transactionDao.updateNeedsClassification(id, false)
        merchantMappingDao.upsert(
            MerchantMappingEntity(
                merchant = merchant.lowercase().trim(),
                category = category.name,
            )
        )
    }

    suspend fun updateMerchant(id: Long, merchant: String) {
        transactionDao.updateMerchant(id, merchant)
    }

    suspend fun updateNote(id: Long, note: String?, merchant: String = "", reference: String? = null) {
        transactionDao.updateNote(id, note)
        if (!note.isNullOrBlank()) {
            val result = smartCategorization.classify(merchant, reference, note)
            when (result) {
                is ClassificationResult.Classified -> {
                    if (result.category != TransactionCategory.OTHER) {
                        transactionDao.updateCategory(id, result.category.name)
                    }
                    transactionDao.updateNeedsClassification(id, false)
                }
                is ClassificationResult.Unavailable -> {
                    // Dictionary returned OTHER and LLM was unreachable — leave category as-is
                }
            }
        }
    }

    suspend fun updateAmount(id: Long, amount: Double) {
        transactionDao.updateAmount(id, amount)
    }

    suspend fun delete(id: Long) {
        transactionDao.delete(id)
    }

    suspend fun retryPendingClassifications() {
        val pending = transactionDao.getPendingClassification()
        for (entity in pending) {
            val result = smartCategorization.classify(entity.merchant, entity.reference, entity.note)
            if (result is ClassificationResult.Classified) {
                transactionDao.updateCategory(entity.id, result.category.name)
                transactionDao.updateNeedsClassification(entity.id, false)
            }
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun ParsedTransaction.toEntity(
        category: TransactionCategory,
        needsClassification: Boolean,
    ): TransactionEntity {
        return TransactionEntity(
            amount = amount,
            fee = fee,
            isIncome = isIncome,
            category = category.name,
            merchant = merchant,
            reference = reference,
            transactionId = transactionId,
            source = source,
            note = null,
            date = date,
            isFromSms = true,
            rawSms = rawSms,
            needsClassification = needsClassification,
        )
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

Key changes:
- Constructor takes `SmartCategorizationService` instead of `CategorizationService`
- `insertFromSms()` uses `smartCategorization.classify()` and sets `needsClassification` based on result
- `updateNote()` now takes `merchant` and `reference` params, uses the full chain
- `updateCategory()` clears `needsClassification` when user manually overrides
- New `retryPendingClassifications()` function
- Helper `ParsedTransaction.toEntity()` to reduce duplication

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: May have compile errors until AppContainer is updated (Task 10). That's fine — continue to next task.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/data/repository/TransactionRepository.kt
git commit -m "update TransactionRepository to use SmartCategorizationService with retry"
```

---

### Task 10: Wire Up AppContainer and TrackittApplication

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/AppContainer.kt`
- Modify: `app/src/main/java/com/ayko/trackitt/TrackittApplication.kt`

- [ ] **Step 1: Update AppContainer**

Replace the entire contents of `app/src/main/java/com/ayko/trackitt/AppContainer.kt`:

```kotlin
package com.ayko.trackitt

import android.content.Context
import com.ayko.trackitt.data.local.TrackittDatabase
import com.ayko.trackitt.data.repository.TransactionRepository
import com.ayko.trackitt.domain.model.TransactionCategory
import com.ayko.trackitt.service.categorization.CloudCategorizationService
import com.ayko.trackitt.service.categorization.DictionaryCategorizationService
import com.ayko.trackitt.service.categorization.NanoCategorizationService
import com.ayko.trackitt.service.categorization.SmartCategorizationService
import com.ayko.trackitt.service.network.NetworkMonitor
import com.ayko.trackitt.service.sms.EcobankSmsParser
import com.ayko.trackitt.service.sms.MoMoSmsParser
import com.ayko.trackitt.service.sms.ParsedTransaction
import com.ayko.trackitt.service.sms.TelecelCashParser

class AppContainer(private val context: Context) {
    val database = TrackittDatabase.getInstance(context)

    private val dictionaryService = DictionaryCategorizationService(
        userLookup = { merchant ->
            val mapping = database.merchantMappingDao().getMapping(merchant)
            mapping?.let {
                try { TransactionCategory.valueOf(it.category) } catch (_: Exception) { null }
            }
        }
    )

    private val nanoService = NanoCategorizationService()
    private val cloudService = CloudCategorizationService()

    private val smartCategorization = SmartCategorizationService(
        dictionary = dictionaryService,
        nano = nanoService,
        cloud = cloudService,
        useOnDeviceAi = {
            context.getSharedPreferences("trackitt", 0)
                .getBoolean("use_on_device_ai", true)
        },
    )

    val repository = TransactionRepository(
        transactionDao = database.transactionDao(),
        merchantMappingDao = database.merchantMappingDao(),
        smartCategorization = smartCategorization,
    )

    val networkMonitor = NetworkMonitor(context)

    private val momoParser = MoMoSmsParser()
    private val ecobankParser = EcobankSmsParser()
    private val telecelParser = TelecelCashParser()

    private val allSenders = mapOf(
        "MobileMoney" to momoParser,
        "ECOBANK" to ecobankParser,
        "T-CASH" to telecelParser,
    )

    val smsSenders: List<String>
        get() {
            val prefs = context.getSharedPreferences("sms_sources", 0)
            return allSenders.keys.filter { prefs.getBoolean("source_$it", true) }
        }

    fun parseSms(sender: String?, body: String): ParsedTransaction? {
        val prefs = context.getSharedPreferences("sms_sources", 0)

        return when {
            sender.equals("MobileMoney", ignoreCase = true) -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body) else null
            }
            sender.equals("ECOBANK", ignoreCase = true) -> {
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body) else null
            }
            sender.equals("T-CASH", ignoreCase = true) -> {
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body) else null
            }
            else -> {
                if (prefs.getBoolean("source_MobileMoney", true)) momoParser.parse(body)?.let { return it }
                if (prefs.getBoolean("source_ECOBANK", true)) ecobankParser.parse(body)?.let { return it }
                if (prefs.getBoolean("source_T-CASH", true)) telecelParser.parse(body)?.let { return it }
                null
            }
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/AppContainer.kt
git commit -m "wire up SmartCategorizationService and NetworkMonitor in AppContainer"
```

---

### Task 11: Update HomeViewModel for Retry Triggers

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt`

- [ ] **Step 1: Add retry logic on launch and network restore**

In `app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt`, add the retry logic. Add these imports at the top:

```kotlin
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
```

Then add an `init` block after the `repository` and `displayPrefs` lines (around line 30):

```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TrackittApplication).container.repository
    private val displayPrefs = application.getSharedPreferences("trackitt_display", 0)
    private fun includeFees() = displayPrefs.getBoolean("show_fees", false)

    init {
        val container = (application as TrackittApplication).container
        viewModelScope.launch {
            repository.retryPendingClassifications()
        }
        viewModelScope.launch {
            container.networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    repository.retryPendingClassifications()
                }
            }
        }
    }

    // ... rest of the ViewModel unchanged
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/ui/screen/home/HomeViewModel.kt
git commit -m "trigger pending classification retries on launch and connectivity restore"
```

---

### Task 12: Update Detail Screen's updateNote Call

**Files:**
- Find and modify: the screen or ViewModel that calls `repository.updateNote(id, note)`

- [ ] **Step 1: Find all callers of updateNote**

Run: `grep -rn "updateNote" app/src/main/java/ --include="*.kt"`

The `updateNote` signature changed — it now requires `merchant` and `reference` parameters. Find every call site and update it to pass those values. The transaction object should be available in context at each call site (the detail screen has the full transaction).

For example, if `DetailViewModel.kt` has:

```kotlin
repository.updateNote(id, note)
```

Change it to:

```kotlin
repository.updateNote(id, note, transaction.merchant, transaction.reference)
```

Do this for every call site found by the grep.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -u
git commit -m "pass merchant and reference to updateNote for LLM categorization"
```

---

### Task 13: Add "Use On-Device AI" Toggle to Settings

**Files:**
- Modify: `app/src/main/java/com/ayko/trackitt/ui/screen/settings/SettingsScreen.kt`

- [ ] **Step 1: Add the AI section to SettingsScreen**

In `app/src/main/java/com/ayko/trackitt/ui/screen/settings/SettingsScreen.kt`, add a new section after the "Display" section (after line 277, before the `// ── Data & Backup ──` comment). Insert:

```kotlin
            // ── AI Categorization ──
            item { SectionHeader("AI Categorization") }

            item {
                val aiPrefs = remember { context.getSharedPreferences("trackitt", 0) }
                var useOnDeviceAi by remember { mutableStateOf(aiPrefs.getBoolean("use_on_device_ai", true)) }

                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Filled.Psychology,
                        label = "Use on-device AI",
                        subtitle = "Classify transactions with Gemini Nano when available",
                        trailing = {
                            androidx.compose.material3.Switch(
                                checked = useOnDeviceAi,
                                onCheckedChange = {
                                    useOnDeviceAi = it
                                    aiPrefs.edit().putBoolean("use_on_device_ai", it).apply()
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                        showDivider = false,
                    )
                }
            }
```

Note: `Icons.Filled.Psychology` is already available from the `material-icons-extended` dependency.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ayko/trackitt/ui/screen/settings/SettingsScreen.kt
git commit -m "add use on-device AI toggle to settings screen"
```

---

### Task 14: Build, Install, and Verify

- [ ] **Step 1: Build the debug APK**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on device**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success

- [ ] **Step 3: Verify the app launches and the database migration works**

Open the app. If it crashes on launch, check logcat:
Run: `adb logcat -s "TrackittDatabase" -d | tail -20`

The migration from version 3→4 should run silently. Existing transactions should still appear.

- [ ] **Step 4: Verify the Settings toggle exists**

Navigate to Settings. The "AI Categorization" section should appear with the "Use on-device AI" toggle, defaulting to ON.

- [ ] **Step 5: Commit (if any fixes were needed)**

```bash
git add -u
git commit -m "fix any build or runtime issues from LLM integration"
```

---

## Execution Prerequisites

Before Task 1 can compile successfully, the user must:
1. Create a Firebase project in Firebase Console
2. Register `com.ayko.trackitt` in the project
3. Download `google-services.json` to `app/`
4. Enable the Generative AI API in Firebase Console

If building before Firebase is set up, temporarily comment out the `google-services` plugin in both `build.gradle.kts` files and the Firebase dependencies. Uncomment after setup is complete.
