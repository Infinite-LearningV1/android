# INF-229 — Android Home Today Status Dashboard Implementation Plan

## Status

Draft plan — created after brainstorming approval on 2026-07-08.

## Scope

Implement/refactor Android Home dashboard so it consumes backend `status-today` through the existing clean architecture path and renders a factual Today Status dashboard.

This plan is bounded to Android Home/data model/UI changes. It must not change backend contract, auth/session business logic, attendance action business logic, geofence lifecycle ownership, or bottom navigation.

## Branch / Worktree

Work only in:

```text
C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard
```

Branch:

```text
fix/android-home-today-status-dashboard
```

Do not edit main checkout:

```text
E:/skrisi/android
```

Main checkout was observed dirty on 2026-07-08 and must remain untouched for this task.

## Reference Spec

- `docs/superpowers/specs/2026-07-08-inf-229-android-home-today-status-dashboard.md`

## Current Evidence Summary

Before implementation:

- Isolated worktree exists, branch is correct, working tree is clean.
- `HomeScreen` is in `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeScreen.kt`.
- `HomeViewModel` is in `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeViewModel.kt`.
- `GetTodayStatusUseCase(forceRefresh)` exists.
- `AttendanceRepository.getTodayStatus(forceRefresh)` exists.
- `ApiService.getTodayStatus()` exists for `GET /api/attendance/status-today`.
- `HomeViewModel` does not yet consume `GetTodayStatusUseCase`.
- `TodayStatusResponse`/domain/mapper do not yet fully align with `checked_in_at_iso`, `checked_out_at_iso`, `work_duration_seconds`, and backend TTL meta.
- Home currently uses `HomeViewModel`, not `AttendanceViewModel`.
- INF-222 reusable design components are available.

## Phase 0 — Guardrails

1. Confirm worktree and branch before editing:

   ```bash
   git -C "C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard" status --short
   git -C "C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard" branch --show-current
   ```

2. Confirm no accidental main checkout edits.
3. Keep all changes inside isolated worktree.
4. Do not touch high-risk network/auth/session/geofence files unless required by compile evidence; if unexpectedly required, stop and report risk.

## Phase 1 — Align TodayStatus contract model

Files:

- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/TodayStatusResponse.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- `app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendanceMapper.kt`

Tasks:

1. Add DTO fields to `TodayStatusData`:

   ```kotlin
   @field:SerializedName("checked_in_at_iso")
   val checkedInAtIso: String? = null

   @field:SerializedName("checked_out_at_iso")
   val checkedOutAtIso: String? = null

   @field:SerializedName("work_duration_seconds")
   val workDurationSeconds: Long? = null
   ```

2. Add domain fields to `TodayStatus`:

   ```kotlin
   val checkedInAtIso: String? = null
   val checkedOutAtIso: String? = null
   val workDurationSeconds: Long? = null
   ```

3. Update `TodayStatusResponse.toDomain()`:

   - Prefer `meta?.cacheTtlSeconds`.
   - Fallback to `AuthRuntimePolicy.SHARED_TTL_SECONDS`.

4. Update `TodayStatusData.toDomain()` to map new fields.

Acceptance:

- Existing repository cache path still compiles.
- Existing callers that construct `TodayStatus` still compile due to default values, or are updated if needed.
- TTL behavior follows backend meta when available.

## Phase 2 — Add HomeViewModel dashboard status state

File:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeViewModel.kt`

Tasks:

1. Inject `GetTodayStatusUseCase`.
2. Add `TodayStatus` dashboard state.

Recommended state shape:

```kotlin
data class HomeTodayStatusUiState(
    val status: UiState<TodayStatus> = UiState.Loading,
    val warningMessage: String? = null,
    val isRefreshing: Boolean = false
)
```

Rationale:

- Plain `UiState<TodayStatus>` can represent loading/success/error, but preserving existing data while showing a non-blocking refresh warning is awkward.
- Wrapper keeps Home factual and avoids blanking the dashboard on refresh failure.

3. Expose:

```kotlin
val todayStatusState: StateFlow<HomeTodayStatusUiState>
```

or equivalent if implementation chooses plain `UiState<TodayStatus>` plus warning state.

4. Add:

```kotlin
fun fetchTodayStatus(forceRefresh: Boolean = false)
fun refreshDashboard(forceRefresh: Boolean = true)
```

5. Initial load in `init`:

```kotlin
fetchTodayStatus(forceRefresh = false)
```

6. `refreshDashboard(forceRefresh = true)` should refresh:

- today status with force refresh
- attendance history
- current address
- internship dashboard data only if retained for non-primary details or compatibility

7. Failure behavior:

- If previous today status success exists, keep it and set warning.
- If no previous data exists, set Today Status state to error.
- Do not blank entire Home.

Acceptance:

- Home consumes `GetTodayStatusUseCase` directly.
- Home does not consume `AttendanceViewModel`.
- Force refresh path exists and calls use case with `true`.

## Phase 3 — Build Home Today Status UI components

Potential new files:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/HomeTodayStatusCard.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/CompanyServicesGrid.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/RecentAttendancePreview.kt` if useful

Tasks:

1. Create `HomeTodayStatusCard`.
2. Use INF-222 components where they fit:

   - `InfiniteCard` / `InfiniteSurface`
   - `InfiniteButton`
   - `InfiniteStatusPill`
   - `InfiniteMetricCard`
   - `InfiniteSectionHeader`
   - `InfiniteInfoRow`
   - `InfiniteInlineAlert`

3. Card layout:

   - soft white/lavender surface
   - rounded corners
   - subtle border/shadow
   - compact two-column status grid
   - status pill/header
   - CTA or disabled state
   - optional refresh action

4. Render fields:

   - Status
   - Mode
   - Attendance target location
   - Check-in
   - Check-out
   - Work duration
   - Geofence/target summary

5. Add formatting helpers near component if not reused elsewhere:

   - status key/label mapping
   - mode formatting
   - time fallback formatting
   - ISO time formatting
   - duration seconds to `h m`
   - geofence summary text

6. CTA behavior:

   - Navigate to Attendance when allowed by status/capability.
   - Do not submit check-in/check-out directly.

Acceptance:

- Today Status card can render loading, success, error, and refresh-warning states.
- Card does not trigger geofence registration.
- Card makes clear that `active_location` is an attendance target, not current GPS.

## Phase 4 — Add Company Services role-gated grid

File:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/CompanyServicesGrid.kt`

Tasks:

1. Create static MVP `CompanyServicesGrid`.
2. Show only for Employee/Admin/Management.
3. Hide for Internship.
4. Recommended MVP items:

   - Attendance
   - Time Off
   - Attendance History
   - WFA only if there is a clear existing route/tab action and it is not a preview list

Implementation note:

- If WFA navigation is not directly available from current Home parameters, omit WFA from MVP rather than adding new route coupling.
- Do not create a backend Company Services contract.

Acceptance:

- Internship cannot see Company Services.
- Company Services does not include WFA request/history preview list.

## Phase 5 — Refactor Home role content

Files:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/EmployeeAndManagementContent.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/InternshipContent.kt`

Tasks:

1. Collect today status state in `HomeScreen`.
2. Pass today status state and refresh/navigation callbacks into role content.
3. Employee/Admin/Management layout:

   - Today/current location context
   - Greeting/identity context if retained
   - Today Status card
   - Company Services grid
   - Recent Attendance preview

4. Internship layout:

   - Today/current location context
   - Greeting/identity context if retained
   - Today Status card
   - Recent Attendance preview
   - No Company Services
   - Remove old primary Internship summary grid if it conflicts with replacement rule

5. Remove/avoid rendering from Home:

   - Smart Reminder
   - Personal Insight
   - Discipline score/advice
   - WFA booking/request preview list
   - Large Internship service list

6. Keep detailed booking history state only if still needed by legacy `DetailsMyBooking` route; do not render it on Home.

Acceptance:

- Existing Today/current location/attendance history concepts remain.
- Old Home component surface is replaced by new dashboard surface.
- No forbidden sections are rendered.

## Phase 6 — Compile and fix issues

Run from isolated worktree:

```bash
./gradlew app:assembleDebug
```

If build fails:

1. Capture the relevant error.
2. Fix compile issues in scoped files.
3. Re-run `app:assembleDebug`.
4. Do not claim passing unless command output confirms success.

Recommended additional checks if feasible:

```bash
./gradlew app:test
./gradlew app:lint
```

If environment blocks Gradle due to known local loopback/env issues, report exact blocker and mark build as `Needs Verification`.

## Phase 7 — Runtime/emulator verification

If emulator/device and accounts are available:

1. Install/open app.
2. Login as Employee/Admin/Management.
3. Go to Home.
4. Capture screenshot/recording:

   - Today Status card visible
   - Company Services visible
   - No Smart Reminder
   - No WFA request/history preview

5. Login/switch as Internship.
6. Capture screenshot/recording:

   - Today Status card visible
   - Company Services hidden
   - No Smart Reminder
   - No WFA request/history preview

7. Verify API/log evidence:

   - Home calls `GET /api/attendance/status-today` on initial load or forced refresh.

8. Verify duration formatting:

   - `work_duration_seconds` displays as a human-readable duration.

9. Verify refresh:

   - Home refresh action or return from Attendance calls `refreshDashboard(forceRefresh=true)` and reloads status-today.

If runtime cannot be executed, mark all visual/runtime evidence as `Needs Verification`.

## Phase 8 — Review and PR notes

Before claiming completion:

1. Inspect diff:

   ```bash
   git -C "C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard" status --short
   git -C "C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard" diff --stat
   git -C "C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard" diff --name-status
   ```

2. Review for acceptance criteria.
3. Prepare final response with required categories:

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

4. PR/review note should mention:

   - Work continued in existing isolated Home/dashboard worktree.
   - Implemented Home Today Status dashboard from `status-today` contract.
   - Added/updated TodayStatus DTO/domain fields and mapper.
   - HomeViewModel now consumes `GetTodayStatusUseCase` directly.
   - Removed/avoided Smart Reminder and WFA booking/request preview from Home.
   - Company Services shown only for Employee/Admin/Management.
   - No backend/auth/session/bottom-nav/geofence lifecycle changes.
   - Build evidence and runtime `Needs Verification` items.

## Implementation Guardrails

- Do not alter backend endpoints.
- Do not alter auth/session refresh behavior.
- Do not alter attendance check-in/check-out mutation semantics.
- Do not add Home-owned geofence registration.
- Do not add Smart Reminder, Personal Insight, discipline score, or recommendation content.
- Do not show WFA request/history preview list on Home.
- Do not show Company Services for Internship.
- Do not edit main checkout.

## Expected Files/Areas Affected

Likely changed:

- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/TodayStatusResponse.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- `app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendanceMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/HomeScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/EmployeeAndManagementContent.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/InternshipContent.kt`

Likely new:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/HomeTodayStatusCard.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/CompanyServicesGrid.kt`

Optional new if useful:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/content/RecentAttendancePreview.kt`

Docs created:

- `docs/superpowers/specs/2026-07-08-inf-229-android-home-today-status-dashboard.md`
- `docs/superpowers/plans/2026-07-08-inf-229-android-home-today-status-dashboard.md`

## Definition of Done for This Plan

This work is Done only when:

- Scope remains bounded to INF-229.
- Affected files/areas are named.
- DTO/domain/mapper align with required status-today fields.
- HomeViewModel consumes `GetTodayStatusUseCase` directly.
- Today Status card and role-gated Company Services are implemented.
- Forbidden Home sections are not rendered.
- Build evidence exists, or missing build is explicitly marked `Needs Verification` with blocker.
- Runtime visual/API evidence exists, or missing runtime verification is explicitly marked `Needs Verification`.
- Docs/ADR need is handled or explicitly not required.
- PR/review note is available.
