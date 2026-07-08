# Profile Hub Style Polish

Date: 2026-07-09

## Goal

Polish the Profile main screen card styling and text hierarchy without changing its structure, data, callbacks, or navigation behavior.

## Scope

- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt`

## Guardrails

- Keep the current Account Hub layout and content grouping.
- Do not change navigation callbacks.
- Do not change backend/auth/session behavior.
- Do not change role visibility logic.
- Do not change section order or routes.
- Change only card transparency/shadow/border and text composition/weight/size hierarchy.

## Visual direction

Use existing repo card language from:

- `UserMyLeaveCard.kt`
- `OverviewCardAttendance.kt`
- `CardAbsence.kt`
- `AttendanceHistoryCard.kt`

Target outcome:

- Softer transparent glass cards
- Synchronized white border treatment
- Softer shadow/glow treatment
- Less aggressive bold text
- Better title/description hierarchy
- Keep existing purple/cyan accent only as subtle support
