# Privacy and data handling

Trackitt is designed to keep personal-finance data on the device by default. This document describes the behavior in the open-source Android app, not a substitute for an independent security review.

## Data handled on the device

Trackitt can process:

- supported financial transaction SMS messages, including sender, message body, timestamp, amount, merchant, and transaction reference where present;
- transactions entered manually;
- budgets, categories, search terms, and locally generated insights; and
- app-lock settings, including a salted PIN hash when a PIN is enabled.

This data is stored in the app's private local storage using Room and Android preferences. Trackitt has no Trackitt-operated server and no analytics SDK in this codebase.

## Why SMS permission is requested

`RECEIVE_SMS` lets Trackitt react to newly received messages. `READ_SMS` lets the app import supported messages from a user-selected historical lookback period during onboarding. The parser ignores messages from unsupported senders.

You can revoke SMS permission in Android settings. Doing so stops new SMS imports and does not delete transactions already stored on the device.

## Optional data transfers

Google Drive backup is opt-in. When you connect a Google account and explicitly back up or restore, Trackitt uses Google Drive's App Data scope to upload or retrieve the local database backup. Those files are kept in the connected account's app-data area rather than a normal Drive folder.

On-device AI uses Gemini Nano only when it is available and enabled; those requests are processed on the device. The cloud AI fallback is currently disabled and no Firebase configuration is included in this repository.

## Security boundaries

Local storage is protected by Android's app sandbox, but device compromise, insecure backups, or a shared unlocked device can still expose data. App lock is a convenience control, not a replacement for device security. Keep Android updated and retain independent records for important financial information.

## Questions or reports

For a suspected privacy or security issue, follow [SECURITY.md](../SECURITY.md). For general questions, open a GitHub discussion or issue without including personal financial data.
