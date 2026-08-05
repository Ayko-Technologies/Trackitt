# Contributing to Trackitt

Thank you for helping improve Trackitt.

## Before you start

- Search existing issues before opening a new one.
- Use the bug-report template for reproducible defects and the feature-request template for proposals.
- Do not include real SMS messages, account numbers, transaction references, database files, Google configuration, API keys, or signing keys in public posts.
- Report security-sensitive concerns privately as described in [SECURITY.md](SECURITY.md).

## Local setup

Trackitt requires JDK 17 and Android SDK Platform 35.

```bash
git clone https://github.com/Ayko-Technologies/trackitt.git
cd trackitt
./gradlew test
```

Open the repository root in Android Studio to run on an emulator or device.

## Pull requests

1. Create a focused branch from `main`.
2. Keep the change scoped to one behavior or concern.
3. Add or update tests when behavior changes.
4. Run `./gradlew test` before opening the pull request.
5. Explain the problem, the change, and how you verified it.

By submitting a contribution, you agree to license it under the Apache License 2.0.
