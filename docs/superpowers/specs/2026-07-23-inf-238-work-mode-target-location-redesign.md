# INF-238 Work Mode and Target Location Redesign

**Status:** Design locked  
**Linear:** INF-238  
**GitHub:** #96  
**Dependency:** INF-140 / #94

## Problem

The Attendance bottom sheet already works as the interaction container, but the current presentation mixes work-mode selection, authoritative target state, map markers, WFA recommendation preview, and attendance action concerns. The redesign must improve hierarchy and clarity without rewriting the bottom-sheet mechanics or changing business semantics.

## Locked Product Contract

```text
WFO target
→ status-today.active_location

WFH target
→ /api/auth/me profile data
→ mandatory and admin-provisioned

WFA target
→ approved booking for the Attendance date
```

WFA recommendations, search results, and marker selections are discovery or request inputs only. They never become authoritative Attendance targets automatically.

## Locked UX Direction

Preserve the existing Attendance bottom-sheet behavior. Redesign only:

- Work Mode cards;
- target information hierarchy;
- marker semantics;
- WFA recommendation presentation;
- contextual recovery;
- primary-action hierarchy.

The screen structure remains:

```text
Attendance top bar
Map context
Existing bottom sheet
```

The map stays visible behind the current sheet. No new draggable-sheet state model is introduced.

## Information Hierarchy

### Work Mode selector

The bottom sheet presents three selectable options:

```text
WFO — Work From Office
WFH — Work From Home
WFA — Work From Anywhere
```

Each option uses an existing Infinite Track surface/card primitive, an existing Material icon, concise supporting copy, and one selected-state indicator.

### Authoritative Target summary

The selected mode is followed by one target summary that displays only authoritative information:

```text
target name or description
target source
radius
current distance when available
inside / outside / unknown range
WFA booking date and status when relevant
```

Preview information is visually and structurally separate from authoritative target information.

### Primary action

Only one primary action is shown for the current state.

```text
WFO/WFH ready
→ Lanjut ke Verifikasi Wajah

WFA not requested
→ Ajukan WFA

WFA pending
→ Lihat status permintaan

WFA rejected
→ Lihat status / Ajukan ulang when allowed

WFA approved + ready
→ Lanjut ke Verifikasi Wajah

Outside range
→ Fokus ke lokasi target or refresh location

WFH profile contract failure
→ Muat ulang
```

Do not display a disabled Attendance CTA beside a competing recovery CTA.

## Mode Behavior

### WFO

Source: `status-today.active_location`.

The selected WFO state shows:

- assigned office identity or description;
- office target marker;
- radius circle;
- target source;
- current distance/range;
- no search or edit action.

### WFH

Source: `/api/auth/me` profile data.

WFH is mandatory and admin-provisioned. The employee cannot edit, search, or pick a WFH target from Attendance.

Normal state:

```text
Work From Home
Lokasi ditetapkan oleh admin
Home geofence
```

Missing or invalid WFH coordinates are a contract/synchronization failure:

```text
Lokasi WFH belum dapat dimuat
Muat ulang
Hubungi admin
```

### WFA

WFA requires an approved booking for the Attendance date.

Lifecycle:

```text
Not requested
Pending
Rejected
Approved
```

Recommendation selection leads to booking preparation. Only approved booking data becomes `SelectedTargetLocation`.

## Marker Semantics

Minimum marker roles:

```text
CURRENT_LOCATION
AUTHORITATIVE_TARGET
WFA_RECOMMENDATION
SEARCH_PREVIEW
```

Suggested presentation:

- current location: cyan/blue;
- authoritative target: primary purple with radius circle;
- WFA recommendation: distinct recommendation marker;
- search preview: outlined or dashed preview marker.

Required mapping direction:

```text
SelectedTargetLocation
→ TargetLocationMapMapper
→ AUTHORITATIVE_TARGET marker
```

Forbidden direction:

```text
marker click
→ silently replace Attendance target
```

Marker styling is presentation-owned. Coordinates and marker roles use the provider-neutral contracts from INF-140.

## WFA Recommendation Contract

Android calls:

```text
GET /api/wfa/recommendations?lat={latitude}&lng={longitude}
```

The backend obtains places from Geoapify and applies FAHP scoring before returning recommendations.

The compact redesign uses only backend-supported, typed fields:

```text
name
address
latitude
longitude
category
suitability_score
suitability_label
distance_from_center
```

### No-image decision

The known response does not include a trustworthy image URL. Therefore the recommendation row contains no venue photograph.

Use:

```text
existing category icon
location name
category + formatted distance
suitability score + label
selection indicator
```

Do not:

- bundle random venue photos;
- fetch unrelated public images;
- add a new image provider;
- imply an illustration is the actual venue photo;
- assume a new backend field.

The score must be labelled as a WFA suitability or recommendation score, not as a Google, Geoapify, or public review rating.

### Richer data

Backend reliability, amenities, internet, noise, crowd, and operational-hours data may be shown only after Android DTO and domain contracts explicitly map and test those fields. They are not required for this compact redesign.

## Existing-Component Strategy

Reuse the current design system before creating feature wrappers:

```text
InfiniteCard / InfiniteSurface
InfiniteInfoRow
InfiniteStatusPill
InfiniteButton
InfiniteIconButton
InfiniteInlineAlert
existing Material icons
```

A small feature wrapper is allowed only for Attendance-specific composition:

```kotlin
@Composable
internal fun WfaRecommendationOption(
    model: WfaRecommendationUiModel,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
)
```

It must not become a second global component system.

## State Ownership

`AttendancePreparationViewModel` owns persistent preparation state:

```text
selected mode
authoritative target
target source
current location
range status
WFA booking status
recommendation state
selected recommendation preview
eligibility
map UI projection
```

The ViewModel does not own provider SDK types or `NavController`.

One-time effects are semantic:

```text
FocusAuthoritativeTarget
FocusRecommendation
OpenWfaBooking
OpenWfaRequests
OpenFaceVerification
RefreshProfile
ContactAdmin
```

The Route executes navigation and map-provider commands.

## Data and Domain Rules

- DTO and profile models do not leak into ViewModel or Compose.
- `/me` WFH data is mapped into a domain target model.
- `SelectedTargetLocation` remains the only authoritative target consumed downstream.
- WFA recommendation selection remains preview state.
- latest-selection-wins cancels or ignores stale mode-resolution results.
- checkout uses the active backend session and does not require mode reselection.

## Minimum State Matrix

```text
WFO resolving / ready / unavailable
WFH resolving / ready / profile-contract failure
WFA not requested / pending / rejected / approved / refresh failure
recommendation loading / content / empty / failure
recommendation unselected / selected
inside range / outside range / current location unavailable
mode switching / stale result ignored
booking handoff idle / navigating
```

## Accessibility and Responsive Rules

- Existing bottom-sheet mechanics remain unchanged.
- Selected state is conveyed by semantics, text, and icon—not color alone.
- Primary actions keep a minimum 48 dp touch target.
- Large-font layouts preserve name, category, distance, and action text.
- Narrow screens may wrap supporting information but must not clip the primary action.
- Recommendation rows use stable semantics for selection and score labels.

## Non-Goals

- Permission redesign.
- Bottom-sheet behavior rewrite.
- Google Maps implementation before INF-140.
- Face Recognition redesign.
- Geofence runtime rewrite.
- Backend Attendance endpoint changes.
- Employee editing of WFH target.
- Fabricated WFA photos.
- A permanent multi-provider map framework.

## Acceptance Criteria

### Product and data

- WFO target comes from `status-today.active_location`.
- WFH target comes from `/api/auth/me`.
- WFH is mandatory, admin-provisioned, and not editable by the employee.
- WFA requires an approved booking.
- Recommendation and search state never replace the authoritative target.

### UX

- Existing bottom-sheet mechanics remain unchanged.
- Work Mode cards clearly expose WFO, WFH, and WFA with one selected state.
- Target summary displays source, radius, range, and booking evidence where applicable.
- Recommendation rows use existing components and category icons with no venue photos.
- Recommendation list and map-marker selection remain synchronized.
- Only one primary action is visible for each state.

### Architecture

- Provider-neutral types from INF-140 are used.
- No Mapbox or Google Maps SDK type exists in domain or ViewModel contracts.
- `/me` DTO does not leak beyond data mapping.
- `SelectedTargetLocation` remains the downstream truth.
- Work Mode rules remain in domain/use cases.
- semantic effects are executed by the Route/map adapter.

### Verification

- Domain tests cover WFO, WFH from `/me`, and WFA approved-booking resolution.
- ViewModel tests cover latest-selection-wins and recommendation selection.
- Mapper tests cover target-to-marker projection.
- Compose tests/previews cover loading, empty, failure, selected, and content states.
- Runtime evidence covers WFO, WFH, WFA recommendation, booking handoff, approved target, and range recovery.
- `./gradlew app:assembleDebug` passes.
