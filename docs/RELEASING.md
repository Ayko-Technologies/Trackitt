# Releasing Trackitt

## Signing keys

Signing keys never go in this repository. Gradle reads them from `~/.config/trackitt/signing/` (or the directory given with `-Ptrackitt.signingDir=...`), or from `TRACKITT_*` environment variables in CI:

| File | Used for | Without it |
|---|---|---|
| `trackitt-dev.keystore` | Debug builds | The machine's own debug key |
| `release-credentials.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) | Release builds | Release builds are unsigned |

**Use one shared dev key on every machine.** Android only installs an update over an app signed with the same key. A debug build from a machine with a different key can't be installed over another one without uninstalling it, and that erases the app's data.

**Never lose the release key.** Apps signed with it can only be updated by builds signed with it. Keep the keystore and its passwords in a password manager, with an offline copy. Both keystores and their passwords are stored in the maintainers' password manager ("Trackitt app signing keys"; the keystores are base64 text fields).

## Releases

Pushing a tag such as `v0.2.0` runs the release workflow. When these repository secrets are set, it builds `assembleRelease` signed with the release key, so each release installs over the last:

- `TRACKITT_RELEASE_KEYSTORE_BASE64`: `base64 -w0 trackitt-release.keystore`
- `TRACKITT_RELEASE_STORE_PASSWORD`, `TRACKITT_RELEASE_KEY_ALIAS`, `TRACKITT_RELEASE_KEY_PASSWORD`
- Optionally `TRACKITT_DEV_KEYSTORE_BASE64`, so CI debug builds share the dev key too.

Without the release secrets, it falls back to a debug-signed preview APK as before.

Moving a phone from a debug build to a release build changes the signing key, so the app must be reinstalled once. Save a backup file first (Settings > Save a backup file) and restore it after installing.

Never commit a keystore, `local.properties`, database export, or secret to Git.
