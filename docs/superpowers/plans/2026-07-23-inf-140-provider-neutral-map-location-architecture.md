# INF-140 — Provider-Neutral Architecture and Google Maps Migration Plan

Date: 2026-07-23

Issue: `INF-140`

GitHub: `#94`

Branch: `codex/inf-140-provider-neutral-location`

Worktree: `E:\skrisi\android\.worktrees\inf-140-provider-neutral-location`

Base: `origin/develop` at `6f1c171`

Design:
`docs/superpowers/specs/2026-07-23-inf-140-provider-neutral-map-location-architecture.md`

Status: Draft revision 2 for operator approval

## Outcome

The branch will:

```text
extract provider-neutral location/map contracts
→ migrate place discovery to Google Places
→ migrate Attendance rendering to Google Maps Compose
→ verify runtime parity
→ remove Mapbox code, dependencies, and token wiring
```

Google is the final runtime provider. The temporary period where both providers
compile exists only inside the migration branch and is not a product-level
runtime switch.

## Execution Rules

- Work only in the isolated INF-140 worktree.
- Keep backend DTO scalar coordinates at transport boundaries.
- Use `GeoCoordinate` in final domain/presentation contracts.
- Implement vertical slices with tests; do not create unused parallel layers.
- Keep Mapbox only as a Git rollback checkpoint until Google runtime passes.
- Do not add a provider selector, remote flag, or permanent provider registry.
- Do not delete Mapbox until every required Google runtime row passes.
- Do not touch the main checkout's local network configuration.
- Never print, copy, or commit any Maps/Places key.
- Moving a tracked key to `local.properties` does not replace key rotation.
- Runtime-sensitive failures remain `Needs Verification`, not Done.
- Commit each phase independently.

## Phase 1 — Key Security, Baseline, and Geographic Primitives

### Task 1: Establish the official Google platform and key baseline

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `.github/workflows/android-branch-verification.yml`
- Modify: `.github/workflows/android-master-firebase-distribution.yml`
- Verify: `.gitignore`
- Create: `local.defaults.properties`
- Maintain: `docs/linear-sync/INF-140-google-maps-platform-baseline.md`

**Steps:**

1. Confirm without printing it that the Manifest contains a literal Google key.
2. Add Secrets Gradle Plugin 2.0.1 through the version catalog and apply it to
   the app module.
3. Add the non-secret `MAPS_API_KEY=missing` schema marker to the tracked defaults file and configure
   `defaultPropertiesFileName = "local.defaults.properties"`.
4. Put the developer key under `MAPS_API_KEY` in ignored `local.properties`.
5. Replace the Manifest literal with `${MAPS_API_KEY}` and keep only
   `com.google.android.geo.API_KEY`.
6. Consume `BuildConfig.MAPS_API_KEY` for later Places initialization; do not add
   another manual Google-key `Properties` reader.
7. Update both GitHub Actions workflows to append `MAPS_API_KEY` from an
   encrypted secret to their ephemeral `local.properties`, then retain cleanup.
8. Prefer separate development and release credentials behind the same
   property name.
9. Fail release assembly clearly when the default remains, without echoing the
   configured value.
10. Keep `local.properties` ignored and never stage it.
11. Enable billing and Maps SDK for Android, and confirm that this existing
    project already has Places API (Legacy) enabled for Places SDK 2.6.0.
12. Restrict each replacement credential by Android application ID, applicable
    signing SHA fingerprints, and only the required APIs.
13. Record debug, CI/release, and Play App Signing fingerprint coverage without
    recording the values.
14. Configure usage, quota, billing-budget, and unexpected-credential alerts
    for a monitored project owner.
15. Rotate the previously tracked key after the replacement is verified.
16. Keep the repository dependency baseline unchanged: Maps Compose 2.11.0,
    Play Services Maps 18.1.0, and Places 2.6.0.
17. Keep AGP 8.5.2, Gradle 8.7, `compileSdk` 34, and `targetSdk` 34 unchanged.
18. Treat dependency and Android toolchain upgrades as a separate task.

**Gate:**

```powershell
git grep -n "com.google.android.geo.API_KEY" -- app/src/main/AndroidManifest.xml
git status --ignored -s local.properties
.\gradlew.bat --no-daemon app:dependencyInsight `
  --dependency play-services-maps --configuration debugRuntimeClasspath
.\gradlew.bat --no-daemon app:dependencyInsight `
  --dependency places --configuration debugRuntimeClasspath
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Manual review must confirm a placeholder, not a literal; workflow output and
Gradle diagnostics must not reveal a secret. Key rotation is not rolled back if
source changes are reverted.

**Commit:**

```text
Secure Google Maps key configuration
```

### Task 2: Record the architecture baseline

**Files:**

- Create: `docs/linear-sync/INF-140-provider-leak-baseline.md`

**Inventory:**

```powershell
rg -n "^import com\.mapbox|^import com\.google\.android\.gms\.maps" app/src/main/java
rg -n "Pair<Double, Double>|Pair<\s*Double" app/src/main app/src/test app/src/androidTest
rg -n "FusedLocationProviderClient|MapAnimationTarget|MapView|Point|LatLng" app/src/main/java
rg -n "selectedTargetLocation|targetLocationMarker|selectedWfa|pickedLocation|selectedMarkerInfo" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Record:

- current Mapbox imports and callback boundaries;
- installed Google Maps/Places dependencies;
- historical Google Maps implementation paths and why they cannot be restored
  unchanged;
- direct platform-location access;
- coordinate-pair contracts;
- target/preview/camera state duplication;
- existing automated and runtime coverage.

### Task 3: Add characterization tests

**Files:**

- Create location legacy behavior tests.
- Extend
  `ResolveSelectedTargetLocationUseCaseTest.kt`.
- Extend `AttendancePreferenceRuntimeStateTest.kt`.
- Create Attendance map/target characterization tests.

**Required behavior:**

- WFO/WFH target resolution;
- current coordinate success/failure;
- existing search/reverse-geocode outcomes;
- marker/camera intent;
- geofence persistence round-trip;
- current unsafe WFA preview-to-target mutation explicitly documented for
  replacement in Phase 4.

### Task 4: Add geographic primitives

**Files:**

```text
domain/model/location/GeoCoordinate.kt
domain/model/location/GeoBounds.kt
domain/model/location/DistanceMeters.kt
```

Add tests for valid boundaries, invalid/non-finite values, inverted bounds, and
negative distance. Values must have no Android, Compose, Mapbox, or Google SDK
dependency.

### Task 5: Migrate core domain geographic models

Modify Attendance `Location`, `SelectedTargetLocation`, WFA models, geofence
candidates, and their mappers to own `GeoCoordinate`.

Rules:

- DTOs keep scalar latitude/longitude when required by backend contracts.
- Compatibility accessors are temporary within the phase.
- No target semantic change occurs yet.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

**Phase commit:**

```text
Introduce shared geographic primitives
```

## Phase 2 — Split Capabilities and Implement Google Places/Address

### Task 6: Extract current-device-location capability

**Create:**

```text
domain/model/location/CurrentLocation.kt
domain/repository/location/CurrentLocationRepository.kt
domain/use_case/location/GetCurrentLocationUseCase.kt
data/location/current/PlayServicesCurrentLocationDataSource.kt
data/location/current/AndroidLocationMapper.kt
data/location/current/CurrentLocationRepositoryImpl.kt
```

Tests cover coordinate, accuracy, timestamp, provider-null, invalid coordinate,
typed failure, and cancellation propagation.

`FusedLocationProviderClient` must leave domain use cases. Saved WFH coordinates
are not current-device-location results.

### Task 7: Implement Google Places discovery/details

**Create:**

```text
domain/model/location/PlaceSearch.kt
domain/model/location/PlaceDetails.kt
domain/repository/location/PlaceDiscoveryRepository.kt
domain/use_case/location/SearchPlacesUseCase.kt
domain/use_case/location/ResolvePlaceDetailsUseCase.kt
data/location/discovery/GooglePlacesDataSource.kt
data/location/discovery/GooglePlacesMapper.kt
data/location/discovery/GooglePlacesSessionManager.kt
data/location/discovery/GooglePlacesDiscoveryRepository.kt
```

**Implementation:**

```text
FindAutocompletePredictionsRequest
→ PlaceSuggestion

FetchPlaceRequest(placeId)
→ PlaceDetails
```

Steps:

1. Lazily initialize pinned Places SDK 2.6.0 once at the application boundary
   with `BuildConfig.MAPS_API_KEY`; use `Places.initialize` and keep client
   creation outside composables. Places API (New) remains a separate upgrade.
2. Treat missing/default configuration as a typed failure and never log the key.
3. Use one `AutocompleteSessionToken` per active search session.
4. Reuse that token for predictions and the selected Place Details request.
5. Renew the token after selection, cancellation, abandonment, or terminal
   recovery.
6. Keep place IDs opaque.
7. Request only `ID`, `DISPLAY_NAME`, `FORMATTED_ADDRESS`, and `LOCATION`.
8. Set Indonesia country/region context.
9. Prefer location bias around a fresh current location or authoritative target;
   search still works without proximity.
10. Debounce input and cancel superseded Google Tasks.
11. Resolve details explicitly before coordinates enter preview state.
12. Test duplicate names with distinct place IDs.
13. Map authentication, quota/rate, network, empty, and unavailable outcomes to
    typed failures.
14. Propagate coroutine cancellation into Google Tasks.

### Task 8: Implement typed address resolution

**Create:**

```text
domain/model/location/ResolvedAddress.kt
domain/repository/location/AddressResolver.kt
data/location/address/AndroidGeocoderDataSource.kt
data/location/address/AndroidAddressMapper.kt
data/location/address/AndroidGeocoderAddressResolver.kt
```

Replace `ReverseGeocodeUseCase` to consume `GeoCoordinate` and return:

```text
Resolved
CoordinateOnly
Failed
```

Do not send the Android-restricted Maps key to a direct Geocoding REST endpoint.
Tests cover complete/partial addresses, empty results, service unavailable,
invalid coordinate, typed failure, and cancellation.

### Task 9: Migrate capability consumers

**Modify:**

```text
AttendanceViewModel.kt
SearchViewModel.kt
SearchUiState.kt
WfaBookingViewModel.kt
affected screens, navigation results, fakes, and tests
```

Requirements:

- no coordinate pairs;
- optional search proximity if current location is unavailable;
- distinct suggestion and resolved-detail state;
- explicit place resolution before preview;
- coordinate-only presentation copy generated in presentation;
- no raw provider exception in UI.

### Task 10: Remove combined legacy location repository

Delete only after a zero-reference proof:

```text
LocationRepository.kt
LocationRepositoryImpl.kt
GetCurrentAddressUseCase.kt
GetCurrentCoordinatesUseCase.kt
SearchLocationUseCase.kt
LocationResult.kt
```

```powershell
rg -n "LocationRepository|LocationRepositoryImpl|GetCurrentAddressUseCase|GetCurrentCoordinatesUseCase|SearchLocationUseCase|LocationResult" `
  app/src/main app/src/test app/src/androidTest
```

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*CurrentLocation*' --tests '*Place*' --tests '*Address*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

**Phase commit:**

```text
Migrate location capabilities to Google services
```

## Phase 3 — Google Maps Compose Presentation

### Task 11: Add provider-neutral map models

**Create:**

```text
presentation/map/model/MapUiState.kt
presentation/map/model/MapMarkerUiModel.kt
presentation/map/model/MapCameraEffect.kt
presentation/map/model/AttendanceMapEvent.kt
presentation/map/mapper/TargetLocationMapMapper.kt
presentation/map/mapper/WfaRecommendationMapMapper.kt
presentation/map/mapper/CurrentLocationUiMapper.kt
```

Tests cover stable IDs, all marker roles, target/recommendation projection,
focus, fit, follow, and empty-fit behavior.

### Task 12: Implement `GoogleAttendanceMap`

**Create:**

```text
presentation/map/adapter/AttendanceMap.kt
presentation/map/adapter/GoogleAttendanceMap.kt
```

**Migrate/delete after parity:**

```text
presentation/components/maps/AttendanceMap.kt
utils/MapUtils.kt
```

Implementation:

1. Render through `GoogleMap` and `rememberCameraPositionState`.
2. Render project markers with Google `Marker` and
   `rememberUpdatedMarkerState`.
3. Render target radii with Google `Circle`.
4. Convert `GeoCoordinate ↔ LatLng` only inside the adapter package.
5. Translate camera effects with `CameraUpdateFactory` and
   `cameraPositionState.animate`.
6. Emit camera-idle center as `GeoCoordinate`.
7. Keep `CameraPositionState`, `MapProperties`, and `MapUiSettings` local to the
   adapter and remembered with stable inputs.
8. Enable scroll/zoom gestures; disable tilt/rotate, zoom controls, map toolbar,
   traffic, and indoor layers.
9. Keep Google My Location layer/button disabled. Render the project current
   location from `CurrentLocationRepository`.
10. Apply dynamic content padding for app bar, system bars, floating controls,
    and bottom sheet so Google attribution is not covered.
11. Add a localized semantic map description and keep focus/selection actions
    available as normal Compose controls.
12. Never launch permissions from the map composable.
13. Use stable marker IDs and updated callback state.
14. Preserve the precise-location fallback.
15. Use standard markers/circles for initial parity; do not add a Map ID,
    advanced markers, or cloud styling in this phase.

### Task 13: Replace persistent camera commands

**Modify:**

```text
AttendanceViewModel.kt
AttendanceScreenState.kt
AttendanceScreen.kt
```

Steps:

1. Add buffered `SharedFlow<MapCameraEffect>`.
2. Test current focus, target focus, recommendation fit, and error feedback.
3. Remove `MapAnimationTarget` from persistent state.
4. Remove `onMapAnimationHandled`.
5. Collect effects at the Route/map boundary.
6. Suppress duplicate rapid effects.
7. Keep collection lifecycle-aware.

**Static gate:**

```powershell
rg -n "MapAnimationTarget|onMapAnimationHandled|com\.mapbox" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt
```

Expected result: no matches.

**Build gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*Map*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

**Phase commit:**

```text
Implement Google Maps attendance rendering
```

Before Phase 4, device evidence must show Google base map, markers/circles,
target focus, current focus, bounds fit, and camera-idle pick behavior.

## Phase 4 — Authoritative Target, Geofence, and Google Cutover

### Task 14: Separate WFA preview from authoritative target

Write the failing invariant first:

```text
WFA recommendation marker click
does not mutate SelectedTargetLocation
```

Keep recommendation identity/details as preview state. Booking navigation is
explicit. Only the approved WFA booking resolver establishes the authoritative
WFA Attendance target.

Verify WFO/WFH behavior and Attendance request construction remain correct.

### Task 15: Collapse duplicate Attendance map/target state

Replace/remove:

```text
targetLocationMarker
legacy targetLocation business duplication
currentUserLatitude/currentUserLongitude
selectedWfaMarkerInfo object duplication
selectedMarkerInfo object duplication
```

Derive map markers from authoritative target and preview identities. Keep
`SelectedTargetLocation` as the only target accepted by eligibility and
submission.

### Task 16: Migrate geofence coordinate plumbing

Modify geofence candidate, manager, AttendancePreference, worker, and tests to
own `GeoCoordinate`. Preserve DataStore scalar key compatibility and map to SDK
scalars only at the Geofencing boundary.

Do not change request IDs, radius, cooldown, restore, registration, or active
monitoring semantics.

### Task 17: Cut runtime paths to Google

1. Make `GoogleAttendanceMap` the only Attendance renderer.
2. Make `GooglePlacesDiscoveryRepository` the only discovery binding.
3. Keep Play Services Location for current location/geofence.
4. Keep Android Geocoder behind `AddressResolver`.
5. Prove no runtime provider toggle exists.
6. Run the complete Google runtime parity matrix.
7. If a required row fails, restore the pre-cutover Git checkpoint and fix it
   before Mapbox deletion.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:assembleDebug
adb devices -l
```

**Phase commit:**

```text
Cut attendance maps over to Google
```

## Phase 5 — Remove Mapbox, ADR, and Evidence

### Task 18: Remove Mapbox and final legacy contracts

After the Google runtime gate passes, delete:

```text
Mapbox renderer/annotation utilities
Mapbox search/address repository code
Mapbox Retrofit service
Mapbox response DTOs
Mapbox API base URL/bindings
Mapbox SDK and Compose dependencies
Mapbox version aliases
MAPBOX_ACCESS_TOKEN Gradle/BuildConfig wiring
Mapbox Manifest metadata
final compatibility coordinate/map contracts
```

**Static gates:**

```powershell
rg -n "Pair<Double, Double>|MapAnimationTarget|LocationRepository|LocationResult" `
  app/src/main app/src/test app/src/androidTest

rg -n "com\.mapbox|MAPBOX_ACCESS_TOKEN|MapboxApiService" `
  app/src/main app/build.gradle.kts gradle/libs.versions.toml

rg -n "^import com\.google\.android\.gms\.maps|^import com\.google\.maps\.android\.compose" `
  app/src/main/java/com/example/infinite_track/domain `
  app/src/main/java/com/example/infinite_track/presentation/screen
```

Expected result: all return no matches. Google imports are allowed only in
provider data/presentation adapter packages and focused tests.

### Task 19: Write ADR

Create:

`docs/adr/ADR-XXX-provider-neutral-map-location-boundary.md`

Record:

- capability architecture;
- rejection of generic `MapRepository`;
- rejection of permanent multi-provider switching;
- Google Places session boundary;
- Android Geocoder decision;
- Maps Compose state/effect ownership;
- authoritative target invariant;
- key injection/rotation;
- Google cutover and Mapbox-removal gates.

### Task 20: Run automated gates

```powershell
.\gradlew.bat --no-daemon app:test
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:lint
```

Separate introduced lint findings from pre-existing findings with exact
evidence.

### Task 21: Run Google runtime verification

Create:

`docs/linear-sync/INF-140-runtime-verification.md`

Required matrix:

1. Maps SDK initializes using injected configuration.
2. Missing/default and invalid/restricted key failures are safe and do not
   disclose the configured value.
3. Google base map renders on a Play-enabled emulator.
4. Google base map renders on at least one physical device.
5. Precise current location succeeds with accuracy/freshness.
6. Location unavailable/failure recovery works.
7. Current-location marker is correct without the Google My Location layer.
8. Authoritative marker and radius are correct.
9. WFA recommendation remains preview-only.
10. Current-location focus works.
11. Target focus works.
12. Recommendation bounds fit works.
13. Pick-on-map camera idle returns the correct coordinate.
14. App bar, system bar, floating-control, and bottom-sheet padding is correct.
15. Google logo/copyright attribution remains visible.
16. TalkBack announces the map and essential actions work without map gestures.
17. Google Places search works with proximity.
18. Google Places search works without proximity.
19. Suggestion/detail resolution preserves place identity.
20. A completed/cancelled search receives a fresh session token.
21. Reverse geocode resolved and coordinate-only behavior is correct.
22. Geofence registration/restoration remains compatible.
23. Attendance request uses only the authoritative target.
24. No Mapbox code executes.

For every row record device/API, commit SHA, setup, expected, observed,
pass/fail, and evidence path.

A required row marked `Needs Verification` blocks Mapbox removal and issue
completion.

### Task 22: Final scope and security review

Verify:

- no API key literal exists in tracked source/docs/tests/logs;
- `local.properties` is ignored and untracked;
- Secrets Gradle Plugin owns Google key exposure to Manifest/BuildConfig;
- CI creates and removes ephemeral configuration without printing the key;
- Google key restrictions/rotation are recorded without the key value;
- billing, enabled APIs, quota, usage, and budget alerts are recorded;
- debug, CI/release, and Play App Signing fingerprint coverage is recorded
  without fingerprint values;
- App Check is either observe-only with a rollout record or explicitly deferred;
  enforcement cannot precede verified-traffic readiness;
- Google is the only runtime map/place provider;
- no runtime provider selector exists;
- Mapbox imports, services, dependencies, DTOs, token wiring, and Manifest
  metadata are absent;
- no Work Mode visual redesign or backend contract change entered scope;
- main checkout local network files were never staged.

```powershell
git diff origin/develop --stat
git diff origin/develop -- app/src/main/java/com/example/infinite_track/di/NetworkModule.kt
git diff --check origin/develop
```

### Task 23: Commit evidence and request review

Review focus:

```text
provider leaks and Mapbox removal
capability boundaries
cancellation and Google Task bridging
Google Places session lifecycle
Maps Compose camera/effect ownership
preview versus authoritative target
geofence compatibility
key restrictions and rotation
runtime evidence
rollback checkpoint
```

**Final commit:**

```text
Record INF-140 Google migration evidence
```

## Final Acceptance Gate

- [ ] Official fixed Google dependency versions are aligned and verified.
- [ ] Google key is injected through Secrets Gradle Plugin from ignored
      local/ephemeral CI configuration.
- [ ] Previously tracked keys are rotated and Android/API-restricted.
- [ ] Billing, Maps SDK for Android, the existing Places API (Legacy) service,
      quota, usage, and budget monitoring are configured.
- [ ] `GeoCoordinate` is the shared coordinate primitive.
- [ ] Domain and ViewModels contain no map-provider SDK types.
- [ ] No final coordinate contract uses `Pair<Double, Double>`.
- [ ] Current location includes accuracy and capture time.
- [ ] Search suggestions and place details are separate.
- [ ] Google Places SDK is the active discovery/details provider.
- [ ] Address resolution is typed and does not fake coordinate addresses.
- [ ] Google Maps Compose is the only active renderer.
- [ ] Map padding preserves attribution and TalkBack/non-gesture access passes.
- [ ] Camera work is a semantic one-time effect.
- [ ] Marker roles distinguish preview from authority.
- [ ] Marker clicks cannot replace `SelectedTargetLocation`.
- [ ] Approved WFA booking is WFA target authority.
- [ ] Geofence behavior is preserved.
- [ ] Google runtime parity passes.
- [ ] Mapbox code, service, DTOs, dependencies, token, and metadata are removed.
- [ ] No permanent multi-provider/runtime-selector architecture remains.
- [ ] App Check observe/enforcement status and rollout evidence are recorded.
- [ ] Unit, build, lint, instrumentation, runtime evidence, and ADR exist.

Do not move INF-140 to Done or unblock INF-238 until every non-runtime item
passes and every required Google runtime row is verified.
