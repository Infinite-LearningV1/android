# INF-229 — Android Home Today Status Dashboard from status-today Contract

## Status

Draft — approved for implementation planning on 2026-07-08.

## Owner

Android

## Branch / Worktree

- Branch: `fix/android-home-today-status-dashboard`
- Worktree: `C:/Users/Febriyadi/.claude/worktrees/android-home-today-status-dashboard`
- Base branch: `develop`

## Problem

The Android Home dashboard currently relies on legacy Home components and does not directly render the backend `status-today` attendance/session contract as the primary personal attendance surface. The existing Home structure also risks keeping duplicate or non-primary sections that are now owned by Attendance, WFA, or History flows.

Home needs to become a factual personal attendance dashboard while preserving only the explicitly adopted concepts:

- Today / current date context
- Current location context
- Today Status card
- Company Services for eligible roles
- Recent Attendance preview

## Goal

Implement a factual Home dashboard that consumes the existing backend `GET /api/attendance/status-today` contract through Android's existing clean architecture flow and renders today's personal attendance/session state.

Required flow:

```text
ApiService.getTodayStatus()
→ AttendanceRepository.getTodayStatus(forceRefresh)
→ GetTodayStatusUseCase(forceRefresh)
→ HomeViewModel
→ Home UI Today Status card
```

Home remains a read-only dashboard surface except for navigation actions.

## Non-goals

Do not implement or change:

- Backend contract or new backend endpoint
- Auth/session business logic
- Attendance check-in/check-out business logic
- Direct check-in/check-out submit from Home
- Bottom navigation
- Active geofence registration or monitoring lifecycle from Home
- Smart Reminder card
- Personal Insight
- Discipline score/trend/advice
- AI-like recommendation/advice
- WFA request/history preview list on Home
- Backend Company Services contract

## Source of Truth Boundary

Backend remains authoritative for:

- Attendance outcome
- Auth/session validity
- Booking approval semantics
- Scheduled-job effects
- Final reporting state

Android Home is responsible for:

- Displaying backend-provided personal attendance/session state
- Displaying local current-location/address context
- Displaying recent attendance preview from existing history flow
- Navigating users to the correct existing flow when an action is available

Android must not invent final attendance/reporting truth when backend state is unavailable.

## Backend Contract

Endpoint:

```http
GET /api/attendance/status-today
```

Expected fields:

- `can_check_in`
- `can_check_out`
- `checked_in_at`
- `checked_out_at`
- `checked_in_at_iso`
- `checked_out_at_iso`
- `work_duration_seconds`
- `active_mode`
- `active_location`
- `today_date`
- `is_holiday`
- `holiday_checkin_enabled`
- `current_time`
- `checkin_window`
- `checkout_auto_time`
- `attendance_session_state`
- `active_attendance_id`
- `meta.cache_ttl_seconds`

## Important Semantics

`status-today.active_location` means the primary attendance target for today.

It is not:

- Current GPS location
- Android current address
- Full geofence location list

`active_location` may represent:

- Current attendance location if attendance already exists today
- Approved WFA booking location if there is an approved booking today
- Default WFO location if there is no attendance/booking

Home may display a geofence/target summary, but it must not register active geofence monitoring only because `active_location` exists.

## Android Model Alignment

If missing, update `TodayStatusResponse.kt` DTO with:

```kotlin
@SerializedName("checked_in_at_iso")
val checkedInAtIso: String? = null

@SerializedName("checked_out_at_iso")
val checkedOutAtIso: String? = null

@SerializedName("work_duration_seconds")
val workDurationSeconds: Long? = null
```

Update domain `TodayStatus` with:

```kotlin
val checkedInAtIso: String? = null
val checkedOutAtIso: String? = null
val workDurationSeconds: Long? = null
```

Update mapper:

- `TodayStatusData.toDomain()` maps `checkedInAtIso`, `checkedOutAtIso`, and `workDurationSeconds`.
- `TodayStatusResponse.toDomain()` prefers `meta.cache_ttl_seconds` when available.
- Fallback TTL remains `AuthRuntimePolicy.SHARED_TTL_SECONDS`.

## HomeViewModel Requirements

`HomeViewModel` must consume `GetTodayStatusUseCase` directly.

Add dashboard-oriented status state, either:

```kotlin
val todayStatusState: StateFlow<UiState<TodayStatus>>
```

or an equivalent wrapper that can preserve existing data and expose refresh warnings.

Required functions:

```kotlin
fetchTodayStatus(forceRefresh = false)
refreshDashboard(forceRefresh = true)
```

Expected behavior:

- Initial Home open calls `getTodayStatusUseCase(forceRefresh = false)`.
- Pull-to-refresh, refresh button, or return from Attendance after check-in/check-out calls `getTodayStatusUseCase(forceRefresh = true)`.
- If refresh fails and existing dashboard data exists, keep the existing data visible and show a factual non-blocking warning.
- If no existing data exists, show an error/empty factual state for Today Status only, not a blank entire Home.

Home must not reuse `AttendanceViewModel` for dashboard status consumption because that ViewModel owns attendance-flow side effects such as geofence registration, check-in/check-out action state, navigation to attendance flow, and map behavior.

## Home Sections

Adopted Home sections:

1. Today / current date context
2. Current location context
3. Today Status card
4. Company Services for Employee/Admin/Management only
5. Recent Attendance preview

Not adopted:

1. Smart Reminder card
2. Personal Insight
3. Discipline score
4. AI-like recommendation/advice
5. WFA request/history preview list
6. Riwayat Booking WFA section
7. Large Internship service list
8. Duplicate sections now owned by WFA tab or History tab

## Role Visibility

Company Services visible for:

- Employee
- Admin
- Management

Company Services hidden for:

- Internship

Internship still sees:

- Today/current location context
- Today Status card
- Recent Attendance preview

## Today Status Card Mapping

Status:

- Prefer `attendance_session_state.label`.
- Fallback to `attendance_session_state.key`.
- Map known keys:
  - `not_started` → `Belum check-in` / `Not Started`
  - `active` → `Sesi aktif` / `Active Session`
  - `completed` → `Selesai` / `Completed`
  - `unavailable` → `Tidak tersedia` / `Unavailable`

Mode:

- `active_mode`

Location:

- `active_location.description`
- Label as attendance target, not current GPS location.

Check-in:

- Prefer `checked_in_at`.
- Fallback to formatted `checked_in_at_iso`.
- If unavailable, display `--`.

Check-out:

- Prefer `checked_out_at`.
- Fallback to formatted `checked_out_at_iso`.
- If unavailable, display `--`.

Work Duration:

- Format `work_duration_seconds` to hours/minutes.
- If unavailable, display `--`.

Geofence:

- Display summary/status only.
- If Android local geofence/current location summary is available, show it factually.
- Otherwise show neutral attendance-target/geofence status.
- Do not register geofence monitoring from Home.

## CTA Rules

If shown, Today Status CTA follows these rules:

- `not_started` + `can_check_in=true` → navigate to Attendance / check-in flow.
- `active` + `can_check_out=true` + `active_attendance_id != null` → navigate to Attendance / check-out flow.
- `completed` → disabled/completed state.
- `unavailable` → disabled/info state.

Home does not submit check-in/check-out directly.

## Visual Direction

Use the uploaded/communicated Home visual reference only as layout/component reference.

Adopt:

- Today Status card
- Semi-liquid soft white card surface
- Compact two-column status grid
- Status field
- Mode field
- Location/attendance target field
- Check-in field
- Check-out field
- Work duration field
- Geofence badge/summary field
- Company Services grid for eligible roles

Do not adopt:

- Smart Reminder card

Style:

- Light theme only
- Semi-liquid, Android-runtime friendly
- Soft white/lavender surface
- Rounded cards
- Subtle borders
- Subtle shadows
- No heavy blur
- No dark mode
- No neon

Brand tokens:

- Primary purple: `#8A3DFF`
- Secondary yellow: `#FFCD29`
- Accent cyan: `#38F9F5`
- Dark text: `#2F2530`
- Light background: `#E7E4E9`
- Soft alert: `#FF6B6B`

## Component Guidance

Prefer reusable INF-222 design-system components when they fit:

- `InfiniteCard` / `InfiniteSurface`
- `InfiniteButton`
- `InfiniteStatusPill`
- `InfiniteMetricCard`
- `InfiniteTimelineRow`
- `InfiniteSectionHeader`
- `InfiniteInfoRow`
- `InfiniteFilterChips`
- `InfiniteBottomActionBar`
- `InfiniteInlineAlert`

Allowed Home-specific components:

- `HomeTodayStatusCard`
- `TodayStatusMetricItem`
- `GeofenceStatusBadge`
- `CompanyServicesGrid`
- `CompanyServiceItem`
- `RecentAttendanceCard`
- `RecentAttendanceRow`
- `HomeInlineWarning`

## Current Repo Evidence Before Implementation

Observed on 2026-07-08:

- Existing isolated worktree/branch is clean and safe to continue.
- `HomeScreen` currently routes by role to `InternshipContent` or `EmployeeAndManagerComponent`.
- `HomeViewModel` does not yet inject `GetTodayStatusUseCase`.
- `AttendanceRepository.getTodayStatus(forceRefresh)` exists.
- `GetTodayStatusUseCase(forceRefresh)` exists.
- `TodayStatusResponse` and domain `TodayStatus` do not yet include ISO/duration fields.
- `TodayStatusResponse.toDomain()` currently uses shared TTL fallback directly instead of preferring `meta.cache_ttl_seconds`.
- Home currently uses `HomeViewModel`, not `AttendanceViewModel`.
- INF-222 design components exist and can be reused.

## Acceptance Criteria

- HomeViewModel consumes `GetTodayStatusUseCase` directly.
- Home exposes `todayStatusState` or equivalent UI state.
- Today Status card renders status, mode, location/attendance target, check-in, check-out, work duration, and geofence summary.
- Android DTO/domain maps `checked_in_at_iso`, `checked_out_at_iso`, and `work_duration_seconds`.
- Mapper uses backend `meta.cache_ttl_seconds` when available.
- Home refresh can force `status-today` refresh.
- Home does not reuse `AttendanceViewModel` for dashboard status consumption.
- Home does not register active geofence only because `active_location` exists.
- Smart Reminder card is not rendered.
- Company Services appears only for Employee/Admin/Management.
- Company Services is hidden for Internship.
- WFA booking/request preview list is removed/not rendered from Home.
- Existing Today/current location/attendance history concepts remain where relevant.
- `./gradlew app:assembleDebug` passes, or failure is reported with evidence and status remains Needs Verification.

## Verification

Required local build:

```bash
./gradlew app:assembleDebug
```

Recommended local checks:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime/emulator smoke when environment is available:

- Open app.
- Go to Home.
- Observe Today Status card.
- Observe Company Services visible for Employee/Admin/Management.
- Observe Company Services hidden for Internship.
- Observe no Smart Reminder card.
- Observe no WFA request/history preview on Home.
- Trigger refresh from Home if UI exposes refresh.
- Return from Attendance after check-in/check-out and verify status refresh if feasible.

Evidence needed:

- Screenshot/recording of Home Today Status card.
- Screenshot/recording showing Company Services for Employee/Admin/Management.
- Screenshot/recording showing Company Services hidden for Internship.
- Screenshot/recording showing no Smart Reminder card.
- Screenshot/recording showing no WFA booking/request list on Home.
- Log/API evidence that Home calls `GET /api/attendance/status-today`.
- Evidence `work_duration_seconds` is displayed correctly as duration.
- Evidence `refreshDashboard(forceRefresh=true)` reloads `status-today`.
- Build evidence: `./gradlew app:assembleDebug`.

If emulator/runtime cannot run, mark runtime items as `Needs Verification`.

## Docs / ADR

No major ADR is expected if implementation only changes Home screen consumption/rendering and does not change:

- Auth/session contract or token refresh behavior
- Attendance capture semantics
- Backend source-of-truth expectations
- Face verification or liveness behavior
- Geofence/background location lifecycle
- Navigation shell or role-visible route behavior
- Network base URL or environment contract
- Firebase distribution/signing/release workflow

PR/review notes must mention:

- `status-today` contract consumed directly by Home.
- Home remains factual and backend/session-driven.
- No Smart Reminder / Personal Insight.
- No WFA request/history list on Home.
- No geofence lifecycle ownership change.
