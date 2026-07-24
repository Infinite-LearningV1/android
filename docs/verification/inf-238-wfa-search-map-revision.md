# INF-238 WFA Search and Map Revision Verification

Verified on 2026-07-24.

## Source

- Branch: `codex/inf-238-wfa-search-revision`
- Base: `develop`
- Automated-gate starting HEAD: `c8b6346`
- Google Places SDK for Android remains pinned to `3.5.0`.
- Gradle wrapper is unchanged from `develop`.
- `local.properties`, `google-services.json`, and keystore files are not changed by this branch.

## Source hygiene

- `git diff develop...HEAD --check`: PASS
- No new raw `Color(0x...)` or linear/radial gradient in the changed Attendance/map flow: PASS
- Debug `isShrinkResources = false` removed: PASS
- Forbidden Places fields (`RATING`, `REVIEWS`, `PHOTO_METADATAS`, `OPENING_HOURS`, `WEBSITE_URI`) absent from the discovery implementation: PASS
- Existing raw colors reported under the Face Scanner are outside this branch diff.

## Automated gates

- `app:testDebugUnitTest`: PASS (`BUILD SUCCESSFUL`)
- `app:compileDebugAndroidTestKotlin`: PASS (`BUILD SUCCESSFUL`)
- `app:lintDebug`: PASS (`BUILD SUCCESSFUL`)
- `app:assembleDebug`: PASS (`BUILD SUCCESSFUL`)

### Final whole-branch review fixes

The final review follow-up started from `c7c86af` and added focused regression
coverage before implementation:

- Two sequential search previews now produce different renderer identities from
  their resolved place identity. A coordinate-bit identity is used only when a
  resolved place ID is unavailable; no locale-sensitive string or hash is used.
- The Google Maps marker subtree, `rememberMarkerState`, and info-window effect
  use that renderer identity. A later preview therefore replaces the marker
  position and title and reopens the compact callout.
- Search provider failures map to localized presentation resource IDs instead
  of Indonesian strings owned by `SearchViewModel`.
- Distance formatting uses the active Compose configuration locale.
- The search field receives a localized accessibility description and its
  leading search icon is decorative.
- `LocationSearchContentTest` resolves localized labels from Android resources
  instead of assuming an Indonesian test locale.

Fresh final-review gate:

- focused marker renderer regression: PASS;
- focused search ViewModel and distance-format tests: PASS;
- `app:testDebugUnitTest app:compileDebugAndroidTestKotlin app:lintDebug app:compileDebugKotlin`:
  PASS (`BUILD SUCCESSFUL`).

The first lint run identified ten WFA search strings that were missing from
`values-in`. The branch now uses English default resources and Indonesian
localized resources; a fresh lint run passed. Gradle continues to report its
existing Gradle 9 deprecation notice.

## Device and instrumentation evidence

- Device: Android Emulator `sdk_gphone16k_x86_64`, API 37
- Debug APK installation with `adb install -r`: PASS
- `AttendanceMapCameraDeliveryViewModelTest`: PASS, 4/4 tests
  - latest WFA selection supersedes older focus;
  - search preview survives map readiness and is consumed once;
  - map recreation restores the explicit selection;
  - late authoritative target resolution does not replace explicit preview focus.
- The first instrumentation launch was killed by a startup ANR while the
  emulator was under approximately 95% total CPU load and ran zero tests.
  After the emulator finished settling, the same targeted ViewModel suite
  completed 4/4.
- `CompactMapCalloutTest`: ENVIRONMENT BLOCKED before its assertion. Espresso
  on this API 37 image fails while initializing input injection with
  `NoSuchMethodException: android.hardware.input.InputManager.getInstance`.
  This is test-harness/API-image incompatibility evidence, not a product
  assertion failure.
- The final-review `LocationSearchContentTest` rerun encountered the same
  pre-assertion `InputManager.getInstance` incompatibility for all four cases.
  A fresh targeted ViewModel instrumentation retry then crashed the test process
  before discovery (`0 tests`). Deterministic unit, Android-test compilation,
  lint, and Kotlin compilation gates remain green; no new runtime-pass claim is
  made from these retries.

## Runtime

- Main sheet recommendation list removed: NEEDS VERIFICATION
- Bottom-sheet surface matches the topbar family without a blue gradient: NEEDS VERIFICATION
- One-character query does not request or show loading: NEEDS VERIFICATION
- Indonesian WFA search predictions: NEEDS VERIFICATION
- Latest query wins during rapid typing: NEEDS VERIFICATION
- Selected result zooms to detail level and shows the compact anchored callout: NEEDS VERIFICATION
- A newer marker selection supersedes the previous camera animation: NEEDS VERIFICATION
- WFO, WFH, and WFA marker colors render as blue, blue-accent, and orange: NEEDS VERIFICATION
- Offline search recovery and retry: NEEDS VERIFICATION

The authenticated Attendance flow and live Places responses were not exercised
because this run did not have a safe operator-provided test session and
Cloud-key confirmation. No credentials or secret-bearing logs were requested
or recorded.

## Google Cloud prerequisite

- Places API (New) enabled: NEEDS VERIFICATION
- Android application restriction matches the tested signing certificate: NEEDS VERIFICATION
- Maps SDK for Android and Places API (New) authorized on the key: NEEDS VERIFICATION
- Billing account/free-usage plan is active for the project: NEEDS VERIFICATION
- Budget and per-method quota alerts configured: NEEDS VERIFICATION

An operator should confirm these items in Google Cloud Console before runtime
acceptance. No API key or secret value is recorded in this document.
