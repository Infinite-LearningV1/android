# INF-140 — Provider-Neutral Map and Location Architecture

Date: 2026-07-23

Linear: `INF-140`

GitHub: `#94`

Branch: `codex/inf-140-provider-neutral-location`

Worktree: `E:\skrisi\android\.worktrees\inf-140-provider-neutral-location`

Base: `origin/develop` at `6f1c171`

Status: Draft revision 2 for operator approval

## Summary

INF-140 extracts the existing Attendance map and location flow behind
project-owned, capability-based contracts and completes the runtime cutover
from Mapbox to Google Maps Platform.

The design does not introduce a universal `MapRepository` or a runtime provider
switch. It separates six concerns:

```text
Geographic primitives
Current device location
Place discovery
Address resolution
Map presentation
Authoritative Attendance target
```

The migration is complete only when real consumers use these contracts, Google
Maps/Places are the active implementations, and verified Mapbox code and
dependencies have been removed. Creating unused interfaces beside either
provider flow is not sufficient.

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
10. Replace Mapbox rendering with Google Maps Compose.
11. Replace Mapbox place discovery with the Google Places SDK.
12. Preserve current user-facing map, marker, camera, search, address, and
    current-location behavior through the cutover.
13. Remove Mapbox code, dependencies, token wiring, and provider DTOs only
    after Google runtime verification passes.
14. Move the Google Maps API key from the tracked Manifest literal into
    ignored `local.properties`/CI environment injection.
15. Leave explicit rollback checkpoints after every implementation phase.

## Non-goals

- No Work Mode or Target Location visual redesign.
- No Attendance Action Hub implementation.
- No geofence lifecycle rewrite.
- No backend Attendance, WFA, or booking contract changes.
- No permanent provider registry or provider selection UI.
- No generic provider framework intended to support both providers forever.
- No marker click that silently replaces the authoritative Attendance target.
- No migration of transport DTO latitude/longitude fields when the backend
  contract requires scalar coordinates.
- No broad navigation refactor.

## Operator Scope Revision

On 2026-07-23 the operator expanded INF-140 from architecture extraction before
Google migration to architecture extraction plus the Google runtime migration.
This revision supersedes the earlier draft's Mapbox-retention non-goals.

Current repository evidence:

```text
Google Maps Compose dependency       → present
Play Services Maps dependency        → present
Google Places SDK dependency         → present
Google Maps API metadata             → present as a tracked literal
Google Maps Compose production code  → absent from develop
historical MapPicker/MapsScreen       → recoverable from repository history
```

The historical implementation is reference material, not code to restore
unchanged. It directly owned permission requests, `Geocoder`, `LatLng`,
camera state, Places initialization, and local UI state in composables. It also
contained hardcoded key usage in history. The new implementation reuses the
installed dependencies and proven interaction concepts while following the
approved capability, Route, ViewModel, and security boundaries.

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

The project already declares Google Maps Compose, Play Services Maps, Google
Places, and Mapbox dependencies. The current `develop` source contains no active
`GoogleMap`, `LatLng`, or `CameraPositionState` production implementation.

The tracked Manifest contains a literal Google Maps API key. The exact value is
treated as sensitive and must never be copied into source, docs, logs, tests, or
tool output. Moving it to `local.properties` does not remove it from Git
history, so the existing key must also be rotated and restricted in Google
Cloud Console.

## Locked Architecture

The selected architecture is capability-based:

```text
Compose Screen
→ ViewModel state + semantic effects
→ Use case
→ capability repository
→ provider/platform data source
→ Google Maps / Google Places / Android Geocoder / Play Services implementation
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
9. The migration seam is temporary and branch-local. There is no runtime
   provider switch.
10. Google becomes the only active map/place provider after verification.
11. Mapbox remains only as a rollback checkpoint until the Google runtime gate
    passes, then its code and dependencies are removed.

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

### Google Places implementation

`GooglePlacesDiscoveryRepository` uses the installed Google Places SDK:

```text
FindAutocompletePredictionsRequest
→ AutocompletePrediction
→ PlaceSuggestion

FetchPlaceRequest(placeId)
→ Place
→ PlaceDetails
```

Rules:

1. One `AutocompleteSessionToken` scopes a user search session.
2. Predictions expose provider identity only as the opaque `placeId`.
3. The UI receives project-owned suggestion text and optional distance only.
4. Selection always calls `resolvePlace(placeId)` before coordinates enter
   preview state.
5. Session tokens are renewed after selection, cancellation, or a completed
   search flow.
6. API status/exception details map to typed failures; raw provider messages do
   not enter UI state.
7. No Places widget launches itself from a composable.

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
AndroidGeocoderDataSource
→ AndroidAddressMapper
→ AndroidGeocoderAddressResolver
```

The Android-restricted Maps/Places key is not sent to a direct Google Geocoding
REST endpoint. If product requirements later require Google Geocoding API
guarantees, that must use a separately reviewed secure backend or restricted
service credential.

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
→ GoogleAttendanceMap maps it to CameraUpdateFactory / CameraPositionState
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
├── GooglePlacesDataSource.kt
├── GooglePlacesMapper.kt
└── GooglePlacesDiscoveryRepository.kt

data/location/address/
├── AndroidGeocoderDataSource.kt
├── AndroidAddressMapper.kt
└── AndroidGeocoderAddressResolver.kt

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
└── GoogleAttendanceMap.kt
```

Existing package spelling such as `data/soucre` is not expanded further.
INF-140 may place new location adapters in the clean `data/location` package
without broad unrelated package renames.

## Migration Strategy

The implementation uses five bounded phases.

### Phase 1 — Security/configuration, characterization, and primitives

- Replace the tracked Manifest key literal with a Gradle placeholder.
- Read `GOOGLE_MAPS_API_KEY` from ignored `local.properties`, with a CI
  environment fallback.
- Rotate and Android-restrict the previously tracked key.
- Record the exact provider leak inventory.
- Add characterization tests for current target selection, search mapping,
  reverse-geocode fallbacks, map effects, and geofence persistence.
- Add `GeoCoordinate`, bounds, distance, and validation.
- Migrate domain geographic models through compatibility mappers.
- Do not change Mapbox rendering behavior.

Rollback: retain the previous build commit for source rollback, but never restore
the literal key. Key rotation is not rolled back.

### Phase 2 — Split capabilities and implement Google Places/address

- Add current-location, place-discovery, and address-resolution contracts.
- Add Play Services current location, Google Places discovery/details, and
  Android Geocoder address implementations.
- Replace `LocationRepository` consumers one vertical slice at a time.
- Remove Fused Location access from domain use cases.
- Preserve search UX through Google autocomplete/detail resolution.

Rollback: Hilt can temporarily restore legacy bindings from the prior phase
commit while each consumer is migrated. The old `LocationRepository` and
Mapbox search code are deleted only after Google Places runtime evidence passes.

### Phase 3 — Google Maps Compose presentation

- Add map UI models, marker roles, map events, and semantic camera effects.
- Implement `GoogleAttendanceMap` with Maps Compose.
- Map project markers to Google `Marker`/`Circle` and semantic camera effects
  to `CameraUpdateFactory`.
- Replace `MapAnimationTarget` state with `SharedFlow<MapCameraEffect>`.
- Remove Mapbox types from ViewModel and public screen/map contracts.

Rollback: keep a checkpoint immediately before changing the public
`AttendanceMap` contract. Revert the Google renderer phase as a unit if runtime
map, marker, camera, or pick-on-map evidence regresses.

### Phase 4 — Authoritative target, geofence, and Google cutover

- Project authoritative targets into map markers.
- Separate WFA preview selection from approved WFA target ownership.
- Remove duplicated Attendance target/marker fields.
- Migrate geofence candidates and persistence boundaries to `GeoCoordinate`.
- Keep geofence lifecycle unchanged.
- Make Google rendering and Google Places the only active runtime paths.
- Run the Google parity matrix before deleting Mapbox.

Rollback: retain characterization tests for the old target resolver and
DataStore fixtures. The Git checkpoint before the Google cutover is the provider
rollback mechanism; no runtime provider flag is added.

### Phase 5 — Remove Mapbox, evidence, and closure

- Delete legacy `LocationRepository`, `LocationResult`, final `Pair` contracts,
  `MapAnimationTarget`, and obsolete MapUtils paths.
- Delete Mapbox rendering/search/address code, DTOs, Retrofit service, token
  wiring, Gradle dependencies, and Manifest metadata.
- Prove domain/ViewModel provider scans are empty.
- Run unit, compile, build, instrumentation, and runtime matrix.
- Write ADR and runtime evidence.
- Record the Google cutover and Mapbox-removal evidence.

Rollback: Mapbox removal happens only after the full Google runtime gate. If the
gate fails, stop in Phase 4 and keep Mapbox code available on the pre-cutover Git
checkpoint. Do not keep both providers as a shipped runtime option.

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
Google autocomplete prediction → suggestion
Google Place → place details
Android Geocoder → resolved/coordinate-only/failure
Places session-token lifecycle
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
Google Maps SDK initialization
current precise location success
location unavailable/failure recovery
Google base map rendering
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
Mapbox code is unreachable before deletion
```

Compile-only evidence cannot mark runtime-sensitive acceptance items Done.

## Security and Privacy

- Do not log Mapbox tokens, request authorization, or user-identifying data.
- Do not log the Google Maps API key, Places requests, or exact user
  coordinates.
- Avoid verbose production logs containing exact current coordinates.
- No coordinate or address is persisted beyond existing product requirements.
- Places session tokens and prediction results are memory-only and scoped to the
  active search flow.

### API key injection

Tracked source contains only:

```xml
android:value="${GOOGLE_MAPS_API_KEY}"
```

Local developer configuration:

```properties
GOOGLE_MAPS_API_KEY=<local secret>
```

Gradle reads `local.properties` and optionally the CI environment variable of
the same name, then injects:

```text
manifestPlaceholders["GOOGLE_MAPS_API_KEY"]
BuildConfig.GOOGLE_MAPS_API_KEY
```

The Manifest placeholder initializes Maps SDK. The BuildConfig value initializes
Places once at the application/data boundary, never inside a composable.

An Android API key is still extractable from an APK. Security comes from Google
Cloud restrictions for the application ID plus signing-certificate SHA
fingerprints, enabled-API restrictions, quotas, and rotation—not from treating
the packaged value as an unrecoverable secret.

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
- Google Places session-token and detail-resolution boundary;
- semantic camera-effect ownership;
- authoritative target invariant;
- Google cutover, key rotation, and Mapbox removal gates.

## Mapbox Removal Gate

Mapbox code and dependencies may be removed only when:

1. The tracked Google key literal has been removed and rotated.
2. Domain and ViewModels contain no provider SDK types.
3. All real location consumers use split capabilities.
4. Google Maps renders every required marker/circle and camera behavior.
5. Google Places search and detail resolution pass runtime verification.
6. `SelectedTargetLocation` is the only Attendance target truth.
7. Address and current-location behavior pass typed-result tests.
8. Unit, build, lint, and instrumentation gates pass.
9. Device evidence proves map, marker, camera, search, address,
   current-location, Attendance submission, and geofence behavior.
10. No required Google runtime row remains `Needs Verification`.

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
- [ ] Google runtime behavior matches the required Mapbox-era product
      capabilities before Mapbox removal.
- [ ] Google Maps Compose is the active and verified renderer.
- [ ] Google Places SDK is the active and verified discovery/details provider.
- [ ] Google Maps key is injected from ignored local/CI configuration.
- [ ] Previously tracked Google keys are rotated and Android-restricted.
- [ ] Mapbox code, dependencies, service, DTOs, token wiring, and Manifest
      metadata are removed after the runtime gate.
- [ ] Mapper, repository, camera, target projection, cancellation, and
      persistence compatibility tests pass.
- [ ] Build, lint, instrumentation, and runtime evidence are recorded.
- [ ] ADR, Google cutover, Mapbox removal, and rollback checkpoints are
      documented.

## Approval Gate

No production implementation starts until this spec and its implementation plan
are self-reviewed, committed on the isolated branch, and approved by the
operator.
