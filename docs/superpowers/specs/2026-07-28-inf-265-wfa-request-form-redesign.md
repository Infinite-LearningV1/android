# INF-265 — Android WFA Request Form Redesign Spec

Date: 2026-07-28  
Branch: `djangosuryaa/inf-265-android-redesign-wfa-request-form-with-review-and-result`  
Base: `develop` at `2191a6e0d06b99f8fdfc3e67f9408102d220f802`  
Linear: https://linear.app/infinite-track-palu/issue/INF-265/android-redesign-wfa-request-form-with-review-and-result-states

> This branch was created from `develop` through the GitHub connector. Before implementation, the execution agent must enter an isolated worktree for this branch in accordance with `CLAUDE.md`.

## 1. Goal

Redesign Android WFA request creation as one explicit transaction owned by Attendance:

```text
Selected WFA candidate location
→ Request form
→ Review request
→ Submitting
→ Success / Failure result
```

The redesign must preserve the repository's Clean Architecture, remove client-owned attendance policy, and consume the server-authoritative WFA configuration introduced by INF-270.

## 2. Scope boundary

### In scope

- Load global WFA radius and active request reasons from Backend.
- Render radius as read-only policy information.
- Collect schedule date, request reason, optional Other explanation, and optional notes.
- Show selected location and employee identity as read-only request context.
- Validate the editable draft before review.
- Render a review surface from the exact draft snapshot that will be submitted.
- Submit one backend mutation per attempt.
- Render backend-confirmed success data.
- Render typed failure states while preserving the draft for edit/retry.
- Create one nested graph and one graph-scoped ViewModel for the transaction.
- Add domain, data, presentation, navigation, and verification coverage required by this flow.

### Explicitly out of scope

- WFA request history, list, filters, or tracking dashboard.
- Contact-tab WFA entry point.
- Management Web settings or rejection flow.
- Backend implementation from INF-270.
- Radius per reason, user, division, or location.
- WFA approval logic.
- Attendance check-in/check-out redesign.
- Face verification, geofence runtime, auth/session, or generic navigation rewrite.
- Renaming the existing `data/soucre` package typo.
- Creating a generic application settings framework.

INF-214 and INF-218 remain owners of WFA request history/list surfaces. INF-270 remains the backend contract owner. INF-271 remains the Management Web owner.

## 3. Current `develop` mapping

### Current presentation

- `presentation/screen/attendance/booking/WfaBookingScreen.kt`
  - receives `NavHostController` directly;
  - mirrors success/error into Compose-local dialog state;
  - renders one full-screen `WfaBookingDialog`;
  - navigates to Home directly after success.
- `presentation/screen/attendance/booking/WfaBookingViewModel.kt`
  - reads latitude/longitude from `SavedStateHandle`;
  - loads the logged-in user;
  - reverse-geocodes the candidate coordinate;
  - owns primitive form fields;
  - formats the date in Presentation;
  - silently falls back to the current date when parsing fails;
  - returns `isBookingSuccessful: Boolean` and `error: String?`.
- `presentation/components/dialog/WfaBookingDialog.kt`
  - allows the employee to edit radius;
  - combines employee, location, schedule, notes, and submit action in one dialog;
  - contains screen-local styling and preview state.

### Current domain

- `domain/use_case/booking/SubmitWfaBookingUseCase.kt`
  - accepts six primitive parameters;
  - forwards them directly to the repository;
  - returns `Result<Unit>`.
- `domain/repository/BookingRepository.kt`
  - mixes booking history and WFA submission;
  - submission accepts client-provided radius and description primitives;
  - submission loses backend-confirmed result data.

### Current data

- `data/soucre/network/request/BookingRequest.kt`
  - sends `schedule_date`, coordinates, radius, description, and notes.
- `data/soucre/network/response/booking/BookingResponse.kt`
  - has a minimal result DTO but is discarded by the repository.
- `data/repository/booking/BookingRepositoryImpl.kt`
  - constructs the request DTO from primitives;
  - maps success to `Unit`;
  - converts HTTP/provider failures into generic `Exception` messages.
- `data/soucre/network/retrofit/ApiService.kt`
  - exposes `POST api/bookings` but no WFA request-config endpoint.

### Current navigation

- `Screen.WfaBooking` is `wfa_booking/{latitude}/{longitude}`.
- `MainContentNavGraph.kt` registers WFA booking as one destination.
- Route arguments are declared as `NavType.FloatType`, reducing coordinate precision.
- No nested WFA request transaction graph exists.

## 4. Locked product and source-of-truth decisions

- Backend is authoritative for global WFA radius, active request reasons, date policy, booking conflicts, final transaction acceptance, status, user identity, suitability, and server timestamps.
- Android owns editable form state, fast local validation, transaction presentation state, and device-side navigation.
- The selected map/search result is a `WfaCandidateLocation`, not an approved attendance target.
- Employee name and division are display-only and must not be submitted as authoritative identity.
- Android must not send radius, status, user ID, suitability score, suitability label, or created timestamp.
- Android must send date as ISO `YYYY-MM-DD`; no alternate output format and no fallback to today.
- Success exists only after the backend confirms booking creation.
- Review is not a second source of truth. It renders an immutable snapshot of the current draft.
- Failure preserves the draft unless the user explicitly abandons the flow.

## 5. Adopted architecture

Preserve the repository contract:

```text
Screen → ViewModel → UseCase → Repository Interface → RepositoryImpl → API
```

Target feature structure:

```text
WfaRequestHost
├── WfaRequestFormScreen
├── WfaRequestReviewScreen
└── WfaRequestResultScreen
        │
        ▼
WfaRequestViewModel
├── LoadWfaRequestConfigUseCase
├── ValidateWfaRequestDraftUseCase
└── SubmitWfaRequestUseCase
        │
        ▼
BookingRepository
        │
        ▼
BookingRepositoryImpl
├── WfaRequestDtoMapper
├── WfaRequestResponseMapper
├── WfaRequestFailureMapper
└── ApiService
```

This is a bounded evolution of the existing booking path. Do not introduce a parallel repository solely for INF-265. `BookingRepository` remains the owner unless implementation proves it materially oversized.

## 6. Navigation and transaction ownership

Create one nested graph:

```text
WfaRequestGraph
├── WfaRequestForm
├── WfaRequestReview
└── WfaRequestResult
```

Rules:

- One graph-scoped `WfaRequestViewModel` owns one request session.
- Form, review, and result obtain the same ViewModel from the parent graph back-stack entry.
- Screens do not receive or retain `NavController`.
- Screens expose semantic callbacks; the graph host performs navigation.
- ViewModel emits one-time navigation/announcement effects; persistent state remains in `StateFlow`.
- Draft and result objects are not serialized through routes.
- `SavedStateHandle` is limited to typed route input/bootstrap state and is not used as a free-form event bus.
- Candidate location route input must use semantic identifiers when available. The transitional coordinate contract must use sufficient precision and must not pass DTOs or unrestricted JSON.
- Each destination is registered once under one graph owner.

Recommended routes:

```text
wfa_request
wfa_request/form
wfa_request/review
wfa_request/result
```

The existing `Screen.WfaBooking.createRoute(latitude, longitude)` may remain as a temporary compatibility entry that forwards into the new graph during migration, then must be removed once all call sites use the new route contract.

## 7. Presentation contract

### Screen responsibility

Screens may only:

- render `WfaRequestUiState`;
- send typed `WfaRequestEvent` values;
- invoke semantic navigation callbacks supplied by the graph host;
- render localized presentation copy and reusable components.

Screens must not:

- construct DTOs;
- call APIs, repositories, or RepositoryImpl;
- parse backend error bodies;
- format business dates for transport;
- own booking policy;
- use Compose-local state as transaction truth.

### State model

```kotlin
sealed interface WfaRequestPhase {
    data object Loading : WfaRequestPhase
    data object Editing : WfaRequestPhase
    data object ReadyForReview : WfaRequestPhase
    data object Reviewing : WfaRequestPhase
    data object Submitting : WfaRequestPhase
    data object Success : WfaRequestPhase
    data object Failure : WfaRequestPhase
}
```

```kotlin
data class WfaRequestUiState(
    val phase: WfaRequestPhase = WfaRequestPhase.Loading,
    val employee: EmployeeSummary? = null,
    val location: WfaCandidateLocation? = null,
    val config: WfaRequestConfig? = null,
    val draft: WfaRequestDraft = WfaRequestDraft.Empty,
    val fieldErrors: WfaRequestFieldErrors = WfaRequestFieldErrors(),
    val submitResult: SubmittedWfaRequest? = null,
    val failure: WfaRequestFailure? = null
)
```

State rules:

- Only `Editing` accepts field mutation events.
- `ReadyForReview` indicates that local validation passed.
- `Reviewing` renders the same draft values without mutation.
- `Submitting` is entered before repository mutation.
- Repeated confirm events while `Submitting` are ignored.
- `Success` contains a backend-confirmed `SubmittedWfaRequest`.
- `Failure` contains a typed failure and preserves the draft.
- Config-load failure is retryable and cannot expose an editable radius fallback.

### One-time effects

```kotlin
sealed interface WfaRequestEffect {
    data object OpenReview : WfaRequestEffect
    data object ReturnToForm : WfaRequestEffect
    data object ReturnToAttendance : WfaRequestEffect
    data object ReturnHome : WfaRequestEffect
    data class Announce(val message: UiText) : WfaRequestEffect
}
```

Effects must not be represented by booleans such as `isBookingSuccessful` or generic `error: String?` fields.

### Events

```kotlin
sealed interface WfaRequestEvent {
    data class ScheduleDateChanged(val date: LocalDate?) : WfaRequestEvent
    data class ReasonSelected(val reasonId: Long) : WfaRequestEvent
    data class OtherReasonChanged(val value: String) : WfaRequestEvent
    data class NotesChanged(val value: String) : WfaRequestEvent
    data object ReviewClicked : WfaRequestEvent
    data object EditClicked : WfaRequestEvent
    data object SubmitConfirmed : WfaRequestEvent
    data object RetryConfigClicked : WfaRequestEvent
    data object RetrySubmitClicked : WfaRequestEvent
}
```

There is no radius-edit event.

## 8. Domain contract

### Configuration

```kotlin
data class WfaRequestConfig(
    val radiusMeters: Int,
    val reasons: List<WfaRequestReason>
)

data class WfaRequestReason(
    val id: Long,
    val label: String,
    val isOther: Boolean
)
```

Configuration invariants:

- `radiusMeters > 0`;
- reason identifiers are unique;
- only active reasons returned by Backend are selectable;
- at most one selectable reason is marked `isOther`.

### Candidate location

```kotlin
data class WfaCandidateLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val formattedAddress: String
)
```

Coordinates must remain valid geographic values. Display text may fall back to formatted coordinates if reverse geocoding fails, but the candidate must not silently become an approved target.

### Draft

```kotlin
data class WfaRequestDraft(
    val scheduleDate: LocalDate?,
    val reasonId: Long?,
    val otherReasonText: String,
    val notes: String,
    val location: WfaCandidateLocation?
) {
    companion object {
        val Empty = WfaRequestDraft(
            scheduleDate = null,
            reasonId = null,
            otherReasonText = "",
            notes = "",
            location = null
        )
    }
}
```

Draft must not contain radius, status, user ID, authoritative suitability, or server timestamps.

### Validation

```kotlin
data class WfaRequestFieldErrors(
    val scheduleDate: WfaRequestFieldError? = null,
    val reason: WfaRequestFieldError? = null,
    val otherReason: WfaRequestFieldError? = null,
    val notes: WfaRequestFieldError? = null,
    val location: WfaRequestFieldError? = null
)

sealed interface WfaRequestValidationResult {
    data class Valid(val command: SubmitWfaRequestCommand) : WfaRequestValidationResult
    data class Invalid(val errors: WfaRequestFieldErrors) : WfaRequestValidationResult
}
```

Android Domain validates fast-feedback requirements:

- date selected;
- reason selected and present in current config;
- Other explanation present when selected reason is Other;
- Other explanation and notes respect server-provided/current contract limits when available;
- candidate location present and coordinates valid.

Backend remains authoritative for past/same-day rules, active reason status at mutation time, booking conflicts, global radius, and final acceptance.

### Submit command

```kotlin
data class SubmitWfaRequestCommand(
    val scheduleDate: LocalDate,
    val reasonId: Long,
    val otherReasonText: String?,
    val notes: String?,
    val location: WfaCandidateLocation
)
```

### Submitted result

```kotlin
data class SubmittedWfaRequest(
    val bookingId: Long,
    val scheduleDate: LocalDate,
    val status: WfaRequestStatus,
    val location: WfaCandidateLocation,
    val reasonLabel: String,
    val radiusMeters: Int,
    val submittedAt: Instant?
)
```

Result-page data must come from Backend response mapping. Local draft values may be used only as defensive display fallback when the locked backend contract explicitly omits a field; such fallback must never imply backend persistence beyond the confirmed success response.

### Result and failure

```kotlin
sealed interface WfaRequestResult {
    data class Success(val request: SubmittedWfaRequest) : WfaRequestResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestResult
}
```

```kotlin
sealed interface WfaRequestFailure {
    data object ConfigUnavailable : WfaRequestFailure
    data object InvalidDate : WfaRequestFailure
    data object ReasonUnavailable : WfaRequestFailure
    data object DuplicateRequest : WfaRequestFailure
    data object NetworkUnavailable : WfaRequestFailure
    data object ServerUnavailable : WfaRequestFailure
    data class ValidationRejected(
        val fieldErrors: Map<String, String>
    ) : WfaRequestFailure
    data class BackendRejected(
        val safeMessage: String
    ) : WfaRequestFailure
    data object Unknown : WfaRequestFailure
}
```

Raw DTOs, Retrofit exceptions, response bodies, and provider errors must not cross into Domain or Presentation.

## 9. Use cases

### `LoadWfaRequestConfigUseCase`

- Calls `BookingRepository.getWfaRequestConfig()`.
- Rejects unusable config such as non-positive radius or empty active reason list as `ConfigUnavailable`.
- Does not invent local reasons or radius defaults.

### `ValidateWfaRequestDraftUseCase`

- Is a pure domain validator.
- Receives the current draft and config.
- Returns field-local errors or a typed `SubmitWfaRequestCommand`.
- Normalizes optional blank text to `null` in the command boundary.
- Never calls the repository.

### `SubmitWfaRequestUseCase`

- Accepts only a validated `SubmitWfaRequestCommand`.
- Delegates one mutation to `BookingRepository.submitWfaRequest(command)`.
- Does not reconstruct DTOs or parse transport failures.
- Returns typed success/failure unchanged or with only domain-level normalization.

## 10. Repository and data contract

Keep `BookingRepository`:

```kotlin
interface BookingRepository {
    suspend fun getWfaRequestConfig(): WfaRequestConfigResult

    suspend fun submitWfaRequest(
        command: SubmitWfaRequestCommand
    ): WfaRequestResult

    // Existing booking-history methods remain unchanged for INF-265.
}
```

`WfaRequestConfigResult` must be typed rather than generic `Result`:

```kotlin
sealed interface WfaRequestConfigResult {
    data class Success(val config: WfaRequestConfig) : WfaRequestConfigResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestConfigResult
}
```

### Retrofit contract from INF-270

```http
GET /api/wfa/request-config
POST /api/bookings
```

Expected create payload:

```json
{
  "schedule_date": "2026-08-10",
  "request_reason_id": 1,
  "request_other_reason": null,
  "notes": "Pertemuan project",
  "latitude": -0.9001,
  "longitude": 119.877
}
```

Android must not send:

```text
radius
user_id
status
suitability_score
suitability_label
created_at
```

### DTO ownership

Data owns:

- config response DTOs;
- submit request/response DTOs;
- `SubmitWfaRequestCommand → WfaRequestDto` mapping;
- `LocalDate → ISO YYYY-MM-DD` conversion;
- blank optional values to `null` conversion;
- response DTO to domain mapping;
- HTTP/provider error classification.

Recommended request DTO:

```kotlin
data class WfaRequestDto(
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("request_reason_id") val reasonId: Long,
    @SerializedName("request_other_reason") val otherReasonText: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
```

### Failure mapping

`WfaRequestFailureMapper` must map stable backend codes when present and use status/message only as a guarded fallback:

```text
DUPLICATE_BOOKING → DuplicateRequest
INVALID_DATE_FORMAT / INVALID_DATE_VALUE / PAST_DATE_NOT_ALLOWED / SAME_DAY_NOT_ALLOWED → InvalidDate
REQUEST_REASON_NOT_FOUND / REQUEST_REASON_INACTIVE → ReasonUnavailable
HTTP 5xx → ServerUnavailable
IOException / timeout / unknown-host → NetworkUnavailable
recognized validation body → ValidationRejected
recognized safe business rejection → BackendRejected
anything else → Unknown
```

Presentation maps `WfaRequestFailure` to localized `UiText`. Repository must not return `Exception(errorMessage)` as the public contract.

## 11. Initial-load orchestration

`WfaRequestViewModel` initial loading combines three bounded inputs:

```text
logged-in employee snapshot
candidate location bootstrap + address resolution
server WFA request config
```

Rules:

- Employee profile is read once through the existing bounded `GetLoggedInUserUseCase` pattern.
- Reverse geocoding is presentation orchestration through the existing domain use case; provider-specific types must not enter UI state.
- Config failure blocks editing and exposes retry.
- Employee/address fallback errors may be shown safely without converting them into backend transaction success/failure.
- No raw exception message is displayed.
- The ViewModel must avoid nested long-lived collection for one-shot prefill.

## 12. UX surfaces

### Form

Render:

- selected location summary;
- employee summary;
- server-provided radius as read-only information;
- schedule-date field;
- server-provided reason selector;
- Other explanation field only when the selected reason is Other;
- optional notes;
- one contextual primary CTA: `Review Permintaan`.

### Review

Render an immutable summary of:

- employee;
- location;
- radius policy;
- schedule date;
- request reason;
- Other explanation when present;
- notes when present.

Actions:

```text
Ubah Data
Kirim Permintaan
```

### Submitting

- Show honest progress copy.
- Disable submit and editable controls.
- Ignore repeated confirm events.
- Back/close must not trigger another mutation.

### Success result

Render backend-confirmed:

- booking/request ID;
- pending status;
- schedule date;
- selected location;
- request reason;
- applied radius when provided;
- actions to Attendance and Home.

Do not add WFA history/list navigation.

### Failure result

Render typed safe feedback with:

- `Perbaiki & Coba Lagi` for field/business correction;
- `Coba Lagi` for retryable transport failures;
- `Kembali` to preserve user control.

Failure must not trigger downstream success refresh or navigation.

## 13. UI component strategy

Reuse current Infinite Track Material 3 tokens/components from INF-219 and INF-222. Do not create a parallel design system.

Feature-local reusable components may include:

```text
WfaSelectedLocationCard
WfaEmployeeSummary
WfaRequestPolicyInfo
WfaRequestDateField
WfaRequestReasonField
WfaRequestNotesField
WfaRequestReviewSummary
WfaRequestSubmittingSurface
WfaRequestResultSurface
```

The legacy `WfaBookingDialog` should be removed after the new graph is wired and no call sites remain. Its generic location under `presentation/components/dialog` must not be retained as a second implementation of the same transaction.

## 14. Migration strategy

1. Add domain contracts and pure validation tests.
2. Add data DTOs, mappers, failure mapper, and repository tests against the INF-270 contract.
3. Extend `ApiService` with config and typed submit contracts.
4. Add ViewModel state/event/effect orchestration with unit tests.
5. Add form/review/result presentation surfaces.
6. Add nested graph and migrate all WFA booking navigation call sites.
7. Remove legacy dialog/screen/ViewModel/request DTO only after the new route compiles and tests pass.
8. Run compile, unit, lint, assemble, instrumentation compile, and device/runtime checks.

No compatibility bridge may preserve editable radius or silent date fallback.

## 15. Testing contract

### Domain

- valid draft produces the exact typed command;
- missing date, reason, location, and Other explanation produce field-local errors;
- blank optional fields normalize to `null`;
- inactive/missing reason is rejected against current config;
- invalid coordinates are rejected;
- radius never appears in draft or submit command.

### Data

- config DTO maps radius and reasons safely;
- command maps date to ISO `YYYY-MM-DD`;
- submit payload excludes radius and suitability;
- backend success maps to `SubmittedWfaRequest`;
- known backend codes map to typed failures;
- network and server failures remain distinguishable;
- raw backend/provider types do not leak.

### ViewModel

- initial load combines employee, candidate, address, and config once;
- config failure produces retryable blocked state;
- only Editing mutates fields;
- review uses the exact validated snapshot;
- edit preserves draft;
- submit enters `Submitting` before repository mutation;
- repeated confirm invokes repository once;
- success stores backend result and emits one navigation/announcement effect;
- failure preserves draft and does not emit success effects;
- retry creates one new attempt.

### Navigation/UI

- one graph owner and one graph-scoped ViewModel;
- form → review → edit preserves state;
- form → review → submit → result has no duplicate destinations/events;
- radius is visible but not editable;
- Other field visibility follows server reason metadata;
- narrow-device and accessibility font-scale layout remains usable;
- process recreation does not create a duplicate submit.

## 16. Verification gate

Required repository evidence:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lintDebug
./gradlew app:assembleDebug
git diff --check
```

Required runtime evidence when environment is available:

1. Open Attendance and select WFA candidate location.
2. Confirm server radius is read-only.
3. Confirm active request reasons load from Backend.
4. Confirm Other requires explanation.
5. Confirm invalid date cannot reach review.
6. Confirm review matches form draft.
7. Confirm one confirm action creates one backend request.
8. Confirm success appears only after backend response.
9. Confirm network/server failure preserves the draft.
10. Confirm returning to Attendance/Home does not replay the mutation.

Because this is UI, navigation, attendance, and transaction work, build-only evidence is insufficient. Missing device/backend runtime evidence must be reported as `Needs Verification`, not Done.

## 17. Risks and mitigations

### Backend contract not merged

INF-265 is blocked by INF-270. Do not invent fallback radius/reason catalogs. Use contract fakes in tests; production implementation waits for the locked endpoint schema.

### Existing WFA callers use the old route

Search all `Screen.WfaBooking.createRoute` call sites, migrate them in the navigation task, and delete the compatibility route only after zero call sites remain.

### Duplicate submit through recomposition or process state

Use one in-flight guard in ViewModel state, enter `Submitting` before invoking the use case, and test repeated events. Backend idempotency remains a separate backend concern.

### Result page displays unconfirmed draft data

Map and render backend response. Use local values only as explicitly documented defensive display fallback, never as proof of persistence.

### Booking history regressions

Keep existing booking-history repository methods and domain models unchanged. INF-265 must modify only submission/config contracts.

### Over-broad refactor

Do not rename `data/soucre`, replace Hilt, split the application module, or redesign unrelated Attendance/navigation flows.

## 18. Definition of Done

- WFA request creation is implemented as Form → Review → Submit → Result under one nested graph.
- One graph-scoped ViewModel owns the transaction state.
- Screen, Domain, Data, DI, and Navigation boundaries match this spec.
- Radius and reasons are loaded from Backend; radius is not editable or submitted.
- Date fallback is removed and ISO mapping lives in Data.
- Submission uses typed command/result/failure contracts, not primitives or `Result<Unit>`.
- Result is backend-confirmed.
- Duplicate submit and duplicate navigation effects are prevented and tested.
- Legacy WFA dialog/screen/ViewModel path is removed after migration.
- WFA history/list and Contact work are not mixed into the change.
- Required tests/build/lint checks pass.
- Device/backend runtime evidence is attached, or remaining gaps are explicitly marked `Needs Verification`.
