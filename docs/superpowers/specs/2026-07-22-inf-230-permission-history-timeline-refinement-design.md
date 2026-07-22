# INF-230 Permission and History Timeline Refinement Design

Date: 2026-07-22
Branch: `codex/inf-230-timeline-scroll-refinement`
Base: `origin/develop` at `412689a`

## Context

The first INF-230 implementation established permission ownership, one-shot effects, recovery semantics, and reusable feedback surfaces. Runtime review on `develop` identified visual and navigation refinements:

- the permission app bar is lower and more inset than other secondary screens because the screen adds status-bar and content padding around an app bar that already owns horizontal padding;
- the permission screen paints its own background and radial gradients over the global application background;
- the required and optional permission lists create too much vertical content;
- the ready-state inline alert duplicates information and should not be rendered;
- a user whose required access is already ready still sees the readiness destination and can retain it in the back stack;
- the History timeline is rendered as fixed-height eager rows inside one outer lazy item, so its pills and rail cannot react continuously to viewport position.

This refinement keeps the approved permission semantics. It changes presentation, navigation completion, and the History timeline's scroll behavior.

## Goals

1. Match the shared secondary-screen app bar configuration and global transparent background.
2. Present the three required permissions as a compact, sequential pill timeline.
3. Hide optional permissions behind an explicit disclosure and keep them non-blocking.
4. Skip the readiness UI and remove its destination when required access is ready.
5. Provide a continuously scroll-linked, center-focus History timeline.
6. Reuse existing colors, typography, shapes, spacing, motion, and `InfiniteStatusPill` behavior.
7. Keep the implementation accessible, responsive, testable, and free of nested vertical scrolling.

## Non-goals

- No permission or geofence semantics change.
- No new required permission.
- No automatic launch of Android permission dialogs during composition or navigation.
- No global redesign of `InfiniteStatusPill`.
- No gradient feedback redesign outside this permission screen.
- No deletion of the legacy permission helper before the existing runtime gate permits it.
- No runtime verification claim from unit tests or compilation alone.

## Selected approach

The visual system exposes exactly two new public timeline-pill composables:

1. `PermissionTimelinePill`
2. `AttendanceHistoryTimelinePill`

Private rail, node, and lazy-list helpers are implementation details and do not become additional public visual components. `InfiniteStatusPill` remains the existing compact status badge used inside the History pill.

The permission timeline is state-driven. The History timeline is viewport-driven using the approved **Center focus** interaction.

## Design-system rules

- Screen and container backgrounds are transparent so the global application background remains visible.
- Permission timeline surfaces use one translucent theme color. They do not use `Brush`, gradients, or decorative radial backdrops.
- Colors come from `InfiniteColors`, existing semantic palettes, and alpha adjustments. No new hardcoded brand colors are introduced in composables.
- Text uses existing typography tokens such as `headline4`, `body1`, and `body2`. Call sites must not override `fontSize`, `lineHeight`, or `fontFamily`.
- Shapes, spacing, and elevation reuse existing design tokens where available.
- Active elevation is subtle. The visual state must remain understandable without shadow.
- Every interactive target is at least 48 dp.
- Both public components accept `modifier: Modifier = Modifier`, data/state parameters, and callbacks. Neither component reads a ViewModel.

## Permission screen structure

`AttendancePermissionReadinessScreen` uses a transparent `Scaffold`:

- `InfiniteTopBar` is placed in `Scaffold.topBar`, matching About, Contact, and other secondary screens.
- The screen removes its explicit `statusBarsPadding`, local vertical app-bar padding, `AttendanceReportBackground`, and `PermissionBackdrop`.
- Scaffold content consumes `innerPadding` exactly once.
- Content uses the repository's normal secondary-screen horizontal inset.
- The hero card, old progress card, default ready guidance, and the “Akses wajib sudah siap” inline alert are removed.
- Required inspection failures render a compact actionable inline alert.

Initial OS inspection does not render the permission timeline. The transparent shell withholds readiness content until trustworthy state is available, preventing a ready user from seeing a flash of incomplete permission UI before automatic navigation.

## `PermissionTimelinePill` contract

Each required permission is one large pill containing:

- a circular numbered node;
- title and supporting copy;
- one compact action/status label;
- semantic state for TalkBack;
- an optional click callback when action is available.

The parent owns sequence and connector state. The three nodes progress as follows:

```text
1 active -> check complete -> 2 active -> check complete -> 3 active -> check complete
```

Behavior:

- only the current actionable pill is elevated;
- an inactive future pill remains visible but subdued;
- a ready or not-required item replaces its number with a check using `AnimatedContent` or an equivalent theme-motion-aware content transition;
- connector segments run from one node center to the next and therefore follow each pill's measured height;
- the accent segment represents completed required access, not scroll position;
- permanent denial changes the current action to application Settings;
- device-location disabled changes the current action to location Settings;
- a completed pill remains readable and does not collapse.

The three-item list remains a normal `Column`; it must not introduce another scroll container.

## Optional permission disclosure

The collapsed row is labeled “Pengingat opsional” and summarizes notification and background-location support. It is collapsed by default on each new readiness destination.

Expanding it reveals the existing optional item copy and actions. Expansion uses `AnimatedVisibility` and `animateContentSize` with theme motion. Optional readiness, denial, and inspection issues never disable manual Attendance entry.

## Permission completion and navigation

The ViewModel remains the source of readiness decisions and one-shot effects.

1. The route starts OS inspection.
2. When trustworthy readiness reports `canEnterAttendance == true`, the ViewModel emits `NavigateToWorkMode` once, guarded by the existing `navigationPending` state.
3. This applies both to an initially ready user and to the transition after the final required item becomes ready.
4. The route handles the effect and acknowledges `NavigationHandled`.
5. Navigation to `Screen.Attendance` removes `Screen.AttendancePermissionReadiness` with an inclusive `popUpTo` and uses `launchSingleTop`.
6. Optional degraded state does not delay this effect.
7. A required inspection failure does not use stale readiness to navigate.

Normal Android permission requests remain explicitly user-triggered. Automatic navigation must not automatically open a permission dialog.

## Error and recovery behavior

- Required inspection failure: actionable inline alert with Retry.
- Recoverable denial: active pill provides the appropriate request action.
- Permanent denial: active pill opens application Settings.
- Device location disabled: active pill opens location-source Settings.
- Settings launch failure: existing snackbar behavior remains.
- Optional inspection failure: shown only inside the expanded optional section.
- Ready state: no success alert; navigation is the success outcome.

## History lazy-list structure

History continues to use one top-level `LazyColumn`. The timeline is no longer one eager `Column` inside a single lazy item.

- Summary, distribution, actions, notices, and timeline header remain normal `item` entries.
- Each `AttendanceRecord` becomes an individual lazy item with stable key `history-${record.id}`.
- A private `LazyListScope` builder maps records and connector positions, but it is not a public visual component.
- There is no nested vertical `LazyColumn` or `verticalScroll`.
- The former fixed 56 dp row height is removed. Pills wrap content and support large fonts.
- Connector segments use each item's actual height and join adjacent node centers.

Empty, loading, and error states keep their existing semantic ownership and do not participate in center-focus transforms.

## `AttendanceHistoryTimelinePill` contract

Each History pill contains:

- a circular date/day node on the rail;
- date and time range;
- location or work-hour supporting text;
- the existing `InfiniteStatusPill` badge;
- connector position information;
- a normalized `focusFraction` in the closed range `0f..1f`.

The component is stateless. The caller calculates `focusFraction` from `LazyListState.layoutInfo`:

```text
distance = abs(itemCenter - viewportCenter)
focusFraction = 1 - min(distance / focusRange, 1)
```

`focusRange` is `viewportHeight * 0.58f`, expressed as a named constant. This keeps the approved behavior proportional across device sizes.

## History Center-focus interaction

The transformation is continuous and reversible while scrolling:

- a pill near the viewport edge is slightly subdued;
- the pill nearest the viewport center reaches full opacity and a subtle elevated scale;
- neighboring pills interpolate smoothly according to their distance;
- the rail's accent progress is timeline-local, not based on the entire History screen;
- the caller finds visible items whose stable keys start with `history-`, maps each key back to its record index, and interpolates the nearest center item with its offset relative to viewport center;
- the resulting progress is clamped between the first and last History record, so summary cards above the timeline do not pre-fill the rail;
- scrolling upward immediately reverses the same transform; there is no one-time reveal state and no replay trigger;
- there is no bounce loop or delayed stagger.

Recommended transform envelope:

- alpha: `0.70f..1.00f`;
- scale: `0.965f..1.00f`;
- vertical translation: `0..-3dp` at center;
- elevation: subtle and proportional to focus.

Transforms are applied through `graphicsLayer` or draw-layer APIs so scroll frames do not trigger item relayout. Scroll-derived values use `derivedStateOf` or a narrowly scoped `snapshotFlow`; expensive mapping must not run in every row body.

When system motion duration scale is zero, content remains fully visible and readable with spatial motion disabled. The rail and status information must not depend on animation.

## Accessibility and responsive behavior

- Pill semantics merge title, requirement/status, and available action without duplicating decorative node content.
- Number-to-check changes expose the updated state description.
- Optional disclosure exposes expanded/collapsed state.
- History focus is decorative; TalkBack order remains chronological and does not change with scale.
- Screen-reader focus must not be driven by viewport center.
- 320 dp width and font scale 2.0 reflow status/action content below supporting text rather than clipping it.
- No essential meaning depends only on color, alpha, scale, or shadow.

## Testing strategy

### Unit tests

- initially ready trustworthy state emits one navigation effect;
- repeated ready observations do not emit duplicate navigation;
- optional degraded state still auto-navigates;
- required inspection failure does not auto-navigate using stale state;
- final required permission completion emits navigation once;
- focus-fraction calculation clamps to `0f..1f` and reaches its maximum at viewport center;
- scroll progress calculation handles empty and zero-scroll-range lists.

### Compose and instrumentation tests

- shared top bar uses the standard scaffold placement and content insets;
- permission background and local gradient backdrop are absent;
- optional section is collapsed by default and expands only after explicit interaction;
- required nodes render numbers, then checks for ready state;
- only the actionable permission pill is enabled/elevated;
- default ready inline alert is absent;
- large font and 320 dp layouts remain unclipped with 48 dp targets;
- History records use stable keys and remain individually scrollable in the outer list;
- scrolling changes center-focus values continuously and reversibly;
- `InfiniteStatusPill` remains present inside History pills;
- reduced-motion state preserves all content and semantics.

### Repository gates

Run with the project's configured Java and Android SDK environment:

```powershell
.\gradlew.bat --no-daemon app:test
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:lint
.\gradlew.bat --no-daemon app:assembleDebug
```

Runtime navigation, permission dialogs, Settings return, large-font behavior, and scroll smoothness require emulator/device verification on `develop` before INF-230 can be marked Done.

## Acceptance criteria

- Permission top bar matches other secondary screens in position and horizontal inset.
- Permission screen adds no local background or gradient backdrop.
- Exactly two new public timeline-pill composables exist.
- Required permission nodes progress from numbers to checks and the connector follows real item height.
- Optional permissions are collapsed and non-blocking.
- A ready user does not remain on or return to the readiness destination.
- The removed ready inline alert is not rendered.
- History records are individual lazy items with stable record-ID keys.
- History pills continuously interpolate toward center focus as the user scrolls.
- The History rail accent follows list scroll without changing domain state.
- Existing typography and `InfiniteStatusPill` are reused.
- All automated gates pass, and runtime-only claims remain explicitly pending until tested on `develop`.

## Expected source areas

- `presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt`
- `presentation/screen/attendance/permission/AttendancePermissionReadinessRoute.kt`
- `presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt`
- permission timeline component files and related UI models/tests
- `presentation/navigation/MainContentNavGraph.kt`
- `presentation/screen/history/HistoryScreen.kt`
- `presentation/design/components/data/InfiniteAttendanceTimelineSection.kt`
- `presentation/design/components/data/InfiniteTimelineRow.kt`
- History timeline component and scroll-focus tests

Sensitive permission/navigation files require focused review and runtime verification according to repository governance.
