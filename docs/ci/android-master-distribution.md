# Android Master-Only Firebase Distribution

## Branch intent
- `feature/*` = isolated development
- `develop` = integration
- `deploy` = review and hardening
- `master` = only branch allowed to distribute to Firebase App Distribution

The documented branch model and the workflow expectations should stay aligned: `deploy` is an intentional hardening lane, not an accidental extra branch.

## Distribution artifact
- Signed release APK only

## Distribution-ready means
- release APK built from `master`
- signing succeeded
- Firebase App Distribution upload succeeded
- tester group targeting succeeded
- release notes attached

## End-to-end-ready means
All distribution-ready requirements passed, plus backend final environment is reachable and smoke-tested.

## Current truth
End-to-end readiness remains blocked until backend final readiness exists.

## Required GitHub secrets
- `MAPBOX_ACCESS_TOKEN`
- `GOOGLE_SERVICES_JSON_BASE64` (preferred)
- `GOOGLE_SERVICES_JSON` (legacy fallback)
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_APP_ID`

## Required GitHub variables
- `FIREBASE_TESTER_GROUPS`
