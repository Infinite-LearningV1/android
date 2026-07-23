# INF-140 — Provider-Neutral Map and Location Implementation Plan

Date: 2026-07-23

Issue: `INF-140`

GitHub: `#94`

Branch: `codex/inf-140-provider-neutral-location`

Worktree: `E:\skrisi\android\.worktrees\inf-140-provider-neutral-location`

Base: `origin/develop` at `6f1c171`

Design:
`docs/superpowers/specs/2026-07-23-inf-140-provider-neutral-map-location-architecture.md`

Status: Draft for operator approval

## Execution Rules

- Work only in the isolated INF-140 worktree.
- Preserve Mapbox as the active provider.
- Implement vertical slices; do not add unused parallel architecture.
- Write or update a failing test before each behavior-changing implementation.
- Keep backend DTO coordinate scalars at transport boundaries.
- Do not touch local network configuration from the main `develop` checkout.
- Do not delete legacy contracts until all consumers and tests have migrated.
- Stop and record `Needs Verification` when a runtime gate cannot be executed.
- Commit each phase independently so rollback does not require history surgery.

## Phase 1 — Characterization and Geographic Primitives

### Task 1: Record the provider and coordinate leak baseline

**Files:**

- Create:
  `docs/linear-sync/INF-140-provider-leak-baseline.md`

**Steps:**

1. Record all production imports of Mapbox and Google map SDK types.
2. Record every `Pair<Double, Double>` coordinate contract.
3. Record direct `FusedLocationProviderClient` consumers.
4. Record overlapping Attendance target/preview/marker/camera state fields.
5. Record all map callbacks exposing `Point` or `MapView`.
6. Record current test coverage and missing runtime coverage.

**Commands:**

```powershell
rg -n "^import com\.mapbox|^import com\.google\.android\.gms\.maps" app/src/main/java
rg -n "Pair<Double, Double>|Pair<\s*Double" app/src/main/java app/src/test app/src/androidTest
rg -n "FusedLocationProviderClient|MapAnimationTarget|MapView|Point" app/src/main/java
rg -n "selectedTargetLocation|targetLocationMarker|selectedWfa|pickedLocation|selectedMarkerInfo" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

**Commit:**

```text
Document INF-140 provider leak baseline
```

### Task 2: Add characterization tests for existing critical behavior

**Files:**

- Create:
  `app/src/test/java/com/example/infinite_track/domain/use_case/location/LocationLegacyBehaviorTest.kt`
- Create:
  `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceMapTargetCharacterizationTest.kt`
- Modify:
  `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt`
- Modify:
  `app/src/test/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreferenceRuntimeStateTest.kt`

**Steps:**

1. Characterize current coordinate success/failure and fallback behavior.
2. Characterize WFO and WFH target resolution.
3. Explicitly mark current WFA preview-to-target behavior as the unsafe behavior
   that Phase 4 will replace.
4. Characterize geofence persistence round-trip.
5. Keep tests provider-independent where possible.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*LocationLegacyBehaviorTest' `
  --tests '*ResolveSelectedTargetLocationUseCaseTest' `
  --tests '*AttendancePreferenceRuntimeStateTest'
```

### Task 3: Add geographic primitives and validation

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/GeoCoordinate.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/GeoBounds.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/DistanceMeters.kt`
- Create:
  `app/src/test/java/com/example/infinite_track/domain/model/location/GeoCoordinateTest.kt`
- Create:
  `app/src/test/java/com/example/infinite_track/domain/model/location/GeoBoundsTest.kt`

**Steps:**

1. Test valid boundary coordinates.
2. Test invalid latitude, longitude, non-finite values, inverted bounds, and
   negative distance.
3. Implement project-owned values without Android, Compose, or provider types.
4. Keep validation/mapping APIs explicit rather than silently clamping values.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GeoCoordinateTest' `
  --tests '*GeoBoundsTest'
```

### Task 4: Migrate core domain geographic models

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/domain/model/attendance/SelectedTargetLocation.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/geofencing/ReminderGeofenceCandidate.kt`
- Modify affected data mappers and tests.

**Steps:**

1. Move geographic ownership to `coordinate: GeoCoordinate`.
2. Retain scalar DTO fields at network boundaries.
3. Add mapper tests before changing each production mapper.
4. Update consumers mechanically without changing target semantics in this
   task.
5. Use temporary compatibility accessors only within the phase.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

**Phase 1 commit:**

```text
Introduce shared geographic primitives
```

**Rollback checkpoint:** tag the phase commit SHA in the runtime evidence
document before capability extraction starts.

## Phase 2 — Split Current Location, Discovery, and Address Capabilities

### Task 5: Extract current-device-location capability

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/CurrentLocation.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/repository/location/CurrentLocationRepository.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/GetCurrentLocationUseCase.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/current/PlayServicesCurrentLocationDataSource.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/current/AndroidLocationMapper.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/current/CurrentLocationRepositoryImpl.kt`
- Create focused unit tests under matching packages.
- Modify:
  `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`

**Steps:**

1. Test Android-location mapping with coordinate, accuracy, and timestamp.
2. Test null provider result, invalid coordinate, platform failure, and
   cancellation propagation.
3. Add typed `LocationAccuracy` and result/failure contracts.
4. Keep permission inspection outside the repository; map platform security
   failures to typed outcomes.
5. Bind the implementation through Hilt.
6. Do not add saved-WFH fallback to this repository.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*CurrentLocation*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

### Task 6: Extract place-discovery capability

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/PlaceSearch.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/PlaceDetails.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/repository/location/PlaceDiscoveryRepository.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/SearchPlacesUseCase.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/ResolvePlaceDetailsUseCase.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/discovery/MapboxPlaceDiscoveryDataSource.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/discovery/MapboxPlaceDiscoveryMapper.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/discovery/MapboxPlaceCandidateStore.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/discovery/MapboxPlaceDiscoveryRepository.kt`
- Create focused mapper, store, and repository tests.
- Modify Hilt bindings.

**Steps:**

1. Test query normalization, optional proximity, and result limit.
2. Map provider feature identity into a stable suggestion ID.
3. Keep full provider candidates only inside the memory-scoped adapter store.
4. Test suggestion/detail separation.
5. Test missing and expired candidate identity.
6. Test duplicate names with distinct provider IDs.
7. Preserve the existing Mapbox forward endpoint behavior.
8. Propagate cancellation.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*Place*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

### Task 7: Extract address-resolution capability

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/domain/model/location/ResolvedAddress.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/domain/repository/location/AddressResolver.kt`
- Replace:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/ReverseGeocodeUseCase.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/address/MapboxAddressDataSource.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/address/MapboxAddressMapper.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/data/location/address/MapboxAddressResolver.kt`
- Create focused tests.
- Modify Hilt bindings.

**Steps:**

1. Test complete address mapping.
2. Test partial provider address mapping.
3. Test empty feature list as `CoordinateOnly`.
4. Test provider/network failure as typed `Failed`.
5. Test cancellation propagation.
6. Remove successful fake coordinate-address strings from the data layer.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*Address*' --tests '*ReverseGeocode*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

### Task 8: Migrate location capability consumers

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchUiState.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`
- Modify affected screens, navigation result contracts, fakes, and tests.

**Steps:**

1. Replace raw coordinate pairs with `CurrentLocation`.
2. Make search proximity optional when current location is unavailable.
3. Represent suggestions and resolved details as distinct state.
4. Require explicit place-detail resolution before pick/search selection enters
   WFA preview state.
5. Map `CoordinateOnly` to explicit presentation copy only at the UI mapper.
6. Preserve current user-facing recovery behavior without raw exceptions.
7. Update instrumentation fakes to implement separate repositories.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*SearchViewModel*' `
  --tests '*WfaBookingViewModel*' `
  --tests '*AttendanceViewModel*'
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

### Task 9: Remove the legacy combined repository

**Files:**

- Delete after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/domain/repository/LocationRepository.kt`
- Delete after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/data/repository/location/LocationRepositoryImpl.kt`
- Delete/replace after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/GetCurrentAddressUseCase.kt`
- Delete/replace after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/GetCurrentCoordinatesUseCase.kt`
- Delete/replace after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/domain/use_case/location/SearchLocationUseCase.kt`
- Delete after zero-reference proof:
  `app/src/main/java/com/example/infinite_track/domain/model/location/LocationResult.kt`
- Modify Hilt bindings and test fakes.

**Zero-reference gate:**

```powershell
rg -n "LocationRepository|LocationRepositoryImpl|GetCurrentAddressUseCase|GetCurrentCoordinatesUseCase|SearchLocationUseCase|LocationResult" `
  app/src/main app/src/test app/src/androidTest
```

The command must return no legacy production consumers before deletion.

**Phase 2 commit:**

```text
Split location provider capabilities
```

**Rollback checkpoint:** preserve the Phase 1 SHA and Phase 2 SHA in the
verification document. Revert Phase 2 as a unit if search, address, or current
location runtime parity fails.

## Phase 3 — Provider-Neutral Map Presentation

### Task 10: Add project-owned map models and mappers

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/model/MapUiState.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/model/MapMarkerUiModel.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/model/MapCameraEffect.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/model/AttendanceMapEvent.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/mapper/TargetLocationMapMapper.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/mapper/WfaRecommendationMapMapper.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/mapper/CurrentLocationUiMapper.kt`
- Create focused tests.

**Steps:**

1. Test all marker roles.
2. Test stable marker IDs independent of display text.
3. Test authoritative target projection.
4. Test recommendation/search preview projection.
5. Test focus, fit, and follow semantic effects.
6. Reject an empty fit-coordinate request or map it to a typed no-op.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*MapMapper*' `
  --tests '*MapCameraEffect*'
```

### Task 11: Move Mapbox execution into the map adapter

**Files:**

- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/adapter/AttendanceMap.kt`
- Create:
  `app/src/main/java/com/example/infinite_track/presentation/map/adapter/MapboxAttendanceMapAdapter.kt`
- Migrate or delete after parity:
  `app/src/main/java/com/example/infinite_track/presentation/components/maps/AttendanceMap.kt`
- Migrate or delete after parity:
  `app/src/main/java/com/example/infinite_track/utils/MapUtils.kt`
- Modify:
  `app/src/androidTest/java/com/example/infinite_track/presentation/components/maps/AttendanceMapApiCompatibilityFixture.kt`
- Add adapter contract/instrumentation tests.

**Steps:**

1. Preserve the current Mapbox style and gesture configuration.
2. Move annotation creation and provider marker IDs into the adapter.
3. Map `GeoCoordinate` to `Point` only inside the adapter.
4. Map camera-idle `Point` back to `GeoCoordinate` before emitting events.
5. Move `CameraOptions`, bounds, and `flyTo` into the adapter.
6. Remove public `MapView` and `Point` callbacks.
7. Explicitly dispose annotation managers, camera subscriptions, handlers, and
   pending callbacks.
8. Preserve the precise-location fallback.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

### Task 12: Replace persistent camera commands with one-time effects

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Create:
  `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceMapCameraEffectTest.kt`
- Modify affected instrumentation tests.

**Steps:**

1. Add buffered `SharedFlow<MapCameraEffect>`.
2. Test target focus, current-location focus, recommendation fit, and error
   feedback.
3. Remove `MapAnimationTarget` from persistent state.
4. Remove `onMapAnimationHandled`.
5. Collect effects at the Route/map-adapter boundary.
6. Prevent duplicate effects from rapid repeated actions.
7. Keep effect handling lifecycle-aware and idempotent.

**Static gate:**

```powershell
rg -n "MapAnimationTarget|onMapAnimationHandled|com\.mapbox" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt
```

Expected result: no matches.

**Phase 3 commit:**

```text
Isolate Mapbox presentation execution
```

**Runtime rollback gate:** do not continue to Phase 4 until an emulator/device
confirms map rendering, style, markers, target focus, current-location focus,
recommendation fit, and pick-on-map camera idle.

## Phase 4 — Authoritative Target and Geofence Coordinate Consumers

### Task 13: Separate WFA preview from authoritative target

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify relevant WFA booking resolver/use cases and UI mapping.
- Modify:
  `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt`
- Create:
  `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/WfaPreviewAuthorityTest.kt`

**Steps:**

1. Write a failing test proving a recommendation-marker click cannot mutate
   `SelectedTargetLocation`.
2. Keep preview identity and preview content in presentation state.
3. Make the booking action explicit.
4. Resolve the authoritative WFA target only from the approved booking
   contract.
5. Keep WFO and WFH authoritative target behavior stable.
6. Verify Attendance request construction accepts only the authoritative
   target.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*ResolveSelectedTargetLocationUseCaseTest' `
  --tests '*WfaPreviewAuthorityTest' `
  --tests '*AttendanceCheckInRequestFactoryTest'
```

### Task 14: Collapse duplicate Attendance map/target state

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify related components and tests.

**Steps:**

1. Derive authoritative marker projection from `SelectedTargetLocation`.
2. Replace scalar current-user coordinates with a typed current-location/UI
   model.
3. Replace duplicated selected marker objects with stable preview marker ID and
   role.
4. Remove `targetLocationMarker`.
5. Remove legacy `targetLocation` when all business consumers use
   `SelectedTargetLocation`.
6. Remove duplicate WFA selected/marker-info object ownership.
7. Keep UI-local modal visibility out of domain state.

**Static gate:**

```powershell
rg -n "targetLocationMarker|currentUserLatitude|currentUserLongitude|selectedWfaMarkerInfo|selectedMarkerInfo" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Expected result: no legacy duplicate fields or mutations.

### Task 15: Migrate geofence coordinate plumbing

**Files:**

- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/geofencing/ReminderGeofenceCandidate.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceManager.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreference.kt`
- Modify:
  `app/src/main/java/com/example/infinite_track/data/worker/LocationEventWorker.kt`
- Modify related tests.

**Steps:**

1. Change candidate and persisted runtime models to own `GeoCoordinate`.
2. Preserve the existing DataStore scalar key format.
3. Map to latitude/longitude only at Geofencing SDK and backend request
   boundaries.
4. Preserve request ID, radius, cooldown, restore, and removal behavior.
5. Add compatibility fixtures for previously persisted values.
6. Do not change geofence registration policy.

**Gate:**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*Geofence*' `
  --tests '*AttendancePreferenceRuntimeStateTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

**Phase 4 commit:**

```text
Enforce authoritative location targets
```

**Rollback checkpoint:** retain compatibility mappers until device evidence
confirms Attendance submission and geofence restoration.

## Phase 5 — Cleanup, ADR, and Runtime Evidence

### Task 16: Remove final legacy contracts

**Files:**

- Delete obsolete location, map, and compatibility files only after reference
  scans are empty.
- Modify tests/fakes that still encode provider or pair contracts.

**Steps:**

1. Remove final `Pair<Double, Double>` coordinate contracts.
2. Remove final `LocationResult`.
3. Remove final `MapAnimationTarget`.
4. Remove obsolete `MapUtils` provider execution.
5. Ensure provider imports remain only in data/presentation adapters and
   focused adapter tests.
6. Ensure no unused generic provider abstractions were introduced.

**Static gate:**

```powershell
rg -n "Pair<Double, Double>|MapAnimationTarget|LocationRepository|LocationResult" `
  app/src/main app/src/test app/src/androidTest

rg -n "^import com\.mapbox|^import com\.google\.android\.gms\.maps" `
  app/src/main/java/com/example/infinite_track/domain `
  app/src/main/java/com/example/infinite_track/presentation/screen
```

Expected result: no matches.

### Task 17: Write ADR and migration gate

**Files:**

- Create:
  `docs/adr/ADR-XXX-provider-neutral-map-location-boundary.md`

**Steps:**

1. Record the capability-based decision.
2. Record rejected generic repository and permanent multi-provider approaches.
3. Record the temporary Mapbox search-candidate store.
4. Record semantic camera ownership and Compose lifecycle boundary.
5. Record the authoritative target invariant.
6. Record Google migration entry and rollback gates.

### Task 18: Run full automated gates

**Commands:**

```powershell
.\gradlew.bat --no-daemon app:test
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:lint
```

If lint includes pre-existing failures, separate them from introduced findings
with exact file/line evidence.

### Task 19: Execute runtime verification

**Files:**

- Create:
  `docs/linear-sync/INF-140-runtime-verification.md`

**Required environment:**

```powershell
adb devices -l
```

**Matrix:**

1. Precise current location succeeds with accuracy/freshness metadata.
2. Location unavailable/failure shows recoverable typed feedback.
3. Mapbox map and style render.
4. Current-location marker is correct.
5. Authoritative target marker is correct.
6. WFA recommendation marker remains preview-only.
7. Current-location focus works.
8. Authoritative-target focus works.
9. Recommendation bounds fit works.
10. Pick-on-map camera idle returns the correct coordinate.
11. Search works with current-location proximity.
12. Search works without proximity.
13. Suggestion/detail resolution preserves identity.
14. Reverse geocode shows resolved address.
15. Coordinate-only result is not reported as resolved address.
16. Existing geofence registration/restoration works.
17. Attendance request uses only the authoritative target.

For each row record:

```text
device/API
commit SHA
setup
expected
observed
pass/fail
evidence path
```

Unavailable runtime rows remain `Needs Verification`.

### Task 20: Final scope and security review

**Steps:**

1. Verify no secret/token/config value was added to docs or logs.
2. Verify no broad `NetworkModule` or dependency change occurred beyond
   required capability wiring.
3. Verify no Google Maps implementation or runtime switch exists.
4. Verify no Work Mode visual redesign entered the diff.
5. Verify the main checkout's local network changes were never staged.
6. Review the full diff against INF-140 acceptance criteria.

**Commands:**

```powershell
git diff origin/develop --stat
git diff origin/develop -- app/src/main/java/com/example/infinite_track/di/NetworkModule.kt
git diff --check origin/develop
```

### Task 21: Commit evidence and request review

**Commit:**

```text
Record INF-140 migration evidence
```

Request review focused on:

```text
provider leaks
capability boundaries
cancellation
Mapbox adapter lifecycle
semantic camera effects
preview versus authoritative target
geofence compatibility
runtime evidence
Google migration and rollback gates
```

## Final Acceptance Gate

- [ ] All real consumers use split capabilities.
- [ ] `GeoCoordinate` is the shared project coordinate.
- [ ] Domain and ViewModels contain no provider SDK types.
- [ ] No final coordinate contract uses `Pair<Double, Double>`.
- [ ] Current location includes accuracy and capture time.
- [ ] Search suggestion and details are separate.
- [ ] Reverse geocode uses resolved/coordinate-only/failure results.
- [ ] Mapbox execution is isolated in provider adapters.
- [ ] Camera work is a semantic one-time effect.
- [ ] Marker roles distinguish preview from truth.
- [ ] Marker clicks cannot silently replace `SelectedTargetLocation`.
- [ ] Approved WFA booking is the WFA target authority.
- [ ] Geofence behavior is preserved.
- [ ] Existing Mapbox runtime behavior is verified.
- [ ] Unit, build, lint, instrumentation, and runtime evidence are recorded.
- [ ] ADR and rollback checkpoints exist.
- [ ] Google migration has not started.

Do not move INF-140 to Done, unblock INF-238, or begin Google Maps migration
until every non-runtime item passes and every required runtime item is either
verified or explicitly marked `Needs Verification`.
