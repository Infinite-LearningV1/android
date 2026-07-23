# INF-238 — Attendance Preparation Product Contract Amendment

Date: 2026-07-23  
Branch: `djangosuryaa/inf-238-attendance-preparation-spec-plan`

## Authority

This amendment corrects the Attendance Preparation design and implementation plan where they model missing WFH location as a normal user-recoverable state.

When this amendment conflicts with:

- `docs/superpowers/specs/2026-07-23-attendance-preparation-design.md`
- `docs/superpowers/plans/2026-07-23-attendance-preparation.md`

this amendment governs.

## Product invariant

WFH location is mandatory and provisioned by an administrator before the employee uses Attendance.

```text
Employee with WFH mode
→ administrator-provisioned home target exists
→ Android resolves the WFH target from backend/profile-authoritative data
```

A missing WFH target is therefore not a normal user recovery flow.

Do not present:

```text
Atur lokasi WFH
Tambahkan lokasi rumah
OpenWfhLocationSettings
Choose another mode because WFH is unconfigured
```

unless the product later introduces explicit employee self-service WFH target management.

## Correct failure classification

If an employee is assigned or allowed to use WFH but the authoritative WFH target is absent, classify the condition as a data-contract or synchronization failure.

Recommended domain reason:

```kotlin
sealed interface AttendancePreparationBlockReason {
    data object CameraPermissionMissing : AttendancePreparationBlockReason
    data object ForegroundLocationPermissionMissing : AttendancePreparationBlockReason
    data object DeviceLocationDisabled : AttendancePreparationBlockReason
    data object WfoTargetUnavailable : AttendancePreparationBlockReason
    data object WfhTargetContractViolation : AttendancePreparationBlockReason
    data object WfaApprovedTargetMissing : AttendancePreparationBlockReason
    data object AttendanceDateUnavailable : AttendancePreparationBlockReason
    data object TargetRefreshFailed : AttendancePreparationBlockReason
}
```

Recommended recovery behavior:

```text
WfhTargetContractViolation
→ block progression
→ offer RefreshStatus when stale data is possible
→ otherwise show contact-admin/support guidance
→ do not let the employee create or edit the WFH target from Attendance
```

Suggested safe UI copy:

```text
Lokasi WFH belum dapat dimuat
Lokasi kerja dari rumah seharusnya telah ditetapkan oleh admin. Muat ulang data atau hubungi admin jika masalah berlanjut.
```

Primary action:

```text
Muat ulang
```

Optional secondary action:

```text
Hubungi admin
```

## WFA contract

WFA is conditional and requires a booking before Attendance can proceed.

```text
WFA recommendation
→ discovery and request input

WFA request submitted
→ pending approval

Approved WFA booking for the relevant attendance date
→ authoritative WFA Attendance target
```

Selecting a recommendation does not make WFA attendance eligible.

Without an approved booking:

```text
WFA selected
→ preparation blocked
→ primary recovery action: Ajukan WFA or Lihat permintaan WFA
→ no Face Recognition navigation
→ no backend attendance submission
```

With an approved booking:

```text
WFA selected
→ resolve target from approved booking
→ evaluate required permissions and device readiness
→ preparation may become Ready
```

## Revised mode matrix

| Mode | Authoritative target | Normal missing-target behavior |
|---|---|---|
| WFO | `status-today.activeLocation` | Block as backend target unavailable; refresh or support guidance |
| WFH | Admin-provisioned home target | Treat as contract/sync failure; refresh or contact admin |
| WFA | Approved booking for attendance date | Expected recoverable blocker; request or inspect WFA booking |

## UX implications for Attendance Action Hub

Normal recovery actions should focus on conditions the employee can actually fix.

```text
Camera permission missing
→ Izinkan kamera

Foreground location missing
→ Izinkan lokasi

GPS disabled
→ Aktifkan GPS

WFA approved booking missing
→ Ajukan WFA / Lihat permintaan

Status stale or target refresh failed
→ Muat ulang status
```

WFH must normally appear as ready with its admin-provisioned target. Do not use `Atur lokasi WFH` as the representative blocked-state example in final designs.

For an exceptional WFH contract failure, show a system/data error with refresh and admin-support guidance rather than a self-service location editor.

## Revised tests

Remove or rewrite tests that assert missing WFH location is a normal recovery path.

Required domain tests:

```text
- WFH resolves from the administrator-provisioned authoritative target.
- WFH target absence maps to WfhTargetContractViolation.
- WFH target absence never emits OpenWfhLocationSettings.
- WFH target absence may emit RefreshStatus or ContactAdmin guidance.
- WFA without approved booking maps to WfaApprovedTargetMissing.
- WFA without approved booking exposes request/history recovery action.
- WFA recommendation alone never makes preparation Ready.
- WFA with approved booking resolves that booking as the authoritative target.
```

Required UI/runtime evidence:

```text
- WFH ready with administrator-provisioned target.
- Exceptional WFH contract failure shows refresh/admin guidance, not location editing.
- WFA blocked without approved booking and shows WFA request recovery.
- WFA ready with approved booking.
```

## Revised acceptance criteria

- WFH target is treated as an administrator-provisioned backend invariant.
- Normal WFH UX does not offer employee self-service target creation or editing.
- Missing WFH target is classified as a contract or synchronization failure.
- WFA requires an approved booking before Face Recognition.
- WFA recommendations remain request inputs and never become Attendance truth.
- Attendance Preparation and Attendance Action UI expose only recovery actions the employee is authorized to perform.
