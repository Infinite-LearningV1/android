# INF-218 — Android WFA Requests API + UI Spec

Date: 2026-07-07
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-wfa-requests-api-ui`
Branch: `fix/android-wfa-requests-api-ui`
Base: `develop` at `5438044`

## Scope

Implement the Android WFA bottom tab content for authenticated-user WFA request/history/status tracking using the backend contract from INF-225:

```http
GET /api/bookings/history
```

The WFA tab is a read/status surface for WFA booking requests. It must not become an active attendance owner.

## Product boundary

### WFA tab owns

- WFA booking/request list.
- WFA booking/request history.
- Pending / Approved / Rejected status tracking.
- Request detail display if available in the list payload.
- Navigation CTA to Attendance flow for creating WFA requests.
- All primary booking-history UI previously previewed from Home/dashboard for any role.

### Attendance screen remains owner of

- Creating WFA requests.
- Selecting WFA location.
- `LocationSearch`.
- `FaceScanner`.
- Check-in/check-out active attendance flow.

### Explicit non-goals

- Do not implement WFA creation in WFA tab.
- Do not implement check-in/check-out in WFA tab.
- Do not show Attendance History data.
- Do not show My Attendance Report data.
- Do not keep WFA booking-history cards/preview sections on Home/dashboard for Internship/Admin/Employee/Management roles.
- Do not show PDF export/reporting metrics.
- Do not show multi-user/employee booking data.
- Do not move WFA under Contact.
- Do not change backend.
- Do not change auth/session business logic.
- Do not change attendance check-in/check-out business logic.

## Navigation contract

Bottom navigation final contract follows INF-224:

```text
Home | History | WFA | Profile
```

`Screen.Wfa.route` must render the WFA Requests screen. `Open Attendance` CTA navigates to `Screen.Attendance.route` only; it must not trigger any check-in/check-out behavior.

## Backend contract

Endpoint:

```http
GET /api/bookings/history?page=1&limit=10&status=all&sort_by=created_at&sort_order=DESC
```

Supported status filter keys:

```text
all
pending
approved
rejected
```

Response semantics:

- `data.summary` = all-status counts for authenticated user.
- `data.bookings` = filtered result by selected status.
- `data.pagination` = pagination metadata for filtered `bookings` result.
- `data.filters` = backend echo of applied filters.

## Privacy and security

Backend response may contain identity fields:

- `user_id`
- `user_full_name`
- `user_email`
- `user_nip_nim`
- `user_position_name`
- `user_role_name`

MVP UI must not display email, NIP/NIM, or raw identity fields. Runtime/API evidence must not include raw tokens, raw auth-bearing requests, full raw response, email, NIP/NIM, or sensitive identifiers.

UI may use only:

- `booking_id`
- `schedule_date`
- `status` / `status_key` / `status_label`
- `location.description`
- `notes`
- `suitability_score`
- `suitability_label`
- `created_at`
- `processed_at`
- `approved_by` only if needed, but default MVP should not show approver.

## Android data model requirements

### Response DTOs

Existing `BookingHistoryResponse` must be extended or replaced in-place to safely match INF-225:

- Add nullable `summary` object.
- Add nullable-safe `bookings` list.
- Add nullable-safe `pagination`.
- Add `status_key` and `status_label` to booking item DTO.
- Keep identity fields parsed only if needed for compatibility, but do not map/display sensitive identity fields.
- Nullable fields must not crash UI.

Recommended DTO shape:

```kotlin
data class BookingHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: BookingData?,
    @SerializedName("message") val message: String?
)

data class BookingData(
    @SerializedName("summary") val summary: BookingSummaryData?,
    @SerializedName("bookings") val bookings: List<BookingItem> = emptyList(),
    @SerializedName("pagination") val pagination: PaginationData?,
    @SerializedName("filters") val filters: FilterData?
)
```

### Domain model

Domain model must support the WFA Requests UI without leaking identity fields:

- `BookingHistoryPage`
  - `bookings: List<BookingHistoryItem>`
  - `summary: BookingHistorySummary`
  - `pagination: BookingHistoryPagination`
- `BookingHistorySummary`
  - `total`
  - `pending`
  - `approved`
  - `rejected`
- `BookingHistoryPagination`
  - `currentPage`
  - `totalPages`
  - `totalItems`
  - `itemsPerPage`
  - `hasNextPage`
  - `hasPreviousPage`
- `BookingHistoryItem`
  - `bookingId`
  - `scheduleDateRaw`
  - formatted `scheduleDate`
  - `statusRaw`
  - `statusKey`
  - display `statusLabel`
  - `locationDescription`
  - `notes`
  - `suitabilityScore`
  - `suitabilityLabel`
  - `createdAtRaw`
  - display `createdAt`
  - `processedAtRaw`
  - display `processedAt`

## UI requirements

WFA Requests screen structure:

```text
WFA Requests
├── Top App Bar
│   ├── Title: WFA Requests
│   └── Subtitle: Track your Work From Anywhere submissions
├── Status Summary
│   ├── Pending
│   ├── Approved
│   └── Rejected
├── Filter Chips
│   ├── All
│   ├── Pending
│   ├── Approved
│   └── Rejected
├── Recent WFA Requests
│   ├── Schedule date
│   ├── Status chip
│   ├── Location
│   ├── Suitability score
│   ├── Suitability label
│   ├── Notes
│   ├── Submitted date
│   └── Processed date / waiting approval
└── Open Attendance CTA
    └── Navigate to Attendance flow
```

Design target should visually follow the provided reference:

- Purple brand accent.
- Light rounded cards.
- Three compact summary metric cards.
- Horizontal rounded filter chips.
- Request cards with left status accent, status chip, date, location, score/label, notes, submitted/processed metadata.
- Bottom purple CTA button.

Do not implement these reference-only elements for MVP:

- Large top hero illustration card.
- Long explanatory banner/card.
- Inline “No WFA requests yet” card under populated request cards.

## Empty/loading/error behavior

- Loading: show progress while first page is loading.
- Error: show user-facing error and retry action.
- Empty: show only when selected filter has no records.
- Empty text:
  - Title: `No WFA requests yet`
  - Body: `Create a WFA request from Attendance when you need to work from another location.`

## Nullable handling

- `suitability_score == null`: show `-` or `Not scored`.
- `suitability_label == null`: show `Not available` or omit label safely.
- `processed_at == null` and status pending: show `Waiting approval`.
- `processed_at == null` and status not pending: show `Not processed yet`.
- `approved_by == null`: do not show approver label for MVP.
- `location == null`: show `Location not available`.
- date parsing failure: display raw backend date string or `-`, never crash.

## UI state contract

Recommended filter model:

```kotlin
enum class WfaRequestStatusFilter(val key: String, val label: String) {
    All("all", "All"),
    Pending("pending", "Pending"),
    Approved("approved", "Approved"),
    Rejected("rejected", "Rejected")
}
```

On filter click:

1. Update selected filter.
2. Reset page to 1.
3. Call `GET /api/bookings/history` with `status = filter.key`.
4. Render `summary` from response unchanged.
5. Render `bookings` from filtered response.
6. Preserve pagination metadata for future load-more.

## Existing repo mapping

Observed in isolated worktree:

- `Screen.Wfa.route` exists.
- `MainContentNavGraph` renders `WfaHistoryScreen()` for `Screen.Wfa.route`.
- `ApiService.getBookingHistory()` already uses `GET("api/bookings/history")`.
- `BookingRepository`, `BookingRepositoryImpl`, and `GetBookingHistoryUseCase` exist.
- Existing WFA screen uses `HomeViewModel.bookingHistoryDetailsState`, not a dedicated WFA ViewModel.
- Existing DTO lacks `summary`, `status_key`, and `status_label`.
- Existing card/dropdown UI does not match target screen enough for INF-218.

## Acceptance criteria mapping

- Worktree isolated: already satisfied before implementation.
- WFA tab content: update `Screen.Wfa.route` content only, no Contact route.
- API call: reuse `ApiService.getBookingHistory` and make `status=all` explicit.
- Filters: chips call all/pending/approved/rejected.
- Summary: read from `data.summary`.
- List: read from `data.bookings`.
- Pagination: domain model stores pagination; optional load more can use `hasNextPage`.
- Nullable fields: mapper must guard all nullable backend fields.
- CTA: navigate to Attendance.
- Build: `./gradlew app:assembleDebug`.

## Verification requirements

Minimum local verification:

```bash
./gradlew app:assembleDebug
```

Recommended if time/environment allows:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime evidence required if possible:

- Login.
- Open WFA tab.
- Summary cards visible.
- Populated request list visible.
- Switch filters All/Pending/Approved/Rejected.
- Empty filter state visible when backend returns zero records.
- Open Attendance CTA navigates to Attendance screen.

If emulator/API runtime cannot be executed, mark as `Needs Verification`.

## Docs/ADR note

This work touches route-visible WFA tab behavior but should align with existing INF-224 contract. If no route ownership changes are made beyond rendering WFA Requests inside `Screen.Wfa.route`, ADR may be unnecessary. PR note must explicitly mention INF-224 alignment:

- Bottom bar remains `Home | History | WFA | Profile`.
- WFA tab is request/history/status list.
- Attendance remains creation/check-in/check-out owner.
