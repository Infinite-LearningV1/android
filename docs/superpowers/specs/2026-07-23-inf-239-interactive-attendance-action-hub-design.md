# INF-239 — Interactive Attendance Action Hub Design

Date: 2026-07-23  
Branch: `djangosuryaa/inf-239-interactive-attendance-action-hub`

## 1. Decision

Layer 3 uses an **Interactive Attendance Action Hub** rather than a static alert plus separate attendance buttons.

The hub is the primary decision surface for check-in and checkout:

```text
One state
→ one short explanation
→ one primary action
→ expandable supporting evidence
```

Tap on a ready check-in or checkout action opens Face Recognition directly. There is no confirmation dialog before Face Recognition because the scanner is already a deliberate second step.

## 2. Product boundary

Layer 3 consumes the typed result of Attendance Preparation and owns the action journey:

```text
Attendance Preparation result
→ Ready / Blocked
→ Face Recognition handoff
→ Submitting
→ Backend result
→ Success / Failure / Completed
```

Face verification is evidence only. Final success exists only after the backend confirms check-in or checkout.

## 3. Goals

1. Make the next action obvious without scanning the entire bottom sheet.
2. Show enough context to build trust without cluttering the default view.
3. Replace disabled attendance buttons with useful recovery actions.
4. Distinguish Face Recognition success from backend submission success.
5. Show a meaningful completed summary instead of a dead CTA.
6. Preserve current Clean Architecture and backend authority.

## 4. Non-goals

- Reworking Face Recognition internals.
- Reworking geofence lifecycle.
- Rebuilding Attendance Preparation.
- Changing backend endpoints or final validation rules.
- Adding employee-managed WFH location setup.
- Treating WFA recommendations as approved Attendance targets.

## 5. Information hierarchy

### Level 1 — Immediate decision

Always visible:

```text
status headline
short supporting text
current intent: check-in / checkout
one primary action
```

### Level 2 — Compact evidence

Visible in the hub without expansion:

```text
work mode
authoritative target
current distance/range when relevant
WFA booking status when WFA is selected
```

### Level 3 — Expandable details

Shown through `Lihat detail`:

```text
required permission readiness
GPS/device-location readiness
backend status freshness
today session/check-in time
selected target source
optional capability state
```

## 6. Interaction rules

- Maximum one primary action.
- Maximum one secondary text action.
- Do not show a disabled Attendance CTA next to a recovery CTA.
- Ready CTA opens Face Recognition directly.
- `VerifyingFace` exists to prevent duplicate navigation and double taps.
- `Submitting` has no retry action until a backend outcome is known.
- Success may use a compact modal sheet/dialog with outcome details.
- Completed has no disabled attendance button; optional action is `Lihat detail`.
- Expand/collapse is presentation-local state unless product behavior requires persistence.

## 7. Business state contract

```kotlin
sealed interface AttendanceActionState {
    data object Resolving : AttendanceActionState

    data class Ready(
        val intent: AttendanceActionIntent
    ) : AttendanceActionState

    data class Blocked(
        val reason: AttendanceBlockReason,
        val recoveryAction: AttendanceRecoveryAction?
    ) : AttendanceActionState

    data class VerifyingFace(
        val intent: AttendanceActionIntent
    ) : AttendanceActionState

    data class Submitting(
        val intent: AttendanceActionIntent
    ) : AttendanceActionState

    data class Success(
        val result: AttendanceActionResult
    ) : AttendanceActionState

    data class Failure(
        val intent: AttendanceActionIntent?,
        val reason: AttendanceFailureReason,
        val recoveryAction: AttendanceRecoveryAction
    ) : AttendanceActionState

    data class Completed(
        val summary: AttendanceDaySummary
    ) : AttendanceActionState
}
```

Business state must not contain display copy such as `label`, `title`, or `message`.

## 8. Presentation model

```kotlin
data class AttendanceActionHubUiModel(
    val semantic: AttendanceActionSemantic,
    val headline: String,
    val supportingText: String,
    val primaryAction: AttendanceActionUiAction?,
    val secondaryAction: AttendanceActionUiAction?,
    val evidence: List<AttendanceActionEvidenceRow>,
    val details: List<AttendanceActionDetailRow>,
    val progress: AttendanceActionProgressUiModel?,
    val resultSummary: AttendanceResultSummaryUiModel?
)
```

A presentation mapper converts typed business state into UI copy, icons, semantics, and actions.

## 9. State matrix

### Resolving

```text
Headline: Memeriksa status attendance
Primary action: none
Details: optional loading skeleton
```

### Ready — Check-in

```text
Headline: Siap untuk check-in
Evidence: mode, target, distance/range
Primary: Check-in sekarang
Action: open Face Recognition directly with CHECK_IN intent
```

### Ready — Checkout

```text
Headline: Sedang bekerja
Supporting: Check-in pukul <time>
Primary: Check-out sekarang
Action: open Face Recognition directly with CHECK_OUT intent
```

Checkout does not require work-mode or target reselection.

### Permission blocked

```text
Primary: Izinkan akses / Buka pengaturan
Secondary: none unless a valid alternative exists
```

### GPS disabled

```text
Primary: Aktifkan GPS
```

### WFA — no request

```text
Headline: Booking WFA diperlukan
Booking status: Belum diajukan
Primary: Ajukan WFA
Secondary: Lihat permintaan
```

### WFA — pending

```text
Headline: WFA menunggu persetujuan
Booking status: Menunggu
Primary: Lihat status permintaan
```

### WFA — rejected

```text
Headline: Booking WFA ditolak
Primary: Lihat alasan
Secondary: Ajukan ulang, only when allowed by backend/product contract
```

### WFA — approved

Approved booking becomes the authoritative target. Preparation may become ready.

### Unexpected WFH target absence

WFH is mandatory and admin-provisioned. Missing WFH is not employee setup.

```text
Headline: Lokasi WFH belum dapat dimuat
Primary: Muat ulang
Secondary: Hubungi admin
Reason: backend/data synchronization or contract failure
```

### Verifying Face

```text
Headline: Membuka verifikasi wajah
Primary: disabled/none
Behavior: prevent duplicate navigation
```

### Submitting

Show two-step progress:

```text
Face verified ✓
Backend confirmation in progress
```

Primary action is disabled with intent-specific processing copy.

### Backend failure

Recovery must be typed:

```text
RetrySubmission
RefreshStatus
RestartFaceVerification
```

Copy and CTA depend on the failure reason. Do not use a universal `Coba lagi` action.

### Success

Show backend-confirmed details:

```text
action
recorded time
work mode
authoritative target
```

Primary: `Selesai`.

### Completed

```text
Attendance hari ini selesai
Check-in time
Check-out time
Work duration
Server confirmation
```

Optional primary action: `Lihat detail attendance`.

## 10. Component composition

```text
AttendanceActionHub
├── AttendanceActionHeader
├── AttendanceActionEvidenceSummary
├── AttendanceActionProgress
├── AttendanceActionResultSummary
├── AttendanceActionDetails
└── AttendancePrimaryAction
```

Reuse Infinite design tokens and existing button/status primitives. New components are allowed only as focused composition components, not a parallel design system.

## 11. Navigation and effects

ViewModel emits semantic effects only:

```kotlin
sealed interface AttendanceActionEffect {
    data class OpenFaceRecognition(val intent: AttendanceActionIntent) : AttendanceActionEffect
    data object RequestRequiredPermission : AttendanceActionEffect
    data object OpenApplicationSettings : AttendanceActionEffect
    data object OpenDeviceLocationSettings : AttendanceActionEffect
    data object OpenWfaRequest : AttendanceActionEffect
    data object OpenWfaRequests : AttendanceActionEffect
    data object ContactAdmin : AttendanceActionEffect
    data object OpenAttendanceDetail : AttendanceActionEffect
}
```

Route/UI owns `NavController`, Activity Result launchers, settings intents, raw routes, and external-contact intents.

## 12. Error handling

- Raw exception messages never appear in UI.
- Backend uncertainty must not be presented as failure if the submission outcome is unknown; refresh status first.
- Duplicate taps and duplicate navigation are blocked while VerifyingFace/Submitting.
- WFA recommendation alone never enables Attendance.
- WFH missing unexpectedly maps to contract/sync failure, not employee setup.

## 13. Accessibility

- Primary actions meet minimum touch-target requirements.
- Semantic state is not conveyed by color alone.
- Progress changes are announced through accessible live-region semantics where appropriate.
- Expand/collapse has explicit accessible labels.
- Evidence rows use concise labels and readable values.

## 14. Testing strategy

### Mapper tests

- Every business state maps to the expected primary action.
- Blocked states never produce both disabled attendance and recovery CTAs.
- WFA no-request/pending/rejected/approved states map correctly.
- Unexpected WFH missing maps to refresh/contact-admin guidance.
- Submitting distinguishes Face verified from backend confirmation.

### ViewModel tests

- Ready tap emits one Face Recognition effect.
- Repeated tap while VerifyingFace emits no duplicate effect.
- Face success transitions to Submitting before repository submission.
- Face failure/timeout/cancel does not submit attendance.
- Backend success produces Success.
- Unknown backend outcome triggers status refresh behavior.
- Checkout bypasses preparation reselection.

### Compose tests

- Compact state shows headline, evidence, and one primary action.
- Detail toggle expands and collapses evidence.
- WFA pending shows booking status and correct action.
- Completed shows summary and no disabled attendance CTA.
- Submitting shows two-step progress.

## 15. Acceptance criteria

- [ ] Interactive Attendance Action Hub is the primary Layer 3 surface.
- [ ] Ready CTA opens Face Recognition directly without a confirmation dialog.
- [ ] One state produces at most one primary action and one secondary text action.
- [ ] Business state contains no display copy.
- [ ] Presentation mapper owns copy and component selection.
- [ ] Compact evidence shows mode, authoritative target, and relevant distance/booking status.
- [ ] Expanded details show permission, GPS, backend freshness, and session evidence.
- [ ] WFA requires an approved booking before Attendance becomes ready.
- [ ] WFH missing is treated as exceptional admin/backend contract failure.
- [ ] Submitting clearly distinguishes Face verification from backend confirmation.
- [ ] Success only occurs after backend confirmation.
- [ ] Completed shows a summary and no dead Attendance CTA.
- [ ] Duplicate Face Recognition navigation/submission is prevented.
- [ ] ViewModel has no `NavController`, Activity, Compose, DTO, Entity, Retrofit, or Room dependency.
- [ ] Relevant unit, ViewModel, Compose, build, and runtime evidence is provided.

## 16. Superseded direction

This design supersedes the static Layer 3 presentation and the old functional-only implementation plan where they conflict. The typed state-machine and backend-authority rules remain valid.