# INF-140 Provider Leak Baseline

Date: 2026-07-23

Scope: Android `app` module before the provider-neutral migration.

## Current provider boundaries

- Four production Kotlin files import Mapbox SDK types directly:
  `MapUtils.kt`, the legacy `AttendanceMap.kt`, `AttendanceScreen.kt`, and
  `AttendanceViewModel.kt`.
- No production Kotlin file imports Google Maps SDK or Maps Compose types yet.
- `AttendanceViewModel` owns Mapbox `Point` values through
  `MapAnimationTarget`, so provider state currently crosses into presentation
  orchestration.
- The legacy map composable exposes `MapView` through `onMapReady`, causing the
  screen to manage renderer lifecycle and camera commands imperatively.

## Location capability leaks

- Five source files reference `FusedLocationProviderClient`, including the
  domain-layer `GetCurrentCoordinatesUseCase`.
- `LocationRepository` combines current-device location, Mapbox reverse
  geocoding, search, and place resolution in one contract.
- `Pair<Double, Double>` remains in the current-location repository/use case,
  AttendancePreference compatibility plumbing, and an instrumentation fake.
- Backend DTO and request boundaries use scalar latitude/longitude and remain
  valid transport boundaries.

## Attendance map and target duplication

`AttendanceScreenState` currently carries all of the following:

- `selectedTargetLocation`;
- `targetLocationMarker`;
- `selectedWfaLocation` and `selectedWfaMarkerInfo`;
- `selectedMarkerInfo`;
- `pickedLocation`;
- persistent `mapAnimationTarget` commands.

The migration must converge these onto an authoritative selected target,
preview identity, provider-neutral marker models, and one-shot camera effects.

## Installed Google baseline

- Maps Compose `2.11.0`;
- Play Services Maps `18.1.0`;
- Places SDK `2.6.0`;
- Play Services Location `21.3.0`.

The repository previously contained Google map experiments, but there is no
current production Google renderer to restore unchanged. The migration will
therefore introduce a narrow adapter around the installed baseline.

## Verification baseline

- Existing unit coverage protects target resolution, eligibility, request
  construction, Attendance runtime preference cleanup, permissions, and
  transient feedback.
- Existing instrumentation coverage includes Attendance face-result recovery.
- There is no automated renderer parity coverage. Google base-map rendering,
  attribution, camera behavior, and device location still require a
  Play-enabled emulator or physical device and remain `Needs Verification`
  until runtime evidence is captured.
