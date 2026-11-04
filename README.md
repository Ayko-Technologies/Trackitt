# Trackitt

[![Android CI](https://github.com/Ayko-Technologies/trackitt/actions/workflows/ci.yml/badge.svg)](https://github.com/Ayko-Technologies/trackitt/actions/workflows/ci.yml)

Trackitt is a privacy-first, local-first Android personal-finance tracker for Ghanaian mobile-money users. It reads supported transaction SMS messages on the device, turns them into editable transactions, and helps people understand spending, budgets, and monthly patterns.

Trackitt is early software. Review the source before trusting it with financial information, and keep independent records of any information that matters to you.

## Privacy promise

Trackitt does not send financial records to Ayko, a Trackitt server, analytics provider, advertiser, or data broker. There is no Trackitt account and no Trackitt-operated backend. SMS processing, transactions, categories, budgets, and insights remain in the app's private storage on the device.

Trackitt has no internet permission at all, so it cannot send anything anywhere. Backups are files you choose a place for with Android's file picker; pick Google Drive there and the Drive app keeps the file in your own Drive. Automatic weekly backup is off unless you enable it. Android cloud backup and device-to-device data migration are disabled.

## What it does

- Reads transaction SMS from MTN MoMo, Telecel (Vodafone) Cash, GhanaPay, AT Money, Ecobank, Stanbic, Republic Bank and other Ghanaian banks and wallets. Amounts are found whether the currency comes first or last ("GHS 50.00", "50.00 GHS", "GH¢50"), so new providers are picked up without an app update.
- Recognises money moved between your own wallets and banks, and leaves it out of spending and income. You can mark any other transaction as a transfer, or undo one.
- Lets you leave any single transaction out of your totals, such as a purchase someone else made with your card.
- Uses merchant receipts (for example from Zeepay, Slydepay or a fuel station) to name the real merchant behind a payment service.
- Shows each wallet's latest balance, and flags when balances stop adding up because an SMS never arrived.
- Imports a chosen lookback period of supported SMS messages during onboarding.
- Stores transactions, categories, budgets, and insights in a local Room database.
- Supports manual transactions, custom categories, search, budgets, and budget alerts.
- Creates on-device monthly insights and optional on-device AI assistance when Gemini Nano is available.
- Can protect access with a PIN and device biometrics.
- Backs up to a file you choose, now or every week, including in your own Google Drive, with no sign-in to Trackitt.

Trackitt has no cloud-AI fallback, analytics SDK, or Trackitt-operated backend.

## Get the APK

Download the latest preview APK from [GitHub Releases](https://github.com/Ayko-Technologies/trackitt/releases). Enable installation from your browser or file manager when Android prompts you, then open the downloaded APK.

Preview releases are debug-signed while production release signing is being set up. They are installable, but they are not Google Play releases and should not be used for high-stakes financial decisions.

## Privacy and permissions

Trackitt needs SMS access to import and process supported financial transaction messages. The app processes those messages locally. Backups go only where you choose to save them; no records are sent to Ayko.

Read the full [privacy and data-handling notes](docs/PRIVACY.md) before using the app.

## Build from source

Requirements:

- Android Studio with Android SDK Platform 35 installed
- JDK 17

```bash
git clone https://github.com/Ayko-Technologies/trackitt.git
cd trackitt
./gradlew test
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Open the project root in Android Studio to run it on an emulator or device.

## Contributing

Contributions are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md), use the supplied issue templates, and do not include real SMS messages, database files, Google account details, API keys, or keystores in issues or pull requests.

For security-sensitive reports, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

## Releases

The release workflow builds and attaches a debug-signed APK for a `v*` tag. See [docs/RELEASING.md](docs/RELEASING.md) for the path to production signing.

## License

Trackitt source code is licensed under the [Apache License 2.0](LICENSE). Bundled third-party font notices are in [NOTICE](NOTICE).
