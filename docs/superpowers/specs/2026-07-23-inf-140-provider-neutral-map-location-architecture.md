# INF-140 — Provider-Neutral Map and Location Architecture

Date: 2026-07-23

Linear: `INF-140`

GitHub: `#94`

Branch: `codex/inf-140-provider-neutral-location`

Worktree: `E:\skrisi\android\.worktrees\inf-140-provider-neutral-location`

Base: `origin/develop` at `6f1c171`

Status: Draft for operator approval

## Summary

INF-140 extracts the existing Attendance map and location flow behind
project-owned, capability-based contracts while Mapbox remains the active
provider.

The design does not introduce a universal `MapRepository`, a runtime provider
switch, or Google Maps UI. It separates six concerns:

```text
Geographic primitives
Current device location
Place discovery
Address resolution
Map presentation
Authoritative Attendance target
```

The extraction is complete only when real consumers use these contracts.
Creating unused interfaces beside the legacy flow is not sufficient.

## Goals

1. Use `GeoCoordinate` as the shared coordinate primitive across Attendance,
   WFA, Target Location, Geofence, search, and map presentation.
2. Remove Mapbox and Google Maps SDK types from domain and ViewModel code.
3. Separate current location, place discovery, and address resolution into
   independent capability contracts.
4. Expose current-location accuracy and capture time instead of returning only
   latitude and longitude.
5. Separate search suggestions from resolved place details.
6. Represent reverse-geocode outcomes as resolved address, coordinate-only, or
   typed failure.
7. Move Mapbox camera, marker, annotation, and lifecycle execution behind the
   presentation map adapter.
8. Make camera requests semantic one-time effects rather than SDK commands
   stored in persistent screen state.
9. Keep `SelectedTargetLocation` as the only authoritative Attendance target.
10. Preserve current Mapbox rendering, search, reverse-geocode, camera, and
    current-location behavior through the migration seam.
11. Leave explicit rollback checkpoints after every implementation phase.

## Non-goals

- No Google Maps rendering or Places implementation.
- No Google Maps visual parity work.
- No Work Mode or Target Location visual redesign.
- No Attendance Action Hub implementation.
- No geofence lifecycle rewrite.
- No backend Attendance, WFA, or booking contract changes.
- No removal of Mapbox dependencies in this issue.
- No permanent provider registry or provider selection UI.
- No generic provider framework intended to support both providers forever.
- No marker click that silently replaces the authoritative Attendance target.
- No migration of transport DTO latitude/longitude fields when the backend
  contract requires scalar coordinates.
- No broad navigation refactor.

## Current Repository Findings

### Provider leaks

Production Mapbox imports currently exist in:

```text
presentation/components/maps/AttendanceMap.kt
presentation/screen/attendance/AttendanceScreen.kt
presentation/screen/attendance/AttendanceViewModel.kt
utils/MapUtils.kt
```

`AttendanceViewModel` defines:

```kotlin
sealed class MapAnimationTarget {
    data class AnimateToLocation(
        val point: Point,
        val zoomLevel: Double
    ) : MapAnimationTarget()

    data class AnimateToFitBounds(
        val points: List<Point>
    ) : MapAnimationTarget()

    object ShowLocationError : MapAnimationTarget()
}
```

This makes the ViewModel depend on Mapbox `Point` and stores one-time camera
commands inside `AttendanceScreenState`.

`AttendanceMap` exposes Mapbox types in its public Compose contract:

```text
currentUserLocation: Point?
onMapReady: (MapView) -> Unit
onCameraIdle: (Point) -> Unit
```

`AttendanceScreen` owns `MapView`, `CameraOptions`, `flyTo`, bounds conversion,
and camera execution. `MapUtils` owns provider annotations and provider marker
identity.

### Capability coupling

`LocationRepository` currently combines:

```text
current address
current coordinates
place search
reverse geocoding
```

`LocationRepositoryImpl` combines:

```text
FusedLocationProviderClient
Mapbox Retrofit service
Mapbox DTO mapping
address fallback copy
coroutine bridging
current-location policy
```

`GetCurrentCoordinatesUseCase` bypasses the repository and injects
`FusedLocationProviderClient` directly. It also conflates two different
concepts:

```text
fresh device location
saved WFH location
```

A saved WFH coordinate is a target/profile value, not a current-device-location
result.

### Untyped outcomes

The current flow uses:

```text
Result<Pair<Double, Double>>
Result<String>
Result<LocationResult>
```

It can return technical copy such as `Lat: ..., Lng: ...` as a successful
address. Cancellation, unavailable location, missing feature, provider failure,
and coordinate-only fallback are not structurally distinct.

### Overlapping Attendance state

`AttendanceScreenState` currently contains overlapping business, preview,
projection, and execution state:

```text
targetLocation
wfoLocation
wfhLocation
selectedTargetLocation
targetLocationMarker
selectedMarkerInfo
pickedLocation
selectedWfaLocation
selectedWfaMarkerInfo
currentUserLatitude/currentUserLongitude
mapAnimationTarget
```

The duplication permits invalid combinations and makes target ownership hard to
audit.

### Authoritative-target violation

`onWfaMarkerClicked` currently:

```text
sets selectedWfaLocation
→ calls resolveAndApplyTargetForMode(WFA)
→ updates selectedTargetLocation
```

A WFA recommendation marker is discovery/preview state. It must not become
Attendance truth until the relevant product action establishes an approved WFA
booking or another explicit authoritative contract.

### Existing dependency state

The project already declares both Google Maps and Mapbox dependencies. INF-140
does not use this as permission to start Google rendering. Dependency cleanup is
deferred until the later provider migration is complete.

## Locked Architecture

The selected architecture is capability-based:

```text
Compose Screen
→ ViewModel state + semantic effects
→ Use case
→ capability repository
→ provider/platform data source
→ current Mapbox or Play Services implementation
```

Rules:

1. Domain and ViewModel code contain no Mapbox or Google Maps SDK models.
2. Android `Location` is converted at the data boundary.
3. Retrofit/Mapbox DTOs remain inside provider data sources and mappers.
4. Compose Screen renders immutable project-owned state and emits typed events.
5. ViewModel specifies what the map should do, not how Mapbox performs it.
6. The map adapter owns `MapView`, style loading, annotations, SDK camera calls,
   SDK listeners, and their cleanup.
7. Discovery and preview state never mutate the authoritative target implicitly.
8. Backend identity and coordinates remain authoritative; address text is
   supporting content.
9. The migration seam is temporary and compile-time. There is no runtime
   provider switch.

## Shared Geographic Primitives

Create project-owned types:

```kotlin
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

data class GeoBounds(
    val southwest: GeoCoordinate,
    val northeast: GeoCoordinate
)

@JvmInline
value class DistanceMeters(
    val value: Double
)
```

Validation:

```text
latitude  ∈ [-90, 90]
longitude ∈ [-180, 180]
distance  >= 0
```

Construction from untrusted provider or transport values goes through mapper
validation and returns typed failure. The primary data classes remain simple
values; they do not depend on Android, Compose, Mapbox, or Google SDKs.

### Model migration rule

Domain models that own a geographic position use:

```kotlin
val coordinate: GeoCoordinate
```

This includes:

```text
Attendance Location
SelectedTargetLocation projection
WFA recommendation/detail
current location
place details
resolved address
geofence candidate/persisted value
```

Transport request/response DTOs may retain `latitude` and `longitude` scalars.
Data mappers translate between DTO scalar fields and `GeoCoordinate`.

Temporary compatibility accessors may be used inside an implementation phase,
but no final domain or presentation contract returns `Pair<Double, Double>`.

## Capability 1 — Current Device Location

Contract:

```kotlin
interface CurrentLocationRepository {
    suspend fun getCurrentLocation(
        accuracy: LocationAccuracy
    ): CurrentLocationResult
}
```

Models:

```kotlin
enum class LocationAccuracy {
    PRECISE,
    BALANCED
}

data class CurrentLocation(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Float?,
    val capturedAtEpochMillis: Long
)

sealed interface CurrentLocationResult {
    data class Available(
        val location: CurrentLocation
    ) : CurrentLocationResult

    data class Unavailable(
        val reason: CurrentLocationUnavailableReason
    ) : CurrentLocationResult

    data class Failed(
        val reason: CurrentLocationFailure
    ) : CurrentLocationResult
}
```

Minimum typed reasons distinguish:

```text
permission unavailable
device location disabled
provider returned null
timeout/provider unavailable
invalid coordinate
unknown
```

Coroutine cancellation is propagated and is not mapped to a user-facing
failure.

Implementation:

```text
PlayServicesCurrentLocationDataSource
→ Android Location mapper
→ CurrentLocationRepositoryImpl
```

`FusedLocationProviderClient` remains the implementation. It does not enter a
domain use case.

The existing saved-WFH fallback is removed from the current-location capability.
Consumers that require a fallback express that policy in a dedicated use case
using an authoritative target/profile source. Search proximity may continue
without proximity when current location is unavailable.

## Capability 2 — Place Discovery

Contract:

```kotlin
interface PlaceDiscoveryRepository {
    suspend fun search(
        request: PlaceSearchRequest
    ): PlaceSearchResult

    suspend fun resolvePlace(
        placeId: String
    ): PlaceDetailsResult
}
```

Models:

```kotlin
data class PlaceSearchRequest(
    val query: String,
    val proximity: GeoCoordinate?,
    val limit: Int
)

data class PlaceSuggestion(
    val id: String,
    val primaryText: String,
    val secondaryText: String?,
    val distanceMeters: Double?
)

data class PlaceDetails(
    val id: String,
    val name: String,
    val formattedAddress: String?,
    val coordinate: GeoCoordinate
)
```

Search suggestions do not expose provider DTOs or automatically become place
details.

### Mapbox transition behavior

The current Mapbox forward endpoint already returns detail-like features rather
than a separate suggest/retrieve exchange. To preserve runtime behavior without
changing provider endpoints during INF-140:

1. `MapboxPlaceDiscoveryDataSource` fetches the existing forward results.
2. `MapboxPlaceDiscoveryMapper` creates `PlaceSuggestion` values.
3. A feature-local, in-memory candidate store retains the mapped `PlaceDetails`
   by stable provider feature ID for the active search flow.
4. `resolvePlace(placeId)` retrieves and validates the corresponding detail.
5. Missing or expired candidate identity returns a typed result and prompts a
   new search; it never guesses by display text.

The candidate store is a temporary Mapbox adapter detail, not a domain cache and
not a permanent provider framework. The later Google Places implementation can
replace it with autocomplete/details calls without changing consumers.

## Capability 3 — Address Resolution

Contract:

```kotlin
interface AddressResolver {
    suspend fun reverseGeocode(
        coordinate: GeoCoordinate
    ): AddressResolutionResult
}
```

Models:

```kotlin
data class ResolvedAddress(
    val formattedAddress: String,
    val locality: String?,
    val administrativeArea: String?,
    val countryCode: String?,
    val coordinate: GeoCoordinate
)

sealed interface AddressResolutionResult {
    data class Resolved(
        val address: ResolvedAddress
    ) : AddressResolutionResult

    data class CoordinateOnly(
        val coordinate: GeoCoordinate
    ) : AddressResolutionResult

    data class Failed(
        val reason: AddressResolutionFailure
    ) : AddressResolutionResult
}
```

Rules:

- An empty provider feature list becomes `CoordinateOnly`.
- A provider/network exception becomes typed `Failed`.
- A formatted coordinate string is presentation copy derived from
  `CoordinateOnly`; it is never a successful resolved address.
- Raw exception messages do not enter UI state.
- Cancellation propagates.

Implementation:

```text
MapboxAddressDataSource
→ MapboxAddressMapper
→ MapboxAddressResolver
```

## Capability 4 — Map Presentation

Map rendering remains presentation code. It is not a domain repository.

Provider-neutral state:

```kotlin
data class MapUiState(
    val markers: List<MapMarkerUiModel>,
    val currentLocation: CurrentLocationUiModel?,
    val interactionMode: MapInteractionMode
)

data class MapMarkerUiModel(
    val id: String,
    val coordinate: GeoCoordinate,
    val role: MapMarkerRole,
    val title: String?,
    val supportingText: String?,
    val radiusMeters: DistanceMeters?,
    val isSelected: Boolean
)

enum class MapMarkerRole {
    CURRENT_LOCATION,
    AUTHORITATIVE_TARGET,
    WFA_RECOMMENDATION,
    SEARCH_PREVIEW
}

enum class MapInteractionMode {
    VIEW_ONLY,
    PICK_LOCATION
}
```

Provider-neutral events:

```kotlin
sealed interface AttendanceMapEvent {
    data class MarkerClicked(
        val markerId: String,
        val role: MapMarkerRole
    ) : AttendanceMapEvent

    data class CameraIdle(
        val center: GeoCoordinate
    ) : AttendanceMapEvent
}
```

The public Attendance map contract does not expose `MapView`, `Point`,
`CameraOptions`, annotation objects, or Google `LatLng`.

### Semantic camera effects

Camera work is a one-time effect:

```kotlin
sealed interface MapCameraEffect {
    data class FocusCoordinate(
        val coordinate: GeoCoordinate,
        val zoom: MapZoomLevel
    ) : MapCameraEffect

    data class FocusAuthoritativeTarget(
        val targetId: String,
        val coordinate: GeoCoordinate
    ) : MapCameraEffect

    data class FitCoordinates(
        val coordinates: List<GeoCoordinate>,
        val padding: MapPadding
    ) : MapCameraEffect

    data object FollowCurrentLocation : MapCameraEffect
}
```

`AttendanceViewModel` emits camera effects through a buffered
`SharedFlow<MapCameraEffect>`. Camera effects are not fields in
`AttendanceScreenState`.

Execution:

```text
AttendanceViewModel emits MapCameraEffect
→ Attendance Route collects the one-time effect
→ AttendanceMap passes it to the active adapter
→ MapboxAttendanceMapAdapter maps it to CameraOptions / flyTo / bounds
```

The adapter owns SDK lifecycle, style readiness, annotation managers, camera
listeners, delayed callbacks, and cleanup. Compose effect keys use stable
project-owned values. SDK callbacks are updated without restarting long-lived
effects unnecessarily.

## Capability 5 — Authoritative Attendance Target

Ownership:

```text
SelectedTargetLocation
→ business truth used by eligibility and submission

MapMarkerUiModel
→ visual projection only

PlaceSuggestion / PlaceDetails
→ discovery and preview

WfaRecommendation
→ recommendation preview

Approved WFA booking
→ authoritative WFA Attendance target
```

Required projection:

```text
SelectedTargetLocation
→ TargetLocationMapMapper
→ MapMarkerUiModel(role = AUTHORITATIVE_TARGET)
```

Forbidden direction:

```text
marker click
→ selectedTargetLocation replaced
```

WFA marker clicks select a preview marker and show preview content only.
Navigation into the booking flow is explicit. The approved booking resolver,
not the preview marker, establishes the WFA target used by Attendance.

`SelectedTargetLocation` remains the only target accepted by work-mode
eligibility and attendance request construction. Legacy `targetLocation` and
`targetLocationMarker` fields are removed after the authoritative projection is
fully migrated.

## Capability 6 — Geofence Coordinate Consumption

INF-140 does not redesign geofence lifecycle. It only replaces coordinate
plumbing:

```text
ReminderGeofenceCandidate.coordinate: GeoCoordinate
ReminderGeofence.coordinate: GeoCoordinate
Geofence registration mapper
→ latitude/longitude SDK scalars at the final Play Services boundary
```

Persisted DataStore scalar keys remain compatible. Serialization/deserialization
maps them to `GeoCoordinate`. Existing request IDs, radius rules, registration
behavior, notification cooldowns, and active-monitoring semantics remain
unchanged.

## Package Direction

```text
domain/model/location/
├── GeoCoordinate.kt
├── GeoBounds.kt
├── DistanceMeters.kt
├── CurrentLocation.kt
├── PlaceSearch.kt
├── PlaceDetails.kt
└── ResolvedAddress.kt

domain/repository/location/
├── CurrentLocationRepository.kt
├── PlaceDiscoveryRepository.kt
└── AddressResolver.kt

domain/use_case/location/
├── GetCurrentLocationUseCase.kt
├── SearchPlacesUseCase.kt
├── ResolvePlaceDetailsUseCase.kt
└── ReverseGeocodeUseCase.kt

data/location/current/
├── PlayServicesCurrentLocationDataSource.kt
├── AndroidLocationMapper.kt
└── CurrentLocationRepositoryImpl.kt

data/location/discovery/
├── MapboxPlaceDiscoveryDataSource.kt
├── MapboxPlaceDiscoveryMapper.kt
├── MapboxPlaceCandidateStore.kt
└── MapboxPlaceDiscoveryRepository.kt

data/location/address/
├── MapboxAddressDataSource.kt
├── MapboxAddressMapper.kt
└── MapboxAddressResolver.kt

presentation/map/model/
├── MapUiState.kt
├── MapMarkerUiModel.kt
├── MapCameraEffect.kt
└── AttendanceMapEvent.kt

presentation/map/mapper/
├── TargetLocationMapMapper.kt
├── WfaRecommendationMapMapper.kt
└── CurrentLocationUiMapper.kt

presentation/map/adapter/
├── AttendanceMap.kt
└── MapboxAttendanceMapAdapter.kt
```

Existing package spelling such as `data/soucre` is not expanded further.
INF-140 may place new location adapters in the clean `data/location` package
without broad unrelated package renames.

## Migration Strategy

The implementation uses five bounded phases.

### Phase 1 — Characterization and geographic primitives

- Record the exact provider leak inventory.
- Add characterization tests for current target selection, search mapping,
  reverse-geocode fallbacks, map effects, and geofence persistence.
- Add `GeoCoordinate`, bounds, distance, and validation.
- Migrate domain geographic models through compatibility mappers.
- Do not change Mapbox rendering behavior.

Rollback: revert primitive/model commits; no provider execution changes yet.

### Phase 2 — Split location capabilities

- Add current-location, place-discovery, and address-resolution contracts.
- Add Play Services and Mapbox implementations.
- Replace `LocationRepository` consumers one vertical slice at a time.
- Remove Fused Location access from domain use cases.
- Preserve current search endpoint behavior through the Mapbox candidate store.

Rollback: Hilt can temporarily bind legacy adapters while each consumer is
migrated. The old `LocationRepository` is deleted only after the reference scan
is empty.

### Phase 3 — Provider-neutral map presentation

- Add map UI models, marker roles, map events, and semantic camera effects.
- Move Mapbox lifecycle, annotations, and camera calls to the adapter.
- Replace `MapAnimationTarget` state with `SharedFlow<MapCameraEffect>`.
- Remove Mapbox types from ViewModel and public screen/map contracts.

Rollback: keep a checkpoint immediately before changing the public
`AttendanceMap` contract. Revert the phase as a unit if runtime map, marker,
camera, or pick-on-map evidence regresses.

### Phase 4 — Authoritative target and geofence consumers

- Project authoritative targets into map markers.
- Separate WFA preview selection from approved WFA target ownership.
- Remove duplicated Attendance target/marker fields.
- Migrate geofence candidates and persistence boundaries to `GeoCoordinate`.
- Keep geofence lifecycle unchanged.

Rollback: retain characterization tests for the old target resolver and
DataStore fixtures. Do not delete compatibility mapping until device evidence
passes.

### Phase 5 — Cleanup, evidence, and migration gate

- Delete legacy `LocationRepository`, `LocationResult`, final `Pair` contracts,
  `MapAnimationTarget`, and obsolete MapUtils paths.
- Prove domain/ViewModel provider scans are empty.
- Run unit, compile, build, instrumentation, and runtime matrix.
- Write ADR and runtime evidence.
- Record the exact cleanup gate for the later Google Maps issue.

Rollback: Mapbox dependencies, Retrofit service, and provider adapter remain
present and operational. The next provider migration does not start if this
phase is `Needs Verification`.

## State and Compose Ownership

Persistent ViewModel state uses immutable project-owned models in `StateFlow`.
One-time camera and feedback work uses `SharedFlow`.

```text
StateFlow:
map markers
selected preview identity
authoritative target projection
current location presentation
interaction mode
loading/error content

SharedFlow:
camera focus/fit/follow
navigation
transient feedback
```

The map composable owns only SDK-instance lifecycle state that cannot live in
the ViewModel. Screen state is hoisted only as high as required. Long-running
Compose effects use stable keys and latest callback references; SDK listener
cleanup is explicit.

## Error and Cancellation Policy

- Cancellation propagates through every suspend boundary.
- Raw provider exceptions are mapped to typed failures and logged without
  secrets or personal coordinate payloads.
- Invalid coordinates fail at mapper boundaries.
- Current-location unavailability is distinct from provider failure.
- Search can continue without proximity.
- Reverse geocoding may return coordinate-only without pretending an address
  was resolved.
- A failed camera effect does not alter the authoritative target.
- Map adapter failure produces typed presentation feedback while preserving the
  last trustworthy screen content.
- Provider token and network configuration remain out of logs and docs.

## Testing Strategy

### Unit tests

Required tests include:

```text
GeoCoordinate validation and mapping
Android Location → CurrentLocation
Mapbox search DTO → suggestion/detail
Mapbox reverse-geocode DTO → resolved/coordinate-only/failure
search candidate identity and expiry behavior
camera effect focus/fit/follow semantics
SelectedTargetLocation → AUTHORITATIVE_TARGET marker
WFA recommendation → WFA_RECOMMENDATION marker
preview click cannot mutate authoritative target
geofence DataStore scalar compatibility
cancellation propagation
```

### Static architecture checks

Final scans:

```powershell
rg -n "^import com\.mapbox|^import com\.google\.android\.gms\.maps" `
  app/src/main/java/com/example/infinite_track/domain `
  app/src/main/java/com/example/infinite_track/presentation/screen

rg -n "Pair<Double, Double>|MapAnimationTarget|LocationRepository|LocationResult" `
  app/src/main/java app/src/test app/src/androidTest
```

Expected final result:

- no provider SDK imports in domain or ViewModels;
- no final coordinate `Pair`;
- no `MapAnimationTarget`;
- no legacy god `LocationRepository`;
- provider imports remain only in provider data sources/adapters and their
  focused tests.

### Build and instrumentation

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:compileDebugKotlin
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:lint
```

Runtime instrumentation/device verification covers:

```text
current precise location success
location unavailable/failure recovery
Mapbox style and map rendering
authoritative target marker
WFA recommendation markers
preview marker selection
camera focus current location
camera focus target
fit recommendation bounds
pick-on-map camera idle
place search with and without proximity
place detail selection
reverse-geocode resolved and coordinate-only
geofence registration/restoration compatibility
Attendance check-in/out target request
```

Compile-only evidence cannot mark runtime-sensitive acceptance items Done.

## Security and Privacy

- Do not log Mapbox tokens, request authorization, or user-identifying data.
- Avoid verbose production logs containing exact current coordinates.
- Provider Retrofit logging behavior is not expanded in this issue.
- No coordinate or address is persisted beyond existing product requirements.
- Candidate-store contents are memory-only and scoped to the active search
  flow.

## ADR and Documentation

Implementation produces:

```text
docs/adr/ADR-XXX-provider-neutral-map-location-boundary.md
docs/linear-sync/INF-140-runtime-verification.md
```

The ADR records:

- capability-based architecture decision;
- rejection of a generic `MapRepository`;
- rejection of permanent multi-provider/runtime switching;
- temporary Mapbox candidate store;
- semantic camera-effect ownership;
- authoritative target invariant;
- Google migration entry and rollback gates.

## Google Migration Entry Gate

Google rendering/search migration may begin only when:

1. Domain and ViewModels contain no provider SDK types.
2. All real location consumers use the split capabilities.
3. `SelectedTargetLocation` is the only Attendance target truth.
4. Mapbox runs only through isolated data/presentation adapters.
5. Unit and build gates pass.
6. Device evidence proves current Mapbox map, marker, camera, search,
   reverse-geocode, current-location, and geofence behavior.
7. Remaining gaps are explicitly `Needs Verification`.

## Acceptance Checklist

- [ ] `GeoCoordinate` is shared by Attendance, WFA, Target Location, Geofence,
      discovery, address, and presentation map models.
- [ ] Domain and ViewModels contain no Mapbox or Google Maps SDK types.
- [ ] Final coordinate contracts contain no `Pair<Double, Double>`.
- [ ] Current location, place discovery, and address resolution are separate
      interfaces and implementations.
- [ ] Current location exposes accuracy and capture time.
- [ ] Saved WFH target is not represented as current device location.
- [ ] Search suggestion and place details are separate states.
- [ ] Address results distinguish resolved, coordinate-only, and typed failure.
- [ ] Map rendering remains presentation-owned.
- [ ] Camera requests use semantic one-time effects.
- [ ] Marker roles distinguish current, authoritative, recommendation, and
      search-preview markers.
- [ ] Preview selection cannot replace `SelectedTargetLocation`.
- [ ] Approved WFA booking remains the WFA authority.
- [ ] Geofence lifecycle behavior is unchanged while coordinate plumbing is
      migrated.
- [ ] Existing Mapbox runtime behavior remains functional.
- [ ] Mapper, repository, camera, target projection, cancellation, and
      persistence compatibility tests pass.
- [ ] Build, lint, instrumentation, and runtime evidence are recorded.
- [ ] ADR, Google migration gate, and rollback checkpoints are documented.

## Approval Gate

No production implementation starts until this spec and its implementation plan
are self-reviewed, committed on the isolated branch, and approved by the
operator.
