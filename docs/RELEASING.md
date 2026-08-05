# Releasing Trackitt

## Preview APKs

Pushing a tag such as `v0.1.0` runs the release workflow. It builds `assembleDebug` and attaches an installable debug-signed APK to the matching GitHub prerelease. This allows people to test Trackitt without building it themselves.

Debug-signed APKs are suitable for previews only. Android treats them as a distinct signing identity from a future production release.

## Production signing

Before publishing a stable release or a Play build:

1. Generate a release keystore outside this repository.
2. Store the keystore and passwords in a secure secret manager with an offline recovery copy.
3. Add CI secrets for the encoded keystore, alias, and passwords.
4. Configure the release build to use those secrets and verify that updates preserve the signing identity.
5. Replace the preview-release workflow with a signed release workflow.

Never commit a keystore, `local.properties`, `google-services.json`, database export, or secret to Git.
