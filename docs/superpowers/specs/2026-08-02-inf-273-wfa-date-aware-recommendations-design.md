# INF-273 — Android Date-Aware WFA Recommendations Design

**Date:** 2026-08-02  
**Linear:** [INF-273 — Android: Auto-load date-aware WFA recommendations inside WFA Request graph](https://linear.app/infinite-track-palu/issue/INF-273/android-auto-load-date-aware-wfa-recommendations-inside-wfa-request)  
**Repository:** `Infinite-LearningV1/android`  
**Integration branch:** `develop`  
**Feature branch:** `feature/inf-273-wfa-date-aware-recommendations`  
**Base:** `develop` at `dbc3c6cd838204519ef0e0bb06763537e21a1290`  
**Backend dependency:** [INF-272](https://linear.app/infinite-track-palu/issue/INF-272/backend-fix-wfa-facility-scoring-with-geoapify-place-details-and)

> This branch contains documentation only. Implementation must be performed from an isolated worktree for this branch.

## 1. Goal

Move future WFA recommendation discovery out of Attendance and into the existing graph-scoped WFA Request transaction.

The employee-facing behavior becomes:

```text
Select WFA mode
→ resolve today's authoritative WFA state
→ when no WFA request exists, automatically open WFA Request
→ default schedule date to tomorrow in Asia/Jakarta
→ automatically load date-aware recommendations
→ select a recommendation
→ fill request reason and notes
→ review
→ submit
→ render backend-confirmed result
```

The user must not need to press a primary `Cari rekomendasi lokasi` button before recommendations appear.

This is a bounded evolution of the current architecture. It is not an Attendance rewrite, navigation rewrite, or WFA request redesign.

## 2. Locked product decisions

### 2.1 Attendance behavior

Attendance remains the owner of today's attendance readiness and authoritative target location.

When WFA mode is selected:

| Backend-authoritative state | Android behavior |
|---|---|
| Approved booking for today | Stay in Attendance and render the approved booking as the authoritative target. |
| Pending request | Keep the existing recovery to WFA request history. |
| Rejected request | Keep the existing recovery to WFA request history. |
| Approved booking for another date / approval missing for today | Keep the existing recovery to WFA request history. |
| No request exists | Automatically open the WFA Request graph after resolution. No additional location-selection CTA is required in Attendance. |
| Status/profile/booking resolution fails | Stay in Attendance and expose the existing typed recovery. Do not open request creation from uncertain state. |

Selecting WFA does not make a recommendation an attendance target. Only a backend-approved WFA booking may become an authoritative target for attendance.

### 2.2 Schedule date

- Initial schedule date is tomorrow in `Asia/Jakarta`.
- Today is invalid and disabled.
- Past dates are invalid and disabled.
- Tomorrow is the earliest valid date, not the only valid date.
- Any later future date remains selectable.
- Android validates `scheduleDate > todayInJakarta` for immediate feedback.
- Backend remains authoritative for strict date, duplicate booking, and final eligibility checks.

### 2.3 Recommendation loading

- Recommendations load automatically after the WFA Request graph has a valid date and current coordinate.
- Changing the schedule date automatically loads recommendations for the new date.
- The old request is canceled and invalidated.
- The previously selected candidate is cleared before the new request starts.
- Stale responses from an old date may never overwrite the newest date state.
- Retry uses the current valid schedule date and the latest available current coordinate.
- A small device-location change does not trigger an automatic reload by itself.

### 2.4 Candidate truthfulness

Android consumes the truthful INF-272 contract:

```text
ranked
insufficient_facility_data
facility_enrichment_failed
```

- Only `ranked` candidates have a non-null final score and final label.
- Missing final score remains `null`; Android must never display it as `0`.
- Missing provider evidence is `unknown`; Android must never present it as unavailable by assumption.
- Non-ranked candidates may remain selectable because missing evidence is not the same as an invalid location.
- Android does not calculate, repair, or infer facility evidence locally.

## 3. Current `develop` mapping

### 3.1 Attendance currently owns request discovery

`AttendanceViewModel` currently injects `GetWfaRecommendationsUseCase`, owns `recommendationJob`, and invokes recommendation discovery whenever WFA mode is selected.

Current orchestration:

```text
onWorkModeSelected(WFA)
→ resolveAndApplyTargetForMode(WFA)
   ├── resolve today's booking and authoritative target
   └── fetchWfaRecommendations(current coordinate)
```

The recommendation request currently has no schedule date.

`AttendancePreparationState` currently contains:

```text
wfaDiscovery
mapPickInteraction
```

`AttendanceScreen` currently owns:

- recommendation marker selection;
- recommendation loading overlay;
- search-location result consumption through `savedStateHandle`;
- pick-on-map interaction;
- navigation to `wfa_request/{latitude}/{longitude}` only after a location has been selected.

This mixes today's attendance readiness with a future request transaction.

### 3.2 WFA Request currently requires a preselected location

Current parent route:

```text
wfa_request/{latitude}/{longitude}
```

`WfaRequestViewModel` reads both route arguments from `SavedStateHandle`. Missing or invalid coordinates produce `BootstrapUnavailable` before configuration is loaded.

Current bootstrap:

```text
route coordinates
→ reverse geocode
→ create WfaCandidateLocation
→ load employee
→ load request config
→ Editing
```

The ViewModel does not currently own:

- current-device coordinate acquisition;
- recommendation query state;
- recommendation cancellation or stale-result protection;
- selected recommendation identity;
- default schedule-date policy.

### 3.3 Form currently assumes location is always present

`WfaRequestFormScreen` returns from `FormContent` when no location exists. The location card is read-only and appears before request details.

The existing `DatePickerButton` already supports a `minimumDate` parameter, but the WFA form does not pass it.

`ValidateWfaRequestDraftUseCase` only checks that a date is present. It does not reject same-day or past dates.

### 3.4 Recommendation data contract is obsolete

Current Retrofit request:

```kotlin
getWfaRecommendations(latitude, longitude)
```

Current DTO requires:

```text
suitability_score: non-null
suitability_label: non-null
score_details:
- wifi_quality
- noise_level
- crowd_density
- operational_hours
- amenities
```

Current Domain requires `suitabilityScore: Double` and `suitabilityLabel: String`.

These fields conflict with INF-272, which requires `schedule_date`, nullable final scoring, explicit candidate status, facility confidence, and tri-state facility evidence.

## 4. Scope boundary

### In scope

- Preserve backend-authoritative WFA state resolution in Attendance.
- Automatically navigate to request creation only for `WFA_NOT_REQUESTED`.
- Remove new-request recommendation ownership from `AttendanceViewModel`.
- Change the WFA Request parent route to no longer require coordinates.
- Default schedule date to tomorrow in Jakarta.
- Disable and reject today/past dates.
- Load recommendations automatically in `WfaRequestViewModel`.
- Reload recommendations on valid date changes.
- Add cancellation and stale-response protection.
- Clear selected location on date changes.
- Consume the INF-272 response contract without fabricated fields.
- Render loading, content, empty, retryable failure, and insufficient-evidence states.
- Preserve form, review, submit, and result semantics from INF-265.
- Preserve manual location search as a secondary fallback through an explicit navigation-result contract.
- Update unit, mapper, repository, ViewModel, navigation, and focused Compose tests.

### Explicitly out of scope

- Changing Backend date policy or adding same-day booking.
- Changing WFA approval/rejection behavior.
- Changing the WFA request-history screen.
- Rewriting Attendance preparation, geofence, face verification, or attendance submission.
- Caching recommendations across request sessions.
- Background prefetch or periodic recommendation refresh.
- Client-side FAHP or client-side facility inference.
- Reintroducing fabricated wifi, noise, crowd, workspace, power-outlet, or reliability scores.
- A broad map-provider refactor.
- A second WFA request ViewModel or parallel request graph.
- Making search or pick-on-map the primary recommendation trigger.

## 5. Adopted architecture

Preserve the repository contract:

```text
Screen
→ WfaRequestViewModel
→ GetWfaRecommendationsUseCase
→ WfaRepository
→ WfaRepositoryImpl
→ ApiService
```

Transaction ownership:

```text
AttendanceViewModel
└── today's authoritative WFA state only

WfaRequestViewModel (graph scoped)
├── Jakarta schedule-date policy
├── current coordinate snapshot
├── recommendation request lifecycle
├── selected candidate
├── request config
├── employee summary
├── editable draft
├── immutable review snapshot
└── submit/result lifecycle
```

No DTO, Retrofit exception, or Compose type may cross into Domain.

## 6. Navigation contract

### 6.1 Parent route

Replace:

```text
wfa_request/{latitude}/{longitude}
```

with:

```text
wfa_request
```

Keep the existing child destinations:

```text
wfa_request/form
wfa_request/review
wfa_request/result
```

Rules:

- The graph still owns one graph-scoped `WfaRequestViewModel`.
- Coordinates, recommendation objects, and draft data are not serialized into routes.
- Screens continue to receive semantic callbacks rather than a `NavController`.
- Each destination remains registered once under one graph owner.
- The old coordinate route is removed after every call site and test is migrated; do not keep two active parent routes.

### 6.2 Attendance entry

`AttendanceViewModel` emits a navigation target only after WFA resolution proves `WFA_NOT_REQUESTED`.

The target contains the semantic parent route only:

```kotlin
NavigationTarget.WfaRequest(Screen.WfaRequestFlow.route)
```

Repeated WFA selections while the same resolution is in flight must not create duplicate navigation events.

### 6.3 Manual location-search fallback

The existing `LocationSearch` destination may remain as a secondary action inside the WFA Request form.

Result handling must use a centralized typed contract rather than a string literal spread across screens:

```kotlin
object LocationSearchResultContract {
    const val RESULT_KEY = "selected_location"
}
```

The graph host consumes the result once, dispatches `WfaRequestEvent.ManualLocationSelected`, and removes the saved-state value after consumption.

This explicit contract is the only accepted `savedStateHandle` use for this result. It is not a free-form event bus.

## 7. Domain contract

### 7.1 Schedule-date policy

Create one domain policy for Jakarta calendar rules:

```kotlin
class WfaScheduleDatePolicy @Inject constructor() {
    fun today(): LocalDate
    fun minimumDate(today: LocalDate = today()): LocalDate
    fun isSelectable(date: LocalDate, today: LocalDate = today()): Boolean
}
```

Invariants:

```text
zone = Asia/Jakarta
minimumDate = today + 1 day
selectable = date > today
```

The policy must be testable with an explicit `today` value. UI uses `minimumDate`; Domain validation uses `isSelectable`.

Add an explicit field error:

```kotlin
WfaRequestFieldError.FUTURE_DATE_REQUIRED
```

### 7.2 Recommendation query

Use one typed query:

```kotlin
data class WfaRecommendationQuery(
    val origin: GeoCoordinate,
    val scheduleDate: LocalDate
)
```

Repository and use case signatures become:

```kotlin
suspend fun getRecommendations(
    query: WfaRecommendationQuery
): WfaRecommendationResult
```

### 7.3 Recommendation result and failure

```kotlin
sealed interface WfaRecommendationResult {
    data class Success(
        val scheduleDate: LocalDate,
        val timezone: String,
        val recommendations: List<WfaRecommendation>,
        val meta: WfaRecommendationMeta?
    ) : WfaRecommendationResult

    data class Failure(
        val failure: WfaRecommendationFailure
    ) : WfaRecommendationResult
}
```

```kotlin
sealed interface WfaRecommendationFailure {
    data object InvalidScheduleDate : WfaRecommendationFailure
    data object DuplicateBooking : WfaRecommendationFailure
    data object NetworkUnavailable : WfaRecommendationFailure
    data object ProviderUnavailable : WfaRecommendationFailure
    data object ServerUnavailable : WfaRecommendationFailure
    data object Unknown : WfaRecommendationFailure
}
```

Authentication failures continue through the repository's existing global session handling and must not be rendered as recommendation-empty state.

### 7.4 Recommendation model

```kotlin
sealed interface WfaRecommendationStatus {
    data object Ranked : WfaRecommendationStatus
    data object InsufficientFacilityData : WfaRecommendationStatus
    data object FacilityEnrichmentFailed : WfaRecommendationStatus
    data class Unsupported(val raw: String) : WfaRecommendationStatus
}
```

```kotlin
enum class WfaFacilityAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}
```

```kotlin
data class WfaFacilityEvidence(
    val internetAccess: WfaFacilityAvailability,
    val openingHours: WfaFacilityAvailability,
    val toilets: WfaFacilityAvailability,
    val airConditioning: WfaFacilityAvailability,
    val wheelchairAccessibility: WfaFacilityAvailability
)
```

```kotlin
data class WfaRecommendation(
    val stableKey: String,
    val placeId: String?,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val placeType: String,
    val distanceMeters: DistanceMeters,
    val status: WfaRecommendationStatus,
    val finalRank: Int?,
    val finalScore: Double?,
    val finalLabel: String?,
    val facilityScore: Double?,
    val facilityConfidence: Int,
    val facilities: WfaFacilityEvidence
)
```

Invariants:

- `Ranked` requires non-null `finalScore` and `finalLabel`.
- Non-ranked states expose `finalScore = null`, `finalLabel = null`, and `finalRank = null`.
- `facilityConfidence` is clamped or rejected outside `0..100`; the mapper must not silently invent a value.
- Stable identity uses non-blank `placeId` first and falls back to the existing normalized name/coordinate key.

The unused legacy `WfaRecommendationDetail`, `ScoreItem`, and `AmenityItem` models are removed when search confirms they have no active consumers.

## 8. Data contract

### 8.1 Retrofit request

```http
GET /api/wfa/recommendations?lat=<lat>&lng=<lng>&schedule_date=YYYY-MM-DD
```

```kotlin
@GET("api/wfa/recommendations")
suspend fun getWfaRecommendations(
    @Query("lat") latitude: Double,
    @Query("lng") longitude: Double,
    @Query("schedule_date") scheduleDate: String
): WfaRecommendationResponseDto
```

`LocalDate` is formatted with `DateTimeFormatter.ISO_LOCAL_DATE` at the data boundary.

### 8.2 Response DTO

The Android DTO mirrors INF-272:

```text
data.schedule_date
data.timezone
data.work_window
data.recommendations[]
meta
```

Each candidate maps:

```text
place_id
name
address
latitude
longitude
distance_meters
place_type
status
final_rank
final_score
final_label
facility_score
facility_confidence
facilities.internet_access
facilities.opening_hours
facilities.toilets
facilities.air_conditioning
facilities.wheelchair_accessibility
```

Facility booleans map as:

```text
true  → AVAILABLE
false → UNAVAILABLE
null  → UNKNOWN
```

Delete the active DTO dependency on `score_details`, wifi, noise, crowd, generic amenities, and non-null suitability score.

### 8.3 Failure mapping

`WfaRepositoryImpl` maps transport and stable Backend codes into `WfaRecommendationFailure`.

At minimum:

| Backend/transport outcome | Domain failure |
|---|---|
| `PAST_DATE_NOT_ALLOWED`, `SAME_DAY_NOT_ALLOWED`, invalid schedule | `InvalidScheduleDate` |
| `DUPLICATE_BOOKING` | `DuplicateBooking` |
| no network / timeout | `NetworkUnavailable` |
| provider/config failure contract | `ProviderUnavailable` |
| HTTP 5xx | `ServerUnavailable` |
| unrecognized safe failure | `Unknown` |

Raw response bodies and Retrofit exceptions do not cross into Presentation.

## 9. Presentation state and events

### 9.1 Recommendation state

Create a request-owned state:

```kotlin
sealed interface WfaRequestRecommendationState {
    data object Initializing : WfaRequestRecommendationState
    data object Loading : WfaRequestRecommendationState
    data object Empty : WfaRequestRecommendationState

    data class Content(
        val recommendations: List<WfaRecommendation>,
        val selectedKey: String?
    ) : WfaRequestRecommendationState

    data class Failure(
        val failure: WfaRecommendationFailure,
        val retryable: Boolean
    ) : WfaRequestRecommendationState
}
```

Extend `WfaRequestUiState` with:

```kotlin
val minimumScheduleDate: LocalDate?
val currentCoordinate: GeoCoordinate?
val recommendationState: WfaRequestRecommendationState
```

`draft.location` remains the single source of truth for the candidate that will be reviewed and submitted. `selectedKey` is presentation identity only.

### 9.2 Events

Add:

```kotlin
data class RecommendationSelected(val stableKey: String) : WfaRequestEvent
data class ManualLocationSelected(val location: WfaCandidateLocation) : WfaRequestEvent
data object RetryRecommendationsClicked : WfaRequestEvent
```

Event behavior:

- `ScheduleDateChanged(valid date)` updates the draft, clears location/selection, cancels the old request, and starts a new request.
- `ScheduleDateChanged(invalid date)` does not call the repository and records `FUTURE_DATE_REQUIRED`.
- `RecommendationSelected` copies the selected candidate's factual name/address/coordinate into `draft.location`.
- `ManualLocationSelected` sets `draft.location` and clears recommendation selection without claiming backend-ranked evidence.
- `RetryRecommendationsClicked` reuses the current valid date and refreshes the current coordinate snapshot before requesting.

### 9.3 Bootstrap and phase behavior

Bootstrap no longer fails because route coordinates are absent.

Target sequence:

```text
Loading
→ compute minimum/default date
→ load employee + config
→ acquire current coordinate
→ Editing + recommendation Loading
→ Content / Empty / recommendation Failure
```

Rules:

- Config/employee bootstrap failure may still use the screen-level `Failure` phase.
- Recommendation failure does not destroy the editable draft and does not become screen-level failure.
- Submit failure preserves the selected date, location, reason, and notes as today.
- Review and result behavior from INF-265 remains unchanged.

## 10. Recommendation request lifecycle

`WfaRequestViewModel` owns one `recommendationJob` and a monotonically increasing request token.

```text
start query
→ cancel previous job
→ increment token
→ set Loading
→ call use case
→ apply result only when token and scheduleDate still match
```

Cancellation rules:

- Date change cancels the old job.
- Retry cancels the old job.
- Leaving the graph clears the ViewModel and cancels its scope.
- Repeated identical events while the same query is loading do not create concurrent duplicate requests.

Stale-result guard checks both:

```text
request token
scheduleDate captured by the request
```

This is required even when coroutine cancellation is used because a transport call may finish after cancellation.

## 11. UI behavior

The existing WFA form remains one scrollable transaction screen.

Recommended order:

```text
Top bar
Schedule date
Recommendation picker
Selected location summary
Employee summary
Reason and notes
Eligibility/review checklist
Continue to review
```

### Recommendation picker

- `Initializing`: compact preparation state.
- `Loading`: loading card and map placeholder; do not block editing reason/notes.
- `Content`: provider-neutral map plus recommendation cards.
- `Empty`: explicit no-recommendation state with retry.
- `Failure`: typed message with retry where allowed.

Each recommendation card shows:

- name;
- address or place type;
- distance;
- final score and label only for `Ranked`;
- facility confidence;
- explicit copy for insufficient evidence or enrichment failure.

Do not show `0%` for missing final score.

### Map

Reuse the provider-neutral map adapter through a focused `WfaRequestMapUiMapper`.

Markers:

- current device location;
- every recommendation candidate;
- selected candidate highlighted.

No geofence circle is rendered because a recommendation is not an approved attendance target.

Marker and card clicks dispatch the same `RecommendationSelected` event.

### Date picker

Pass `uiState.minimumScheduleDate` to the existing controlled `DatePickerButton.minimumDate` parameter.

The Domain validator remains mandatory even though the picker disables invalid dates.

## 12. Attendance cleanup

After request ownership moves:

- Remove `GetWfaRecommendationsUseCase` from `AttendanceViewModel`.
- Remove `recommendationJob` and `fetchWfaRecommendations` from Attendance.
- Remove recommendation loading/retry/selection and new-request map-pick state from `AttendancePreparationState`.
- Remove recommendation markers from `AttendanceMapUiMapper`.
- Remove recommendation list/search/pick actions from the Attendance bottom sheet for new-request creation.
- Remove recommendation loading overlay and marker-click handling from `AttendanceScreen`.
- Keep approved target rendering, target focus, location readiness, and attendance submission unchanged.

Do not remove the shared provider-neutral map infrastructure.

## 13. Error and copy contract

Presentation maps Domain failures to safe localized UI text.

Required distinctions:

- date no longer eligible;
- duplicate request for selected date;
- current location unavailable;
- network unavailable;
- provider temporarily unavailable;
- server unavailable;
- no recommendations found;
- candidate has insufficient facility evidence;
- candidate enrichment failed.

Config failure and recommendation failure remain distinct.

No raw Backend message, provider payload, exception message, or API key is rendered or logged.

## 14. Testing strategy

TDD is required.

### Domain

- tomorrow is the minimum date in Jakarta;
- today and past are invalid;
- future dates are valid;
- validation returns `FUTURE_DATE_REQUIRED`;
- recommendation status and evidence invariants.

### Data

- query sends ISO `schedule_date`;
- ranked DTO maps complete score data;
- insufficient-data DTO maps null scores;
- enrichment-failed DTO maps null scores;
- facility booleans/null map to tri-state Domain values;
- unknown status is safe and non-scored;
- stable key prefers `place_id`;
- backend error codes map to typed failures.

### WFA Request ViewModel

- bootstrap defaults date to tomorrow;
- bootstrap loads recommendations automatically;
- no route coordinates are required;
- date change clears selection and reloads;
- old job is canceled;
- stale old-date response is ignored;
- duplicate identical load is guarded;
- retry preserves draft fields;
- location failure is recommendation failure, not config failure;
- selection writes exact candidate into the draft;
- non-ranked candidate remains selectable;
- review still requires a location;
- submit/retry behavior from INF-265 remains intact.

### Attendance

- approved WFA stays in Attendance;
- pending/rejected/missing approval keeps request-history recovery;
- not-requested state automatically emits WFA Request navigation;
- Attendance no longer calls recommendation use case;
- repeated WFA selection does not emit duplicate navigation.

### Navigation and Compose

- parent route has no coordinate arguments;
- form/review/result share one controller;
- search result is consumed once through the typed key;
- today/past are blocked by the date picker and Domain;
- loading/content/empty/failure are rendered;
- nullable score does not render `0`;
- candidate selection updates the selected location card;
- changing date clears the selected location UI.

## 15. Verification gates

Before Done, evidence must include:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Run focused connected tests when an emulator/device is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Minimum runtime matrix:

```text
WFA approved today
WFA pending
WFA rejected
WFA not requested
initial tomorrow recommendation load
date change to later future date
same-day/past blocked
recommendation empty
network failure + retry
stale response after date change
ranked candidate
insufficient facility data candidate
enrichment failed candidate
manual search fallback
review and successful submit
backend duplicate-date rejection
```

## 16. Acceptance criteria

- Selecting WFA automatically enters request creation only when Backend truth resolves to no existing request.
- Approved WFA remains the authoritative Attendance target.
- Pending/rejected/missing-approval states keep their existing recovery.
- WFA Request parent route no longer requires latitude/longitude.
- Tomorrow in Jakarta is the initial date and earliest selectable date.
- Today and past dates are disabled and rejected by Domain.
- Recommendations load automatically with `lat`, `lng`, and `schedule_date`.
- Date changes cancel/invalidate the old query, clear selection, and reload automatically.
- Stale responses cannot overwrite current state.
- Recommendation state belongs to `WfaRequestViewModel`, not `AttendanceViewModel`.
- New DTO/Domain models represent nullable final scoring and explicit statuses truthfully.
- No active Android presentation depends on fabricated score details.
- Ranked and non-ranked candidate UI is honest.
- Manual search remains secondary and does not masquerade as a ranked recommendation.
- Existing form/review/submit/result semantics remain intact.
- Unit, mapper, repository, ViewModel, navigation, and focused UI tests are updated.
- Build, test, and lint evidence is attached before the issue is marked Done.
