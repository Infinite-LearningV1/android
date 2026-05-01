# Android GitHub Workflows

## Branch verification workflow

### `android-branch-verification.yml`
Runs branch-level Android verification for `feature/**`, `develop`, `deploy`, and pull request validation into `develop`, `deploy`, and `master`.

This workflow is for verification only. It does not upload to Firebase App Distribution and must not be treated as a release channel.

## Master distribution workflow

### `android-master-firebase-distribution.yml`
Runs Firebase App Distribution from `master` only.

Contract:
- Trigger: `push` to `master` only.
- No Firebase distribution from `develop`, `deploy`, `feature/**`, or `pull_request`.
- Build command: `./gradlew app:assembleRelease`.
- Distributed APK artifact: `app/build/outputs/apk/release/app-release.apk`.
- Firebase CLI authentication is non-interactive with service account JSON via `GOOGLE_APPLICATION_CREDENTIALS`.
- The workflow includes GitHub Actions artifact upload steps for both the release notes artifact and APK artifact.
- The workflow includes cleanup of temporary secret/config files: `release-keystore.jks`, `firebase-service-account.json`, `app/google-services.json`, and `local.properties`.

Required inputs are documented in `docs/ci/android-master-distribution.md`.
