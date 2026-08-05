# Privacy-first data handling

Trackitt is designed to keep personal-finance data on the device by default. This document describes the behavior in the open-source Android app, not a substitute for an independent security review.

## Our privacy commitment

Trackitt does not transmit financial records to Ayko, a Trackitt-operated server, analytics provider, advertiser, or data broker. The app has no Trackitt account, no Trackitt backend, and no analytics SDK.

The only time a financial record can leave the device is when you deliberately start a Google Drive backup. That backup is sent only to the Google Drive App Data area of the Google account you choose. It is never scheduled automatically; Android cloud backup and device-to-device data migration are disabled.

## Data handled on the device

Trackitt can process:

- supported financial transaction SMS messages, including sender, message body, timestamp, amount, merchant, and transaction reference where present;
- transactions entered manually;
- budgets, categories, search terms, and locally generated insights; and
- app-lock settings, including a salted PIN hash when a PIN is enabled.

This data is stored in the app's private local storage using Room and Android preferences. Android cloud backup and device-to-device data migration are disabled, so Android does not independently copy app data to a backup or transfer service.

## Why SMS permission is requested

`RECEIVE_SMS` lets Trackitt react to newly received messages. `READ_SMS` lets the app import supported messages from a user-selected historical lookback period during onboarding. The parser ignores messages from unsupported senders.

You can revoke SMS permission in Android settings. Doing so stops new SMS imports and does not delete transactions already stored on the device.

## Optional Google Drive backups

Google Drive backup is opt-in and manual. When you connect a Google account and explicitly select backup or restore, Trackitt uses Google Drive's App Data scope to upload or retrieve the local database backup. The backup is stored in the connected account's app-data area rather than a normal Drive folder. Trackitt does not receive, retain, or have access to those backups outside the Google account you select.

On-device AI uses Gemini Nano only when it is available and enabled; those requests are processed on the device. Trackitt has no cloud-AI fallback and includes no Firebase configuration.

## Security boundaries

Local storage is protected by Android's app sandbox, but device compromise, a compromised Google account, or a shared unlocked device can still expose data. App lock is a convenience control, not a replacement for device security. Keep Android updated, secure the Google account you use for any backup, and retain independent records for important financial information.

## Questions or reports

For a suspected privacy or security issue, follow [SECURITY.md](../SECURITY.md). For general questions, open a GitHub discussion or issue without including personal financial data.
