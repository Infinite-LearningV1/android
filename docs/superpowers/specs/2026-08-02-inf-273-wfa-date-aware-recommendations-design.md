# INF-273 — Android Date-Aware WFA Recommendations Design

**Date:** 2026-08-02  
**Linear:** [INF-273](https://linear.app/infinite-track-palu/issue/INF-273/android-auto-load-date-aware-wfa-recommendations-inside-wfa-request)  
**Repository:** `Infinite-LearningV1/android`  
**Integration branch:** `develop`  
**Feature branch:** `feature/inf-273-wfa-date-aware-recommendations`  
**Base:** `develop` at `dbc3c6cd838204519ef0e0bb06763537e21a1290`  
**Backend dependency:** [INF-272](https://linear.app/infinite-track-palu/issue/INF-272/backend-fix-wfa-facility-scoring-with-geoapify-place-details-and)

> This branch contains documentation only. Implementation must run from an isolated worktree for this branch.

## 1. Goal

Move future WFA recommendation discovery from Attendance into the existing graph-scoped WFA Request transaction.

```text
Select WFA mode
→ resolve today's backend-authoritative WFA state
→ when no request exists, automatically open WFA Request
→ default schedule date to tomorrow in Asia/Jakarta
→ automatically load recommendations
→ select location
→ fill request
→ review
→ submit
→ backend-confirmed result
```

There is no primary `Cari rekomendasi lokasi` trigger. Recommendations appear automatically after the request graph has a valid date and current coordinate.

## 2. Locked behavior

### Attendance

Attendance remains responsible for today's attendance readiness and authoritative target.

| Resolved WFA state | Behavior |
|---|---|
| Approved for today | Stay in Attendance and use the approved booking target. |
| Pending or rejected | Keep the existing request-history recovery. |
| Approval missing for today | Keep the existing request-history recovery. |
| Not requested | Automatically open `wfa_request`. |
| Status/profile/booking resolution failure | Stay in Attendance and keep typed recovery. |

A recommendation is never an attendance target. Only a backend-approved booking can become the WFA attendance target.

### Date

- Business timezone: `Asia/Jakarta`.
- Initial date: tomorrow.
- Minimum valid date: tomorrow.
- Today and past dates are disabled in UI and rejected in Domain.
- Any later future date is selectable.
- Backend remains authoritative for strict date and duplicate-booking validation.

### Recommendation lifecycle

- Initial valid date automatically loads recommendations.
- Date change clears the selected candidate and starts a new request.
- The previous request is canceled and invalidated.
- A stale old-date response cannot overwrite the newest date state.
- Retry uses the current valid date and refreshes the current coordinate snapshot.
- Device-location drift alone does not trigger an automatic reload.

### Candidate truthfulness

Android consumes these INF-272 states:

```text
ranked
insufficient_facility_data
facility_enrichment_failed
```

Only `ranked` candidates expose final score/rank/label. Missing scoring remains `null`, never `0`. Missing facility evidence remains unknown, never inferred unavailable. Non-ranked candidates remain selectable because incomplete evidence is not an invalid location.

## 3. Current `develop` mapping

### Attendance owns future-request discovery today

`AttendanceViewModel` injects `GetWfaRecommendationsUseCase`, owns `recommendationJob`, and calls `fetchWfaRecommendations()` whenever WFA is selected. `AttendancePreparationState` owns `wfaDiscovery` and map-pick state. `AttendanceScreen` owns recommendation markers, loading overlay, search result consumption, map pick, and navigation only after a coordinate is selected.

Current flow:

```text
WFA selected
→ resolve today's WFA state
→ also fetch recommendations with lat/lng only
→ select recommendation/search/map location
→ navigate to wfa_request/{latitude}/{longitude}
```

This mixes today's attendance transaction with a future booking request.

### WFA Request requires preselected coordinates

`Screen.WfaRequestFlow` is `wfa_request/{latitude}/{longitude}`. `WfaRequestViewModel` reads both arguments, reverse-geocodes them, and fails bootstrap when they are absent. The ViewModel already owns form/review/result, but not current location, date policy, recommendation state, cancellation, or stale-response protection.

### Form and validation assumptions

`WfaRequestFormScreen` stops rendering when location is null. `DatePickerButton` already supports `minimumDate`, but the form does not pass it. `ValidateWfaRequestDraftUseCase` only checks date presence.

### Data contract mismatch

Current Retrofit sends only `lat` and `lng`. Current DTO/Domain require non-null `suitability_score`, `suitability_label`, and fabricated `score_details` fields. INF-272 instead requires `schedule_date`, explicit candidate status, nullable final scoring, facility confidence, and tri-state facility evidence.

## 4. Scope

### In scope

- Preserve current backend-authoritative WFA state resolution in Attendance.
- Auto-open request creation only for `WFA_NOT_REQUESTED`.
- Remove new-request recommendation ownership from Attendance.
- Change parent route to `wfa_request` without coordinates.
- Default and validate future Jakarta dates.
- Auto-load and reload recommendations in `WfaRequestViewModel`.
- Add cancellation, duplicate-load guard, and stale-response guard.
- Clear selection on date change.
- Consume the truthful INF-272 contract.
- Render loading, content, empty, failure, ranked, and incomplete-evidence states.
- Preserve INF-265 form/review/submit/result behavior.
- Preserve location search as a secondary fallback through one explicit saved-state result contract.
- Update focused tests, build, and lint evidence.

### Out of scope

- Same-day booking.
- Backend policy changes.
- WFA approval/history redesign.
- Attendance, geofence, face, or submission rewrite.
- Recommendation cache or background prefetch.
- Client-side FAHP or facility inference.
- Broad map-provider refactor.
- Making search/map-pick the primary recommendation trigger.

## 5. Architecture and ownership

```text
Screen
→ WfaRequestViewModel
→ GetWfaRecommendationsUseCase
→ WfaRepository
→ WfaRepositoryImpl
→ ApiService
```

```text
AttendanceViewModel
└── today's WFA state and approved target

WfaRequestViewModel (graph scoped)
├── date policy
├── current coordinate snapshot
├── recommendation lifecycle
├── selected candidate
├── employee/config
├── editable draft
├── review snapshot
└── submit/result lifecycle
```

No DTO, Retrofit exception, `Context`, Compose type, or `NavController` crosses into Domain.

## 6. Navigation

Parent route becomes:

```text
wfa_request
```

Child destinations remain:

```text
wfa_request/form
wfa_request/review
wfa_request/result
```

Rules:

- One graph-scoped `WfaRequestViewModel` remains shared by all children.
- Coordinates and draft objects are not route arguments.
- The old coordinate route is removed after all call sites migrate.
- Attendance emits one semantic `NavigationTarget.WfaRequest` only after the latest resolution returns `WFA_NOT_REQUESTED`.
- Rapid repeated selection cannot emit duplicate concurrent navigation.

Location search remains secondary. Centralize its result key:

```kotlin
object LocationSearchResultContract {
    const val RESULT_KEY = "selected_location"
}
```

`LocationSearchScreen` writes that key. The WFA Request graph consumes the typed `LocationResult` once, dispatches `ManualLocationSelected`, then removes the value. This is an explicit navigation result contract, not a free-form event bus.

## 7. Domain contract

### Date policy

```kotlin
class WfaScheduleDatePolicy {
    fun today(): LocalDate
    fun minimumDate(today: LocalDate = today()): LocalDate
    fun isSelectable(date: LocalDate, today: LocalDate = today()): Boolean
}
```

The implementation uses `Asia/Jakarta` and supports a fixed `Clock` factory for deterministic tests.

Add:

```kotlin
WfaRequestFieldError.FUTURE_DATE_REQUIRED
```

### Query and result

```kotlin
data class WfaRecommendationQuery(
    val origin: GeoCoordinate,
    val scheduleDate: LocalDate
)
```

```kotlin
sealed interface WfaRecommendationResult {
    data class Success(
        val scheduleDate: LocalDate,
        val timezone: String,
        val recommendations: List<WfaRecommendation>,
        val meta: WfaRecommendationMeta?
    ) : WfaRecommendationResult

    data class Failure(val failure: WfaRecommendationFailure) : WfaRecommendationResult
}
```

```kotlin
sealed interface WfaRecommendationFailure {
    data object CurrentLocationUnavailable : WfaRecommendationFailure
    data object InvalidScheduleDate : WfaRecommendationFailure
    data object DuplicateBooking : WfaRecommendationFailure
    data object NetworkUnavailable : WfaRecommendationFailure
    data object ProviderUnavailable : WfaRecommendationFailure
    data object ServerUnavailable : WfaRecommendationFailure
    data object Unknown : WfaRecommendationFailure
}
```

`CurrentLocationUnavailable` is created by request orchestration. Transport failures are created by `WfaRepositoryImpl`.

### Candidate model

```kotlin
sealed interface WfaRecommendationStatus {
    data object Ranked : WfaRecommendationStatus
    data object InsufficientFacilityData : WfaRecommendationStatus
    data object FacilityEnrichmentFailed : WfaRecommendationStatus
    data class Unsupported(val raw: String) : WfaRecommendationStatus
}

enum class WfaFacilityAvailability { AVAILABLE, UNAVAILABLE, UNKNOWN }

data class WfaFacilityEvidence(
    val internetAccess: WfaFacilityAvailability,
    val openingHours: WfaFacilityAvailability,
    val toilets: WfaFacilityAvailability,
    val airConditioning: WfaFacilityAvailability,
    val wheelchairAccessibility: WfaFacilityAvailability
)
```

`WfaRecommendation` contains place identity, name/address/coordinate/type/distance, status, nullable final rank/score/label, nullable facility score, confidence `0..100`, and five facility evidence values.

Stable key uses non-blank `place_id` first and the existing normalized name/coordinate fallback otherwise.

## 8. Data contract

Request:

```http
GET /api/wfa/recommendations?lat=<lat>&lng=<lng>&schedule_date=YYYY-MM-DD
```

Response fields consumed:

```text
data.schedule_date
data.timezone
data.work_window
data.recommendations[]
meta

recommendation:
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

Facility mapping:

```text
true  → AVAILABLE
false → UNAVAILABLE
null  → UNKNOWN
```

Remove active dependencies on legacy suitability and score-detail DTOs. A malformed ranked candidate must not invent a score; map it to an unsupported non-scored state or reject that candidate deterministically.

Failure mapping:

| Outcome | Domain |
|---|---|
| Past/same-day/invalid date code | `InvalidScheduleDate` |
| `DUPLICATE_BOOKING` | `DuplicateBooking` |
| IO/timeout | `NetworkUnavailable` |
| Provider/config failure code | `ProviderUnavailable` |
| HTTP 5xx | `ServerUnavailable` |
| Other safe failure | `Unknown` |

Global session handling continues to own authentication failures.

## 9. Presentation state and events

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

Extend `WfaRequestUiState` with minimum schedule date, current coordinate, and recommendation state. `draft.location` remains the only location reviewed and submitted; `selectedKey` is presentation identity.

Add events:

```kotlin
RecommendationSelected(stableKey)
ManualLocationSelected(location)
RetryRecommendationsClicked
```

Behavior:

- Valid date change updates date, clears location/selection, cancels old load, and starts a new load.
- Invalid date records `FUTURE_DATE_REQUIRED` and does not call the repository.
- Recommendation selection copies factual location fields into `draft.location`; scores are not submitted.
- Manual location clears recommendation selection and does not claim ranked evidence.
- Recommendation failure preserves date, reason, notes, and any unrelated draft state.

## 10. Concurrency

`WfaRequestViewModel` owns one `recommendationJob`, one monotonically increasing request ID, and the active query.

```text
start
→ cancel previous job
→ increment request ID
→ Loading
→ acquire coordinate
→ call use case
→ apply only when request ID and captured date still match current state
```

Cancellation alone is insufficient; every result application checks both request ID and date. An identical query already loading is not started twice.

## 11. UI

Form order:

```text
Top bar
Schedule date
Recommendation picker
Selected location summary
Employee
Reason and notes
Checklist
Continue to review
```

The picker renders loading, map/cards, empty, and typed failure states. Cards show name, address/type, distance, facility confidence, and final score only for ranked candidates. Incomplete evidence and enrichment failure have explicit non-zero copy.

Reuse the provider-neutral map adapter through `WfaRequestMapUiMapper`:

- current-location marker;
- recommendation markers;
- selected marker highlight;
- no geofence circle and no authoritative-target role.

Marker and card clicks emit the same selection event. Pass `minimumScheduleDate` to the existing controlled `DatePickerButton`.

## 12. Attendance cleanup

Remove from Attendance:

- recommendation use-case injection/job/fetch/retry;
- request recommendation state and selection;
- request search/map-pick controls;
- recommendation markers/loading overlay;
- coordinate-based booking navigation.

Keep current-location display, approved target, range, permission, face, and attendance submission behavior unchanged. Delete request-only helpers only after usage search proves they are dead.

## 13. Tests and verification

TDD coverage:

- Jakarta tomorrow/today/past/future date policy;
- field-level future-date validation;
- DTO mapping for ranked, insufficient, enrichment-failed, unknown, and malformed candidates;
- tri-state facilities and stable identity;
- ISO `schedule_date` request and typed failure mapping;
- default automatic load without route coordinates;
- date-change cancellation, selection reset, duplicate guard, and stale-result rejection;
- current-location failure and retry;
- non-ranked candidate selection;
- approved/pending/rejected/not-requested Attendance matrix;
- parent route without args and one graph-scoped controller;
- search result consumed once;
- nullable score never rendered as zero;
- form/review/submit/result regression.

Required commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

When an emulator/device is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 14. Acceptance criteria

- Selecting WFA auto-opens request creation only when no request exists.
- Approved WFA remains the Attendance target; pending/rejected/missing approval keep history recovery.
- Parent route has no coordinates.
- Initial/minimum date is tomorrow in Jakarta; today/past are blocked in UI and Domain.
- Recommendations automatically request `lat`, `lng`, and `schedule_date`.
- Date change cancels/invalidates old work, clears selection, and reloads.
- Stale responses cannot overwrite current state.
- Recommendation state belongs to `WfaRequestViewModel` only.
- New DTO/Domain models preserve nullable scores and explicit statuses.
- No active UI depends on fabricated score details.
- Ranked and incomplete-evidence presentation is honest.
- Manual search remains secondary and never masquerades as ranked evidence.
- Existing review/submission/result behavior remains intact.
- Relevant tests, build, and lint evidence are available before Done.
