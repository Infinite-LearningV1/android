# Android Master-Only Firebase Distribution

This document is the CI contract for Android Firebase App Distribution.

## Finalized workflow contract

- Firebase App Distribution runs only from `master`.
- The distribution workflow triggers on push to `master` only.
- There is no Firebase distribution from `develop`, `deploy`, `feature/**`, or `pull_request`.
- Release APK build command: `./gradlew app:assembleRelease`.
- Distributed artifact: `app/build/outputs/apk/release/app-release.apk`.
- Firebase CLI authentication is non-interactive through service account JSON written to `firebase-service-account.json` and exposed with `GOOGLE_APPLICATION_CREDENTIALS`.
- The workflow includes a GitHub Actions artifact upload step for generated release notes.
- The workflow includes a GitHub Actions artifact upload step for the signed APK.
- The workflow includes cleanup of temporary secret/config files: `release-keystore.jks`, `firebase-service-account.json`, `app/google-services.json`, and `local.properties`.

## Branch intent

- `feature/**` = isolated development and branch verification only.
- `develop` = integration and branch verification only.
- `deploy` = review/hardening and branch verification only.
- `master` = only branch allowed by the repository workflow contract to distribute to Firebase App Distribution.

The documented branch model and the workflow expectations should stay aligned: `deploy` is an intentional hardening lane, not an accidental extra branch.

## Distribution-ready means

A run is distribution-ready only when all of the following pass from `master`:

- release APK build completed with `./gradlew app:assembleRelease`
- release signing succeeded
- Firebase App Distribution upload succeeded
- tester group targeting succeeded through `FIREBASE_TESTER_GROUPS`
- release notes generation and GitHub Actions artifact upload step completed
- signed APK GitHub Actions artifact upload step completed
- cleanup step completed for temporary secret/config files

## End-to-end-ready means

All distribution-ready requirements passed, plus the backend final environment is reachable and smoke-tested by the intended tester path.

## Current truth

End-to-end readiness remains blocked until backend final readiness exists.

## Required GitHub secrets

- `MAPBOX_ACCESS_TOKEN`
- `GOOGLE_SERVICES_JSON_BASE64` preferred, or `GOOGLE_SERVICES_JSON` as fallback
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_APP_ID`

## Required GitHub variables

- `FIREBASE_TESTER_GROUPS`

## Authentication and generated files

The Firebase CLI must not require an interactive login in CI. The workflow writes the `FIREBASE_SERVICE_ACCOUNT_JSON` secret to `firebase-service-account.json`, sets `GOOGLE_APPLICATION_CREDENTIALS` to that file, and uses Firebase CLI under that service account context.

The workflow also reconstructs temporary build inputs from GitHub-managed secrets, including `app/google-services.json`, `release-keystore.jks`, and `local.properties`. These are temporary secret/config files, and the workflow includes cleanup for them.

## Needs Verification

Repository files can prove the intended workflow contract and trigger configuration, but they cannot by themselves prove GitHub branch protection or ruleset enforcement for `develop -> master` promotion.

Before treating the release process as governance-complete, verify in GitHub repository settings that branch protection / ruleset GitHub enforcement matches the team's policy for merging from `develop` to `master`.
