# INF-140 — Google Maps Platform Baseline

Date reviewed: 2026-07-23

This document translates the current official Google Maps Platform guidance into
the configuration baseline for Infinite Track. It is a migration checklist, not
a place to store credentials, certificate fingerprints, project IDs, or exact
user coordinates.

## Official references

- Maps SDK for Android setup:
  https://developers.google.com/maps/documentation/android-sdk/config
- Maps Compose:
  https://developers.google.com/maps/documentation/android-sdk/maps-compose
- API key security:
  https://developers.google.com/maps/api-security-best-practices
- Secrets Gradle Plugin:
  https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin
- Places SDK for Android setup:
  https://developers.google.com/maps/documentation/places/android-sdk/config
- Places SDK versions:
  https://developers.google.com/maps/documentation/places/android-sdk/versions
- Autocomplete (New):
  https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete
- Places session tokens:
  https://developers.google.com/maps/documentation/places/android-sdk/place-session-tokens
- Places App Check:
  https://developers.google.com/maps/documentation/places/android-sdk/app-check
- Map padding and controls:
  https://developers.google.com/maps/documentation/android-sdk/configure-map
- Accessibility:
  https://developers.google.com/maps/documentation/android-sdk/accessibility
- Reporting and monitoring:
  https://developers.google.com/maps/documentation/android-sdk/report-monitor

Recheck the official version pages immediately before implementation. Fixed
versions are required; `+` and `latest` are not allowed.

## Repository compatibility snapshot

| Item | Current repository | Official baseline reviewed | Decision |
|---|---:|---:|---|
| `compileSdk` | 34 | 34 or newer | keep 34 for this migration |
| `minSdk` | 26 | Maps 23+, Places 24+ | compatible |
| Maps Compose | 2.11.0 | 6.12.0 | keep 2.11.0; upgrade separately |
| Play Services Maps | 18.1.0 | 20.0.0 | keep 18.1.0; upgrade separately |
| Places SDK | 2.6.0 | 5.1.1 | keep 2.6.0; upgrade separately |
| Secrets Gradle Plugin | absent | 2.0.1 | add through the version catalog |
| `buildConfig` | enabled | required for Places key access | keep enabled |

The Google SDK upgrade is not a blind version replacement. Run Gradle
`dependencyInsight`, compile tests, and a Play-enabled emulator/device smoke test
before migrating production consumers.

Dependency upgrades are explicitly outside this migration. The implementation
keeps Gradle 8.7, AGP 8.5.2, `compileSdk` 34, and the repository's existing
Google SDK versions. A later dependency-upgrade task can evaluate the current
official baseline behind its own compile and runtime gates.

## Google Cloud project checklist

Required before Google runtime cutover:

1. Billing is enabled for the intended Google Cloud project.
2. Maps SDK for Android is enabled.
3. The existing project already has Places API (Legacy) enabled for the pinned
   Places SDK 2.6.0 runtime; new projects cannot enable this legacy service.
4. The Android credential is restricted to the Infinite Track application ID
   and the appropriate signing-certificate fingerprints.
5. API restrictions allow only Maps SDK for Android and the existing Places
   API (Legacy) service used by this pinned baseline.
6. Development, CI/release, and Play App Signing fingerprints are inventoried
   without committing their values.
7. The previously tracked credential is rotated after the replacement
   credential is verified.
8. Usage, quota, and billing alerts are configured and assigned to a monitored
   owner email.

Prefer distinct development and release credentials with the same
`MAPS_API_KEY` property name in their respective build environments. This
isolates quota, rotation, and incident response without leaking provider
configuration into Kotlin code.

## Local and CI key injection

Use the official Secrets Gradle Plugin instead of adding another manual
`Properties` reader:

```properties
# local.properties — ignored
MAPS_API_KEY=<developer Android-restricted key>
```

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

The resolved `com.google.android.geo.API_KEY` Manifest metadata is the single
runtime key source for both Maps and one-time Places initialization.

Add a tracked defaults file containing only:

```properties
MAPS_API_KEY=missing
```

Configure the plugin with
`defaultPropertiesFileName = "local.defaults.properties"`.

Local and CI builds must replace the default. Release assembly must fail with a
clear message when the default remains, without printing the supplied value.
GitHub Actions writes `MAPS_API_KEY` from an encrypted repository/environment
secret into the ephemeral `local.properties` it already creates, then removes
that file in cleanup.

During the migration, `local.properties` may contain both the Mapbox token and
`MAPS_API_KEY`. Remove the Mapbox path only after the runtime parity gate.

## SDK initialization ownership

- Maps SDK is initialized by Manifest metadata.
- Places SDK 2.6.0 is initialized lazily at the application boundary with
  `Places.initialize`; its provider reads the resolved Manifest metadata and the
  API key is never read by a composable.
- Migration to Places API (New) and `Places.initializeWithNewPlacesApiEnabled`
  is deferred to the separate Places dependency-upgrade task.
- A singleton `PlacesClient` is provided through Hilt.
- Composables never read keys, initialize SDKs, create repositories, request
  permissions, or own search sessions.
- A missing/invalid configuration maps to a typed configuration failure and
  safe UI state; raw keys and provider error payloads are never logged.

## Maps Compose boundary

`GoogleAttendanceMap` is the only package allowed to translate project map
models to Google Maps Compose types.

```text
Attendance Route
→ immutable MapUiState + one-time MapCameraEffect
→ GoogleAttendanceMap
→ GoogleMap / Marker / Circle / CameraPositionState
```

Compose ownership rules:

- `CameraPositionState`, `MapProperties`, `MapUiSettings`, and marker state stay
  inside the adapter.
- Use `rememberCameraPositionState` and `rememberUpdatedMarkerState`.
- The ViewModel emits semantic effects such as focus, fit, and follow; it never
  stores `LatLng`, `CameraUpdate`, or `CameraPositionState`.
- Maps Compose owns the underlying map lifecycle. Do not reintroduce manual
  `MapView` lifecycle forwarding.
- Convert provider callbacks to stable project IDs and `GeoCoordinate` before
  they leave the adapter.

Infinite Track renders its own current-location marker from
`CurrentLocationRepository`. The Google My Location layer and built-in My
Location button stay disabled so the map is not a second location/permission
owner.

Initial interaction defaults:

- scroll and zoom gestures: enabled;
- tilt and rotate gestures: disabled;
- built-in zoom controls: disabled;
- built-in My Location button: disabled;
- map toolbar: disabled;
- map type: normal;
- traffic and indoor layers: disabled.

These defaults keep attendance interactions in-app and avoid duplicated
controls. Any later product change must be expressed as project-owned map
configuration, not ad hoc SDK calls from a screen.

## Layout, attribution, and accessibility

- Apply dynamic map content padding for the app bar, system bars, floating
  controls, and bottom sheet.
- Padding must keep the Google logo and copyright notices visible.
- Do not cover attribution with custom cards, snackbars, or bottom sheets.
- Add a localized semantic description for the map and verify TalkBack focus.
- Keep non-map actions available as normal Compose controls; map gestures cannot
  be the only way to focus current location or choose a target.
- Verify font scale, landscape, and edge-to-edge layouts on device.

## Marker and styling decision

Initial cutover uses standard `Marker` and `Circle`, which satisfy the current
attendance target, current location, WFA recommendation, and radius use cases.
A Map ID is therefore not required for functional parity.

Advanced markers and cloud-based map styling are deferred. If later approved,
add an Android Map ID through ignored/CI configuration, check runtime advanced
marker capability, and keep a standard-marker fallback. Never use
`DEMO_MAP_ID` in production.

Local JSON map styling may be evaluated after parity if it improves visual
integration without hiding required map content or attribution.

## Places search policy

Use pinned Places SDK 2.6.0 programmatically behind
`PlaceDiscoveryRepository`; the provider boundary allows a later New SDK
upgrade without changing UI contracts.

- Start one `AutocompleteSessionToken` when a user begins a search session.
- Reuse it across prediction requests and the selected Place Details request.
- End and replace it after selection, cancellation, abandonment, or error
  recovery.
- Debounce input and cancel superseded Google Tasks through coroutine
  cancellation.
- Use Indonesia as the country/region context.
- Prefer location bias around a fresh current location or authoritative target;
  do not hard-restrict the result area unless the product contract requires it.
- Request only fields available and needed on 2.6.0:
  `ID`, `NAME`, `ADDRESS`, and `LAT_LNG`.
- A suggestion remains an opaque `placeId` plus display text until an explicit
  Place Details resolution succeeds.
- Provide typed empty, unavailable, rate-limited, authentication, and network
  outcomes.
- Do not make selection mandatory; cancellation returns to the previous valid
  state.

## App Check rollout

App Check protects Places SDK (New), not the map renderer or the pinned 2.6.0
legacy path. Defer this hardening phase until the separate Places SDK upgrade:

1. Complete the functional migration on the pinned Places SDK 2.6.0 baseline.
2. Upgrade to a supported Places SDK (New) baseline.
3. Register the Android package and its SHA-256 fingerprint for App Check.
4. Integrate Firebase App Check with Play Integrity at application startup
   before constructing the Places client.
5. Use only the Firebase debug provider/token path for local emulator and CI.
6. Observe verified, outdated, and invalid traffic metrics.
7. Enable enforcement only after legitimate released clients are predominantly
   verified.

The rollout must account for startup attestation latency, Play Integrity quota,
and devices that fail the selected attestation policy. App Check does not
replace Android application/API restrictions on the key.

## Monitoring and operational gates

Configure and record, without identifiers or secrets:

- Maps SDK request/load usage;
- Places request and error trends;
- quota thresholds;
- billing budget alerts;
- unexpected credential usage;
- App Check traffic categories before enforcement;
- an owner email that receives deprecation and breaking-change notices.

Runtime verification must include a Play-enabled emulator and at least one
physical device. A compile-only result cannot close INF-140.

## Explicitly out of scope

- Navigation SDK;
- Routes API;
- server-side Geocoding API calls from Android;
- permanent runtime Mapbox/Google switching;
- advanced markers or cloud styling before functional parity;
- changing backend attendance authority or geofence semantics.
