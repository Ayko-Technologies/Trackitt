# Trackitt

[![Android CI](https://github.com/Ayko-Technologies/trackitt/actions/workflows/ci.yml/badge.svg)](https://github.com/Ayko-Technologies/trackitt/actions/workflows/ci.yml)

Trackitt is a local-first Android personal-finance tracker for Ghanaian mobile-money users. It reads supported transaction SMS messages on the device, turns them into editable transactions, and helps people understand spending, budgets, and monthly patterns.

Trackitt is early software. Review the source before trusting it with financial information, and keep independent records of any information that matters to you.

## What it does

- Parses supported MTN MoMo, Telecel Cash, and Ecobank transaction messages.
- Imports a chosen lookback period of supported SMS messages during onboarding.
- Stores transactions, categories, budgets, and insights in a local Room database.
- Supports manual transactions, custom categories, search, budgets, and budget alerts.
- Creates on-device monthly insights and optional on-device AI assistance when Gemini Nano is available.
- Can protect access with a PIN and device biometrics.
- Offers optional Google Drive App Data backups and restores.

The cloud AI fallback is deliberately disabled until Firebase is configured. Trackitt does not include an analytics SDK or a Trackitt-operated backend.

## Get the APK

Download the latest preview APK from [GitHub Releases](https://github.com/Ayko-Technologies/trackitt/releases). Enable installation from your browser or file manager when Android prompts you, then open the downloaded APK.

Preview releases are debug-signed while production release signing is being set up. They are installable, but they are not Google Play releases and should not be used for high-stakes financial decisions.

## Privacy and permissions

Trackitt needs SMS access to import and process supported financial transaction messages. The app processes those messages locally. Google Drive access is used only after you choose to connect an account and create or restore an App Data backup.

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

Contributions are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md), use the supplied issue templates, and do not include real SMS messages, database files, API keys, keystores, or Google service configuration in issues or pull requests.

For security-sensitive reports, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

## Releases

The release workflow builds and attaches a debug-signed APK for a `v*` tag. See [docs/RELEASING.md](docs/RELEASING.md) for the path to production signing.

## License

Trackitt source code is licensed under the [Apache License 2.0](LICENSE). Bundled third-party font notices are in [NOTICE](NOTICE).
