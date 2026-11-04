# Privacy-first data handling

Trackitt is designed to keep personal-finance data on the device by default. This document describes the behavior in the open-source Android app, not a substitute for an independent security review.

## Our privacy commitment

Trackitt does not transmit financial records to Ayko, a Trackitt-operated server, analytics provider, advertiser, or data broker. The app has no Trackitt account, no Trackitt backend, and no analytics SDK.

Trackitt does not have Android's internet permission, so the app itself cannot send data anywhere. The only way a financial record leaves the app is a backup file you choose a place for. If you pick Google Drive (or another cloud app) in Android's file picker, that app uploads the file to your own account. Android cloud backup and device-to-device data migration are disabled.

The on-device AI library (Google ML Kit) would normally send Google anonymous usage logs. Trackitt removes the internet permission that library asks for, so those logs are never sent. The AI model itself runs in Android's AICore service on the device.

## Data handled on the device

Trackitt can process:

- supported financial transaction SMS messages, including sender, message body, timestamp, amount, merchant, and transaction reference where present;
- transactions entered manually;
- budgets, categories, search terms, and locally generated insights; and
- app-lock settings, including a salted PIN hash when a PIN is enabled.

This data is stored in the app's private local storage using Room and Android preferences. Android cloud backup and device-to-device data migration are disabled, so Android does not independently copy app data to a backup or transfer service.

## Why SMS permission is requested

`RECEIVE_SMS` lets Trackitt react to newly received messages. `READ_SMS` lets the app import transaction messages from a lookback period you choose. To recognise banks and wallets it has never seen before, Trackitt reads every SMS in that period on the device, keeps only messages that report a transaction on your own account (an amount plus your balance or account), and discards the rest without storing them. Messages from personal phone numbers are never treated as transactions. You can switch off individual banks and wallets, or automatic detection of new ones, under Settings > SMS sources.

Merchant receipts (for example from a payment service) are not stored as transactions. Trackitt keeps the amount, merchant name and time for up to two days to give the matching wallet transaction a clearer name, then deletes unmatched ones.

You can revoke SMS permission in Android settings. Doing so stops new SMS imports and does not delete transactions already stored on the device.

## Backups

Backups are copies of the local database, written to a file you choose with Android's file picker: phone storage, an SD card, a USB drive, or a cloud app such as Google Drive. Trackitt never signs in to Google or any other service. When the file is in a cloud app, that app uploads it to your account.

If you turn on automatic backup, Trackitt rewrites the same file about once a week and checks it reads back correctly. If it can no longer reach the file, it notifies you so you can choose it again. Treat backup files as you would a bank statement. Restoring checks that a file is a Trackitt database before replacing what is on the phone, and keeps the previous data as a safety copy.

On-device AI uses Gemini Nano only when it is available and enabled; those requests are processed on the device. Trackitt has no cloud-AI fallback and includes no Firebase configuration.

## Security boundaries

Local storage is protected by Android's app sandbox, but device compromise, access to wherever you keep backup files, or a shared unlocked device can still expose data. App lock is a convenience control, not a replacement for device security. Keep Android updated, secure the Google account you use for any backup, and retain independent records for important financial information.

## Questions or reports

For a suspected privacy or security issue, follow [SECURITY.md](../SECURITY.md). For general questions, open a GitHub discussion or issue without including personal financial data.
