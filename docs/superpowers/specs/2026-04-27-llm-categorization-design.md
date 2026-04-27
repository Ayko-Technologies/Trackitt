# LLM-Powered Transaction Categorization

**Date:** 2026-04-27
**Status:** Approved

## Problem

The dictionary-based categorization system handles known merchants and reference keywords well, but fails on person-to-person transactions where the merchant name is a person (e.g., "KOFI ASARE") and only the note (e.g., "Intercession Retreat") provides enough context to classify correctly. These cases require natural language understanding — only an LLM can infer that an "Intercession Retreat" payment is a church transaction.

## Approach

Add LLM-based classification as a fallback behind the existing dictionary. When the dictionary returns OTHER, an LLM classifies the transaction using merchant name, note, and reference fields. The system prefers on-device inference (Gemini Nano via ML Kit) for privacy, falling through to a cloud API (Gemini Flash-Lite via Firebase AI Logic) when on-device is unavailable or fails.

## Classification Chain

```
Transaction arrives (SMS or manual)
  │
  ├─ Income? → INCOME (skip everything)
  │
  └─ Dictionary lookup (merchant + reference + note)
       ├─ Match found → use that category
       └─ Returns OTHER → LLM fallback
            │
            ├─ On-device AI enabled in settings?
            │    ├─ Yes → Gemini Nano available?
            │    │         ├─ Yes → run prompt → valid category returned?
            │    │         │                      ├─ Yes → use it
            │    │         │                      └─ No (OTHER/invalid/error) → cloud
            │    │         └─ No (unsupported/not downloaded) → cloud
            │    └─ No → cloud
            │
            └─ Cloud (Gemini Flash-Lite via Firebase AI Logic)
                 ├─ Success → valid category? → use it
                 └─ Failure (no internet/error) → OTHER + flag needsClassification
```

## LLM Prompt

Same prompt for both Nano and Cloud:

```
Classify this transaction into exactly one category.

Categories: FOOD, GROCERIES, TRANSPORT, BILLS, SHOPPING, TRANSFER, HOUSE, BEAUTY, CHURCH, HEALTH, AIRTIME, OTHER

Transaction:
- Merchant: {merchant}
- Note: {note or "none"}
- Reference: {reference or "none"}

Respond with only the category name, nothing else.
```

Response parsing: `TransactionCategory.valueOf(response.trim().uppercase())`. If parsing fails, treat as OTHER.

## Interface Changes

The `CategorizationService` interface expands:

```kotlin
interface CategorizationService {
    suspend fun categorize(
        merchant: String,
        reference: String? = null,
        note: String? = null
    ): TransactionCategory
}
```

Changes from current:
- Adds `note` parameter
- Becomes `suspend fun` (LLM calls are async)

## New Files

### `NanoCategorizationService.kt`
Wraps ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt:1.0.0-beta2`).
- `checkStatus()` to verify device support and model availability
- `generateContent()` with the classification prompt
- Returns OTHER on any failure (unavailable, unparseable response, exception)
- Manages model lifecycle (`warmup()`, `close()`)

### `CloudCategorizationService.kt`
Wraps Firebase AI Logic (`com.google.firebase:firebase-ai`).
- Uses Gemini 2.5 Flash-Lite model
- Same prompt as Nano
- Returns OTHER on any failure (no internet, API error, quota exceeded)
- Throws a distinct exception (or returns a sealed result) when failure is due to connectivity vs. genuine OTHER classification, so the caller can set `needsClassification` appropriately

### `SmartCategorizationService.kt`
Orchestrates the full chain: Dictionary → Nano → Cloud.
- Constructor takes: `DictionaryCategorizationService`, `NanoCategorizationService`, `CloudCategorizationService`, and a `useOnDeviceAi: () -> Boolean` lambda that reads the SharedPreferences toggle
- Does NOT implement `CategorizationService` — instead exposes `suspend fun classify(merchant, reference?, note?): ClassificationResult` where `ClassificationResult` is a sealed class: `Classified(category)` or `Unavailable` (all LLM sources failed)
- The repository calls this directly and uses the sealed result to decide whether to set `needsClassification = true`

### `NetworkMonitor.kt`
Registers a `ConnectivityManager.NetworkCallback` to detect connectivity changes.
- Exposes a `Flow<Boolean>` for network availability
- Used to trigger retry of pending classifications when connectivity is restored

## Modified Files

### `TransactionEntity.kt`
New column: `needsClassification` (Boolean, default false).

### `TransactionDao.kt`
New query: `getPendingClassification(): List<TransactionEntity>` — returns all transactions where `needsClassification = true`.
New update: `clearNeedsClassification(id: Long)`.

### `TrackittDatabase.kt`
Migration 3 → 4: `ALTER TABLE transactions ADD COLUMN needsClassification INTEGER NOT NULL DEFAULT 0`.
Version bumped to 4.

### `TransactionRepository.kt`
- `insertFromSms()`: passes `note` (null for SMS) to `categorize()`. Sets `needsClassification = true` if LLM was unreachable.
- `updateNote()`: passes merchant + note + reference to full chain. Sets `needsClassification` based on result.
- New `retryPendingClassifications()`: queries pending transactions, runs each through the LLM chain, updates category and clears flag on success.

### `AppContainer.kt`
Wires up the new services:
- Creates `NanoCategorizationService`
- Creates `CloudCategorizationService`
- Creates `SmartCategorizationService` with all three + settings toggle
- Passes `SmartCategorizationService` as the `CategorizationService` to `TransactionRepository`
- Creates `NetworkMonitor`

### `build.gradle.kts`
New dependencies:
```kotlin
implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
implementation("com.google.firebase:firebase-ai")
```
Add `com.google.gms.google-services` plugin.

### Settings Screen
New toggle: "Use on-device AI" (SharedPreferences key: `use_on_device_ai`, default true).

### Main ViewModel / Activity
- Call `retryPendingClassifications()` on app launch
- Register `NetworkMonitor` callback to trigger retries on connectivity restore

## Settings Toggle

- Key: `use_on_device_ai` in SharedPreferences namespace `"trackitt"`
- Default: `true`
- When OFF: Nano is skipped entirely, chain goes Dictionary → Cloud
- Purpose: escape hatch if Nano consistently misclassifies on user's device

## Retry System

**When `needsClassification` is set to true:**
- Nano was skipped or failed AND cloud was unreachable (no internet, error)
- The LLM pipeline never ran successfully

**When it stays false:**
- Dictionary matched (no LLM needed)
- LLM ran and returned a category (including OTHER — that's a real classification)
- Income transactions (always INCOME)

**Retry triggers:**
1. App opens — ViewModel calls `retryPendingClassifications()` on launch
2. Connectivity restored — `NetworkMonitor` fires, calls the same function

**Retry behavior:**
- Query all `needsClassification = true` transactions
- Run each through the LLM chain (Nano → Cloud)
- On success: update category, set `needsClassification = false`
- On failure: leave flag, retry on next trigger
- No aggressive polling or scheduling

## Privacy & Data Minimization

- Only merchant name, note, and reference are sent to the LLM — no amounts, dates, or personal identifiers
- On-device (Nano): nothing leaves the phone
- Cloud (Firebase AI Logic): data goes to Google's Gemini API. Free tier data may be used to improve Google products; paid tier opts out.
- Firebase AI Logic handles API key management — no keys embedded in APK

## Dependencies & Size Impact

| Dependency | APK Size Impact |
|-----------|----------------|
| ML Kit GenAI Prompt | Minimal (thin wrapper over AICore system service) |
| Firebase AI Logic + BoM | ~1-2 MB |
| **Total** | **~2-3 MB** |

No new permissions required (INTERNET already granted).

## Prerequisites (Manual Setup)

1. Create a Firebase project (or use existing one from Google Drive backup)
2. Register the app in Firebase Console
3. Download `google-services.json` to `app/` directory
4. Enable the Generative AI API in Firebase Console
5. Free tier (Spark plan) is sufficient

## What Stays Untouched

- SMS parsers (MoMo, Ecobank, Telecel Cash)
- Transaction UI screens (list, detail) — besides settings
- Merchant mapping / user override system
- Category enum and colors
- Database backup (Google Drive)
- All existing categories
