# INF-201 Face Frame / Progress-Rail State Sync — Design

- Date: 2026-07-26
- Linear: INF-201 (follow-up revision on the merged PR #103 redesign)
- Scope owner: `presentation/screen/attendance/face/`
- Status: Approved design, pending implementation plan

## Goal

Make the neon face-detection frame (`FaceVerificationFrame`) render the correct visual for
every `LivenessState`, and keep the numbered liveness progress rail visually attached to the
frame border and always consistent with `LivenessSequencer` progress. This is a
presentation-only revision: no state-machine, ViewModel, or detection-pipeline changes.

## Problems being fixed

1. The rail renders as a horizontal row at the bottom inside the frame; the approved mockups
   attach numbered nodes (1–4) to the frame border (1–2 on the left edge, 3–4 on the right).
2. `frameStyleFor()` uses a catch-all `else` branch, so `LOW_LIGHT` and `VERIFYING_FACE`
   accidentally inherit liveness visuals (rail shown during low light, contrary to mockup).
3. `FAILURE` renders a plain red frame; the mockup shows a cyan frame with a red X badge.
4. `TIMEOUT` renders a plain red frame; the mockup shows a purple frame with a stopwatch
   icon, "Verification Timeout" title/subtitle, and a "00:00 / Time's up" ring inside.
5. `SUCCESS` badge is a Surface-background circle with a purple check; the mockup shows a
   solid purple circle with a white check.
6. When `readyToVerify` is true, the rail disappears; decision: show all four nodes as passed.

## Non-goals

- No changes to `FaceScannerViewModel`, `LivenessSequencer`, `FaceScannerTransitionPolicy`,
  `FaceDetectorHelper`, or any domain/data contract.
- No changes to the bottom sheet, top bar, status pills, diagnostics card, or result copy.
- No new similarity/threshold exposure; release-diagnostics posture is unchanged.
- No challenge-policy change (fixed 4-challenge sequence stays as merged in PR #103).

## State → visual contract

`frameStyleFor()` becomes an exhaustive `when` over all nine `LivenessState` values — no
`else` branch — so adding a state forces a compile-time decision.

| `LivenessState` | Frame stroke | Rail | Badge | Inner content |
|---|---|---|---|---|
| `IDLE` | Dashed purple | HIDDEN | NONE | SILHOUETTE |
| `DETECTING_FACE` | Dashed purple | HIDDEN | NONE | SILHOUETTE |
| `WAITING_FOR_LIVENESS` | Solid purple | PROGRESS | NONE | NONE |
| `LOW_LIGHT` | Solid purple | HIDDEN | NONE | NONE |
| `LIVENESS_DETECTED` | Solid purple | `readyToVerify` ? ALL_PASSED : PROGRESS | NONE | NONE |
| `VERIFYING_FACE` | Solid purple | ALL_PASSED | NONE | NONE |
| `SUCCESS` | Solid cyan | HIDDEN | CHECK | NONE |
| `FAILURE` | Solid cyan | HIDDEN | CROSS | NONE |
| `TIMEOUT` | Solid purple | HIDDEN | NONE | TIMEOUT_INFO |

Rail data derivation is unchanged and remains the single source of truth:
`passedCount = (challengeIndex - 1).coerceAtLeast(0)`, `activeIndex = challengeIndex`,
`total = challengeTotal`. `ALL_PASSED` renders `railNodeStates(total, 0, total)`-equivalent
visuals (every node passed) without inventing parallel progress state.

Colors: reuse the existing local constants `FramePurple (0xFF8A3DFF)`,
`FrameCyan (0xFF38F9F5)`, `FrameRed (0xFFFF5C5C)` (red now used only for the CROSS badge)
plus `InfiniteColors` tokens for node/badge fills.

## FrameStyle model

```kotlin
enum class RailMode { HIDDEN, PROGRESS, ALL_PASSED }
enum class FrameBadge { NONE, CHECK, CROSS }
enum class FrameInnerContent { NONE, SILHOUETTE, TIMEOUT_INFO }

data class FrameStyle(
    val color: Color,
    val dashed: Boolean,
    val railMode: RailMode,
    val badge: FrameBadge,
    val inner: FrameInnerContent
)
```

The previous booleans (`showRail`, `showCheckBadge`, `showSilhouette`) are removed. Illegal
combinations (badge on a dashed frame, silhouette plus rail) become unrepresentable.
`frameStyleFor(state: FaceScannerState): FrameStyle` stays a top-level pure function for JVM
unit testing.

## Rail on the frame border

Rendered inside `BoxWithConstraints` in `FaceVerificationFrame`, positioned as fractions of
the frame height (no hardcoded dp offsets), matching the mockups:

```text
Node 1 → left edge,  center at 20% of frame height
Node 2 → left edge,  center at 45% of frame height
Node 3 → right edge, center at 35% of frame height
Node 4 → right edge, center at 55% of frame height
```

- Each node is centered on the stroke line (half outside the frame), 28.dp circle, with a
  short horizontal tick connector toward the frame, per mockup.
- Node visuals reuse the existing `RailNode` composable (passed = Accent cyan + check,
  active = Secondary amber + number, pending = outlined). `RailNode` changes from `private`
  to `internal` so the frame can place nodes individually. `railNodeStates()` and its tests
  are untouched.
- The old horizontal `LivenessProgressRail` row is no longer rendered by the frame. The
  `LivenessProgressRail` composable itself is deleted together with its only call site
  (`railNodeStates` and `RailNode` remain).

## TIMEOUT inner content

When `inner == TIMEOUT_INFO`, the frame renders a centered column inside its bounds. The
full-stage dimming seen in the mockup is owned by `FaceScannerScreen`: a semi-transparent
black scrim layer over the camera preview (below the frame) shown only when
`livenessState == TIMEOUT`. Column content:

```text
[stopwatch + exclamation icon inside a Surface-colored circle]
Title: "Verification Timeout"
Subtitle: "You didn't complete the verification in time. Please try again."
[amber ring: "00:00" over "Time's up"]
```

All copy goes through `strings.xml` with Indonesian counterparts in `values-in/strings.xml`
(e.g. "Waktu Verifikasi Habis", "Verifikasi tidak selesai tepat waktu. Silakan coba lagi.",
"Waktu habis"). No hardcoded literals in the composable. The ring shows the static terminal
"00:00" state; it does not re-run a countdown (the live countdown remains part of the
guidance text as today).

## SUCCESS / FAILURE badges

- CHECK: solid `FramePurple` circle, white check icon, subtle glow — replaces the current
  Surface-background badge.
- CROSS: solid `FrameRed` circle, white X icon — new; `FAILURE` frame stroke becomes cyan.
- Badge position stays `Alignment.TopCenter`, overlapping the stroke as in the mockups.
- Accessibility: badges carry contentDescription (verified / not matched) resolved from
  `strings.xml`, so state is not conveyed by color alone; dashed-vs-solid and inner content
  further differentiate states.

## Files affected

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt` — model, exhaustive mapping, border-rail layout, badges, timeout content
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt` — `RailNode` visibility, remove row composable
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt` — minor: full-stage scrim behind the frame when `TIMEOUT`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml` — timeout copy
- `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrameStyleTest.kt` — one assertion block per state + `readyToVerify` variants
- `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt` — timeout content, badge, and border-rail assertions

Unchanged: ViewModel, sequencer, transition policy, bottom sheet, top bar, result surfaces.

## Testing

- JVM: `frameStyleFor` covered for all nine states, including regressions "LOW_LIGHT has no
  rail", "FAILURE is cyan + CROSS", "TIMEOUT is purple + TIMEOUT_INFO", "readyToVerify →
  ALL_PASSED", "VERIFYING_FACE → ALL_PASSED".
- Compose (androidTest): timeout column visible on TIMEOUT; check/cross badges by
  contentDescription; four border nodes exist during liveness.
- Local gate: `./gradlew app:test app:lint app:assembleDebug`.
- Runtime gate (CLAUDE.md): emulator/device screenshots of liveness, low-light, timeout,
  failure, success frames — these also feed the still-open INF-201 evidence list.

## Risks

- Node fraction positions may collide with the challenge chip or guidance text on very small
  screens; mitigated by fraction-based placement and device check on a narrow emulator.
- Removing `LivenessProgressRail` breaks any external caller — repo grep shows the frame is
  the only call site; `LivenessProgressRailStateTest` targets `railNodeStates`, which stays.
- `FAILURE` losing the red frame could reduce at-a-glance severity; mitigated by the red
  CROSS badge, red status pill row (unchanged), and bottom-sheet failure copy.
