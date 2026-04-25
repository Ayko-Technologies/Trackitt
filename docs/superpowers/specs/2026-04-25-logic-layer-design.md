# Trackitt Logic Layer — Design Spec

## Purpose

Replace all hardcoded UI data with a real persistence and processing layer. After this work, the app will parse incoming MTN MoMo SMS, categorize transactions automatically, store them in a local Room database, and let users back up to Google Drive manually.

## Architecture

**Pattern:** ViewModel + Repository. One `TransactionRepository` owns all data access. ViewModels expose Flows to Compose screens. SMS parsing writes to the repository. No DI framework — manual `AppContainer` in a custom Application class.

**Dependency chain:** Room → Repository → ViewModels → UI. SMS service and categorization engine plug into the repository.

---

## 1. Room Database

### TransactionEntity

| Column | Type | Notes |
|---|---|---|
| `id` | Long (auto) | Primary key |
| `amount` | Double | Always positive |
| `fee` | Double | MoMo fee charged |
| `isIncome` | Boolean | true = received, false = spent |
| `category` | String | Enum name: FOOD, TRANSPORT, BILLS, etc. |
| `merchant` | String | Parsed recipient/sender name |
| `reference` | String? | SMS reference field |
| `transactionId` | String | MoMo transaction ID (unique constraint, prevents duplicates) |
| `source` | String | "MTN MoMo", "Telecel Cash", etc. |
| `note` | String? | User-added note |
| `date` | Long | Epoch millis |
| `isFromSms` | Boolean | true = auto-parsed, false = manual entry |
| `rawSms` | String? | Original SMS body for debugging |

### MerchantMappingEntity

| Column | Type | Notes |
|---|---|---|
| `merchant` | String | Primary key, normalized (lowercase trimmed) |
| `category` | String | User-assigned category enum name |

### TransactionDao

- `insert(transaction)` — conflict strategy IGNORE (deduplicates on transactionId)
- `getByMonth(year, month): Flow<List<TransactionEntity>>` — all transactions for a given month
- `getToday(): Flow<List<TransactionEntity>>` — today's transactions
- `getCategoryTotals(year, month): Flow<List<CategoryTotal>>` — grouped sum by category for the chart
- `getTodayTotal(): Flow<Double>` — sum of today's expenses
- `getMonthTotal(year, month): Flow<Double>` — sum of month's expenses
- `searchByMerchant(query): Flow<List<TransactionEntity>>` — LIKE query on merchant column
- `getById(id): Flow<TransactionEntity>` — single transaction
- `updateCategory(id, category)` — update a transaction's category
- `delete(id)` — delete a transaction

### MerchantMappingDao

- `getMapping(merchant): MerchantMappingEntity?` — lookup by normalized name
- `upsert(mapping)` — insert or replace

### TrackittDatabase

- Version 1, no migrations
- Entities: TransactionEntity, MerchantMappingEntity
- Created as singleton via AppContainer

---

## 2. SMS Parsing

### SmsBroadcastReceiver

- Registered in AndroidManifest for `android.provider.Telephony.SMS_RECEIVED`
- Filters by sender ID (MTN MoMo sender number/short code)
- Passes raw SMS text to `MoMoSmsParser`
- On successful parse, inserts via `TransactionRepository`

### MoMoSmsParser

Pure function: `fun parse(smsBody: String): ParsedTransaction?`

Identifies transaction type by prefix:

| Prefix | Type | isIncome |
|---|---|---|
| "Payment made for" | P2P send | false |
| "Payment received for" | P2P receive | true |
| "Y'ello. You have Paid" | Merchant payment | false |
| "Your payment of" | Merchant confirmation | false |
| "Cash Out made for" | Agent withdrawal | false |
| "Payment for GHS" (contains "Bills.") | Bill payment | false |
| "You have received" | Bank transfer in | true |
| "Cash transfer recieved:" | Cash transfer in | true |

For each type, regex extracts:
- **Amount**: handles `GHS {amount}`, `GHS{amount}`, `{amount} GHS`
- **Merchant/recipient**: the name after "to" or "from"
- **Transaction ID**: always present as "Transaction ID:", "Transaction Id:", or "Financial Transaction Id:"
- **Fee**: "Fee charged: GHS{amount}" or "Fee was GHS {amount}" or "TRANSACTION FEE: {amount}"
- **Reference**: "Reference: {value}" when present
- **Date**: parsed when present in format "YYYY-MM-DD HH:mm:ss", otherwise uses current time

### ParsedTransaction

Data class holding all extracted fields, mapped to TransactionEntity before insert.

### Deduplication

Merchant payments produce two SMS (e.g., "Y'ello..." and "Your payment of...") sharing the same Transaction ID. Room's IGNORE conflict strategy on the unique `transactionId` column silently drops the duplicate.

### SmsBackfillWorker

- OneTimeWorkRequest via WorkManager
- Triggered after SMS permission is granted on first launch
- Reads SMS inbox via `ContentResolver` query on `content://sms/inbox`
- Filters for MoMo sender
- Parses each message, bulk-inserts
- Capped at 90 days of history
- Runs once, stores a "backfill_completed" flag in SharedPreferences

---

## 3. Categorization Engine

### CategorizationService

Interface: `fun categorize(merchant: String): TransactionCategory`

Lookup order:
1. **MerchantMappingDao** — check for user override (exact match on normalized name)
2. **Built-in dictionary** — case-insensitive substring match against ~40 known Ghanaian merchants
3. **Fallback** — return `OTHER`

### Built-in Dictionary (initial set)

```
melcom → SHOPPING
shoprite → GROCERIES
game → SHOPPING
palace → SHOPPING
maxmart → GROCERIES
ecg → BILLS
gwcl → BILLS (water)
dstv → BILLS
gotv → BILLS
vodafone → BILLS
airtel-tigo → BILLS
mtn → BILLS (when paying to MTN, not MoMo)
bolt → TRANSPORT
uber → TRANSPORT
yango → TRANSPORT
trotro → TRANSPORT
papaye → FOOD
kfc → FOOD
chicken inn → FOOD
pizza inn → FOOD
marwako → FOOD
burger king → FOOD
frankies → FOOD
expressPay → INCOME
ecobank → INCOME
gcb → INCOME
stanbic → INCOME
fidelity → INCOME
calbank → INCOME
Bills.INV → BILLS
bigpay → TRANSFER
```

### User Learning

When a user changes a transaction's category via the Detail screen:
1. Update the transaction's category in Room
2. Upsert to MerchantMappingEntity with the normalized merchant name → new category
3. Future transactions from the same merchant auto-categorize correctly

### Future AI Hook

The `CategorizationService` is an interface. V1 uses `DictionaryCategorizationService`. A future `AiCategorizationService` can swap in (TFLite model, on-device LLM, etc.) behind the same contract.

---

## 4. Google Drive Backup

### Manual only (v1)

Two buttons in Settings: "Back up now" and "Restore from backup."

### Export Flow

1. User taps "Back up now"
2. App initiates Google Sign-In (One Tap), scoped to `drive.appdata`
3. On auth success, close the Room database, copy the `.db` file
4. Upload to Google Drive hidden app folder (`appDataFolder`)
5. Filename: `trackitt-backup-{timestamp}.db`
6. Show confirmation snackbar with timestamp

### Import Flow

1. User taps "Restore from backup"
2. Google Sign-In if not already authed
3. List backup files from `appDataFolder` (show date and size)
4. User selects one
5. Download `.db` file, close current Room database, replace file
6. Restart the database, refresh UI
7. Show confirmation

### Dependencies

- `com.google.api-client:google-api-client-android`
- `com.google.apis:google-api-services-drive`
- `com.google.android.gms:play-services-auth`

### Auth

- Google Identity Services (One Tap)
- Scoped narrowly to `drive.appdata` only — cannot see user's other Drive files
- Token kept in memory, re-auth when expired
- No persistent credentials stored on device

---

## 5. ViewModel + UI Wiring

### AppContainer

Created in `TrackittApplication` (custom Application class). Holds:
- `TrackittDatabase` singleton
- `TransactionRepository`
- `CategorizationService`

ViewModels access it via `(application as TrackittApplication).container`.

### HomeViewModel

- `selectedPeriod: MutableStateFlow<Period>` — TODAY or MONTH
- `total: Flow<Double>` — switches between today/month total based on period
- `chartCategories: Flow<List<ChartCategory>>` — category totals for current month, sorted descending
- `transactions: Flow<List<Transaction>>` — current period's transactions
- `leadCategory: Flow<TransactionCategory?>` — derived from chartCategories for dynamic color

### AddViewModel

- `amount: MutableStateFlow<String>`
- `selectedCategory: MutableStateFlow<TransactionCategory?>`
- `note: MutableStateFlow<String>`
- `source: MutableStateFlow<String>` — defaults to "MTN MoMo"
- `save()` — validates amount > 0 and category selected, creates Transaction, inserts via repository, returns success/failure

### DetailViewModel

- Takes `transactionId: Long` from nav argument
- `transaction: Flow<Transaction>` — single transaction from Room
- `suggestedCategory: TransactionCategory` — what the engine guessed
- `updateCategory(category)` — updates transaction + saves MerchantMapping
- `delete()` — removes transaction from Room

### SearchViewModel

- `query: MutableStateFlow<String>`
- `results: Flow<List<Transaction>>` — debounced 300ms, Room LIKE query on merchant
- `recentSearches: Flow<List<String>>` — last 5 terms, stored in SharedPreferences

### UI Changes

- Delete `FakeHomeData.kt`
- All screens receive their ViewModel via `viewModel()` factory
- Hardcoded date strings ("April 2026", "Today · 2:14 PM") replaced with formatted values from transaction data
- MonthChart header ("TRACKITT · APRIL") derives month name from selected period
- Empty state triggers naturally when Flow emits empty list
- Detail screen receives transaction ID via nav argument, loads real data

---

## 6. First Launch Flow

1. App opens → Home screen shows empty state (no transactions)
2. App checks SMS permission → if not granted, shows a card: "Allow SMS access to automatically track your MoMo transactions"
3. User grants permission → app enqueues `SmsBackfillWorker`
4. Worker scans last 90 days of MoMo SMS, parses and inserts
5. Home screen populates as transactions flow in
6. `SmsBroadcastReceiver` is now active for future messages
7. If user denies permission → app works fine with manual entry only, re-prompts gently later

---

## What's NOT in this spec

- Telecel Cash / AT Money parsing (future — user's Telecel phone is dead)
- Auto backup (future — manual only for v1)
- AI/ML categorization (future — interface is ready)
- Budget tracking feature (future — the "budgets" FAB is a placeholder)
- Settings content beyond backup (future)
- Bottom sheet for category drill-down (UI exists in Paper but logic deferred)
