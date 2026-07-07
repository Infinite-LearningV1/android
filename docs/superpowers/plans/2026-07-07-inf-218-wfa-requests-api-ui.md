# INF-218 — Android WFA Requests API + UI Implementation Plan

Date: 2026-07-07
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-wfa-requests-api-ui`
Branch: `fix/android-wfa-requests-api-ui`
Base: `develop` at `5438044`
Spec: `docs/superpowers/specs/2026-07-07-inf-218-wfa-requests-api-ui.md`

## Current mapping

1. Main checkout `E:\skrisi\android` is on `develop` at `5438044`, with one unrelated untracked docs plan. Do not edit main checkout.
2. Isolated worktree is ready and clean:
   - Path: `C:\Users\Febriyadi\.claude\worktrees\android-wfa-requests-api-ui`
   - Branch: `fix/android-wfa-requests-api-ui`
3. Existing relevant code:
   - `presentation/navigation/Screen.kt` has `Screen.Wfa` and `Screen.Attendance`.
   - `presentation/navigation/MainContentNavGraph.kt` maps `Screen.Wfa.route` to `WfaHistoryScreen()`.
   - `data/soucre/network/retrofit/ApiService.kt` already has `GET("api/bookings/history")`.
   - `data/soucre/network/response/booking/BookingHistoryResponse.kt` lacks INF-225 summary/status-label fields.
   - `data/repository/booking/BookingRepositoryImpl.kt` maps only bookings + hasNextPage.
   - `domain/model/booking/BookingHistoryPage.kt` only stores bookings + hasNextPage.
   - `domain/model/booking/BookingHistoryItem.kt` lacks score/date/processed/status label fields needed by UI.
   - Existing WFA screen is tied to `HomeViewModel`; plan should introduce a dedicated WFA ViewModel for separation.

## Implementation strategy

Reuse the existing booking API/repository path instead of creating a duplicate service. Extend the existing booking history DTO/domain model to match INF-225, then add a dedicated WFA presentation layer for the bottom tab.

This avoids changing backend, auth/session logic, and attendance business logic.

Additional product decision from 2026-07-07 execution follow-up: remove WFA booking-history preview/cards from Home/dashboard for all roles. The WFA bottom tab becomes the single primary booking-history/status surface.

Additional UI architecture decision: request/status colors must be global presentation/theme tokens instead of hardcoded screen-local `Color(0x...)` mappings. Pull-to-refresh is handled by `WfaRequestsUiState.isRefreshing` in `StateFlow` so refresh loading is separate from first-page loading.

## Step 1 — Extend backend DTOs safely

Files:

- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/BookingHistoryResponse.kt`

Changes:

- Make response nullable-safe:
  - `message: String?`
  - `data: BookingData?`
- Add `BookingSummaryData` with `total`, `pending`, `approved`, `rejected`.
- Add `status_key` and `status_label` to `BookingItem`.
- Make nullable backend fields safe:
  - `scheduleDate: String?`
  - `status: String?`
  - `location: BookingLocation?`
  - `createdAt: String?`
  - keep `suitabilityScore` nullable; prefer `Double?` or keep `Float?` consistently with mapper.
- Make `BookingData.bookings` default to empty list.
- Make `pagination` and `filters` nullable with mapper fallback.

Checkpoint:

- DTO can parse sample INF-225 response including `summary`, `status_key`, `status_label`.
- DTO can parse missing/null optional fields without throwing before mapper.

## Step 2 — Extend domain models

Files:

- `app/src/main/java/com/example/infinite_track/domain/model/booking/BookingHistoryItem.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/booking/BookingHistoryPage.kt`

Changes:

- Add `BookingHistorySummary`.
- Add `BookingHistoryPagination`.
- Extend `BookingHistoryPage` to include:
  - `summary`
  - `pagination`
  - keep `hasNextPage` compatibility if existing callers rely on it.
- Extend `BookingHistoryItem` for WFA UI fields:
  - `statusKey`
  - `statusLabel`
  - `suitabilityScore`
  - `createdAtRaw`
  - `createdAt`
  - `processedAtRaw`
  - `processedAt`

Compatibility rule:

- Preserve existing constructor defaults where possible so existing `HomeViewModel`, `DetailsMyBooking`, and preview code continue compiling.

Checkpoint:

- Existing callers still compile or need minimal updates only.

## Step 3 — Update mapper

Files:

- `app/src/main/java/com/example/infinite_track/data/mapper/booking/BookingMapper.kt`

Changes:

- Map `BookingHistoryResponse` / `BookingData` to `BookingHistoryPage` with fallback summary/pagination values.
- Map `BookingItem` to `BookingHistoryItem` safely:
  - location null -> `Location not available`
  - schedule date null -> `-`
  - status key: prefer `status_key`, fallback `status`, fallback `unknown`
  - status label: prefer `status_label`, fallback formatted status key
  - suitability score null remains null
  - suitability label null -> `Not available`
  - created date null -> `-`
  - processed null pending -> `Waiting approval`
  - processed null non-pending -> `Not processed yet`
- Add date formatting helpers for:
  - `yyyy-MM-dd`
  - `yyyy-MM-dd HH:mm:ss`
- Keep mapper free of logging raw response or identity fields.

Checkpoint:

- Mapper has no unsafe `!!` or direct non-null access to nullable backend fields.

## Step 4 — Update repository response mapping

Files:

- `app/src/main/java/com/example/infinite_track/data/repository/booking/BookingRepositoryImpl.kt`
- optionally `domain/repository/BookingRepository.kt`
- optionally `domain/use_case/booking/GetBookingHistoryUseCase.kt`

Changes:

- Continue calling existing `apiService.getBookingHistory`.
- Pass explicit `status = "all"` from WFA ViewModel when All filter selected. Existing top-card caller may still use null if desired.
- Convert full response to `BookingHistoryPage`, not only `bookings + hasNextPage`.
- If `success == false`, return failure using `message` fallback.
- For API/network failure, preserve existing user-safe failure messages.

Checkpoint:

- Repository returns summary + pagination for WFA ViewModel.

## Step 5 — Create WFA UI state + ViewModel

Files to add:

- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaRequestsUiState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaRequestsViewModel.kt`

Changes:

- Add `WfaRequestStatusFilter` enum with explicit backend keys.
- Add UI state:
  - `selectedFilter`
  - `summary`
  - `bookings`
  - `pagination`
  - `isLoading`
  - `isLoadingMore` if implementing load more now
  - `errorMessage`
- On init, load `All` status.
- On filter click:
  - set selected filter
  - reset page to 1
  - load with `status = filter.key`
- Retry reloads current selected filter.
- Optional: implement future-ready load more using pagination `hasNextPage`, but do not overcomplicate MVP.

Checkpoint:

- WFA screen no longer depends on `HomeViewModel` for bottom tab content.
- Filter keys are controlled and cannot send arbitrary UI labels.

## Step 6 — Build target UI components

Files to add:

- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/components/WfaRequestStatusSummary.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/components/WfaRequestFilterChips.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/components/WfaRequestCard.kt`
- optional local helpers in same files if compact.

Design requirements:

- Follow reference image style:
  - purple accent
  - rounded white cards
  - compact summary metric cards
  - horizontal chips
  - card with status accent strip and status pill
  - bottom purple CTA
- Do not add hero illustration card.
- Do not add long explanatory banner/card.
- Do not show empty card under populated list.

Checkpoint:

- Cards display required data:
  - schedule date
  - status chip
  - location
  - suitability score/label safely
  - notes
  - submitted date
  - processed state/date

## Step 7 — Replace WFA bottom tab screen content

Files:

- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaHistoryScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaBookingHistoryContent.kt` or replace with new `WfaRequestsScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`

Preferred approach:

- Add `WfaRequestsScreen` with `WfaRequestsViewModel`.
- Keep `WfaHistoryScreen` as a thin wrapper or rename usage carefully.
- Update `MainContentNavGraph` to pass `onOpenAttendance = { navController.safeNavigate(Screen.Attendance.route) }`.

Checkpoint:

- `Screen.Wfa.route` still owns WFA bottom tab content.
- `Open Attendance` only navigates to `Screen.Attendance.route`.
- No Contact route usage for WFA.

## Step 8 — Add tests where feasible

Preferred tests:

- Mapper unit test for INF-225 sample payload semantics.
- Mapper unit test for null suitability/processed/location/date fallback.

Candidate test files:

- `app/src/test/java/com/example/infinite_track/data/mapper/booking/BookingMapperWfaRequestsContractTest.kt`

If test setup is too costly, at minimum keep implementation mapper pure and verify compile; mark test coverage as `Needs Verification`.

Checkpoint:

- Tests do not include sensitive raw identity values beyond synthetic placeholders.

## Step 9 — Verification

Run from worktree:

```bash
cd C:/Users/Febriyadi/.claude/worktrees/android-wfa-requests-api-ui
./gradlew app:assembleDebug
```

If time/environment allows:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime/emulator checklist if possible:

1. Login.
2. Open WFA tab.
3. Confirm title/subtitle.
4. Confirm summary cards display backend `summary`.
5. Confirm All filter calls/loads `status=all`.
6. Confirm Pending filter calls/loads `status=pending`.
7. Confirm Approved filter calls/loads `status=approved`.
8. Confirm Rejected filter calls/loads `status=rejected`.
9. Confirm empty state appears only for empty selected filter.
10. Confirm Open Attendance CTA navigates to Attendance screen.

Evidence redaction:

- Do not paste token.
- Do not paste raw auth-bearing request.
- Do not paste email/NIP/NIM/full name if logs/screenshots show them.

## Step 10 — Review and final report

Before claiming complete:

- Run git diff/stat.
- Run verification commands.
- Request code review if available.
- Report using Android required categories:
  - Fact
  - Assumption
  - Mismatch
  - Risk
  - Needs Verification
  - Recommendation
  - Files/areas affected
  - Verification evidence
  - Docs/ADR update note
  - PR/review note

## Risks and mitigations

### Risk: Existing `HomeViewModel` callers regress

Mitigation:

- Preserve domain model defaults.
- Keep repository method signature unchanged unless required.
- Do not remove existing booking history path used by Home/DetailsMyBooking.

### Risk: Backend nullable fields crash UI

Mitigation:

- DTO nullable-safe.
- Mapper fallback values.
- UI never assumes score/date/location non-null.

### Risk: WFA tab accidentally becomes Attendance owner

Mitigation:

- CTA only navigates to Attendance.
- No check-in/check-out usecase injection in WFA ViewModel.
- No attendance history/report data rendered.

### Risk: Runtime/API evidence not available locally

Mitigation:

- Mark runtime as `Needs Verification`.
- Provide build/test evidence separately.

### Risk: INF-222 component API changes

Mitigation:

- Use stable Material3 primitives and existing theme tokens.
- Keep screen-specific components compact and easy to migrate.

## Commit/PR note draft

```text
Implements INF-218 WFA Requests as WFA bottom tab content aligned with INF-224.

- Uses existing GET /api/bookings/history API path with INF-225 summary/bookings/pagination contract.
- Adds All/Pending/Approved/Rejected filters with explicit backend status keys.
- Renders backend summary counts separately from filtered request list.
- Handles nullable suitability score/label and processed timestamp safely.
- Adds WFA request cards, summary cards, filter chips, loading/error/empty states, pull-to-refresh, and Open Attendance CTA.
- Adds reusable `InlineRefreshingIndicator` for dashboard/page reuse.
- Adds global request status color tokens/resolver for reusable status components.
- Removes WFA booking-history preview/cards from Home/dashboard for all roles.
- Keeps Attendance screen as owner for WFA creation and check-in/check-out flows.
- Does not move WFA under Contact.
- Does not change backend, auth/session business logic, or attendance business logic.

Verification:
- ./gradlew app:assembleDebug: <result>
- ./gradlew app:test: <result or Needs Verification>
- ./gradlew app:lint: <result or Needs Verification>
- Runtime/emulator WFA tab evidence: <result or Needs Verification>
```
