# INF-265 WFA Request Visual Redesign

Date: 2026-07-28

Status: Approved design

Target: Android, Jetpack Compose
Base: `develop` at merge commit `94e89d9e`

## Objective

Redesign the existing WFA request Form, Review, and Result states to follow the
approved visual reference while preserving the transaction behavior delivered
by INF-265.

The first and second reference panels are one Form page. Their sections must be
stacked in a single vertically scrollable surface rather than implemented as
separate navigation destinations.

## Product Decisions

1. The Form remains one navigation destination and one `LazyColumn`.
2. The selected-location map is a real Google Maps preview.
3. The map is read-only: pan, zoom, rotate, tilt, and toolbar gestures are
   disabled so vertical gestures always scroll the page.
4. Form, Review, Result, loading, and failure states do not paint a page
   background. They remain transparent so the root/Home background is visible.
5. Cards may keep their shared glass surfaces, borders, shadows, and semantic
   colors.
6. Every visual element is rendered through an existing shared component.
   A new shared component is allowed only when no appropriate component exists.
7. Typography, colors, spacing, radius, density, and surface styling come from
   the existing core typography and design tokens.
8. The UI must not claim that a schedule conflict check passed because the
   current backend/domain contract does not provide that evidence.

## Scope

### Form

The Form is one scrollable page with these sections in order:

1. Top bar with Back, centered WFA Request title, and Close.
2. Selected location card:
   - location icon, display name, and formatted address;
   - real read-only Google Maps preview;
   - selected-location marker;
   - server-owned radius circle;
   - radius label and a valid-location status indicator.
3. Employee information card:
   - full name;
   - division;
   - supporting copy explaining that data comes from the user profile.
4. Request detail section:
   - controlled shared date picker;
   - controlled shared reason dropdown;
   - conditional other-reason input;
   - optional notes textarea with character counter.
5. Eligibility/information card containing only facts supported by the current
   state:
   - the selected location has valid coordinates;
   - the validation radius was loaded from server configuration;
   - required request fields can be reviewed before submission.
6. Full-width Continue action at the end of the scroll.

Existing field validation, state ownership, and test tags remain intact.

### Review

The Review page contains:

1. Top bar with Back and Close.
2. Review title and supporting guidance.
3. Location card with the same read-only map, marker, radius circle, and policy
   status used by the Form.
4. Request detail card for date, reason, other reason when applicable, and
   notes when present.
5. Employee information card.
6. Server-radius policy card.
7. Full-width Submit action followed by an outlined Edit action.

Submitting disables backward/edit navigation and keeps the existing duplicate
submission guard.

### Result

The successful Result page contains:

1. Top bar with Back and Close.
2. Shared success hero with a large semantic success visual, title, and
   supporting message.
3. Request detail card containing booking ID, backend status, date, location,
   reason, and applied radius.
4. Outlined secondary action to return to Attendance/status.
5. Primary action to return Home.

The loading and failure variants continue to use shared state components and
the existing failure-action mapping.

## Transparent Surface Ownership

`InfiniteTrackApp` owns the global background. `MainScreen`, its `Scaffold`, and
the nested navigation host already use transparent containers.

The WFA screens therefore:

- do not call `background(...)` on their page root;
- do not introduce a replacement gradient, solid color, or duplicated home
  background;
- use `Color.Transparent` only where a Compose container explicitly requires a
  container color;
- continue using shared glass cards for readable foreground content.

This keeps the root background visually continuous across Home and WFA Request.

## Component Architecture

Screens remain orchestration-only and compose presentation-level components.
They do not render raw text, icons, buttons, progress indicators, colors, or
typography.

### Components to reuse

- `InfiniteTopBar`
- `InfiniteCard`
- `InfiniteSectionHeader`
- `InfiniteInfoRow`
- `InfiniteStatusPill`
- `InfiniteButton`
- `InfiniteLoadingState`
- `InfiniteErrorState`
- `InfiniteSupportingText`
- `DatePickerButton`
- `InfiniteTrackTextArea`
- `AttendanceMap`

### Existing components to adapt

1. `AttendanceMap`
   - accept an interaction/read-only setting;
   - disable every map gesture and map toolbar in read-only mode;
   - retain existing behavior for Attendance consumers by default.

2. `InfiniteTrackDropDown`
   - become a controlled component whose selected label is owned by the caller;
   - use core typography, theme colors, design spacing, and a minimum 48 dp
     touch target;
   - keep a compatibility overload only if a live consumer requires it.

3. `InfiniteTrackTextArea`
   - support optional maximum length and character counter without moving draft
     ownership into the component;
   - continue using core typography and theme colors.

### New shared components allowed

New components are placed under `presentation/design/components` or the
existing shared presentation component hierarchy, never under a feature-local
`wfa_request/components` package.

Expected reusable molecules:

- read-only location map card;
- semantic checklist/info item;
- result hero.

Each public component accepts `modifier: Modifier = Modifier`, uses design
tokens, and exposes data/callback parameters rather than a ViewModel.

## Map Model

The map preview is derived entirely from the current `WfaCandidateLocation` and
server configuration:

- center: candidate latitude and longitude;
- marker category: WFA;
- circle center: the same coordinate;
- circle radius: server-provided `radiusMeters`;
- camera: one focus effect centered on the candidate;
- permission: the preview does not request live device location and does not
  enable the Google Maps my-location layer.

The component must show a deterministic shared error/placeholder state when:

- coordinates are invalid;
- Google Maps cannot render;
- the Maps runtime is unavailable.

Map failure does not erase the textual location or block review when the domain
state is otherwise valid.

## State and Data Boundaries

No domain or repository contract changes are required.

- `WfaRequestViewModel` remains the transaction owner.
- Form controls continue to dispatch the existing `WfaRequestEvent` types.
- Review renders the normalized draft already owned by the ViewModel.
- Result renders only backend-confirmed submission data.
- Radius remains backend-authoritative and read-only.
- The redesign does not add optimistic schedule-conflict claims.

## Navigation

- Back from Form returns to Attendance.
- Close from Form returns to Attendance.
- Back or Close from Review returns to the editable Form when not submitting.
- Back or Close from Result returns to Attendance when not submitting.
- Result actions retain the existing Attendance and Home destinations.
- The graph-scoped `WfaRequestViewModel` and exact draft snapshot are preserved.

## Error Handling

- Configuration failure uses `InfiniteErrorState` and the existing retry/back
  action mapping.
- Field errors remain adjacent to their controls.
- Submission failure uses the existing typed mapping and recovery behavior.
- Map rendering failure falls back locally inside the location component and
  does not replace the full page.
- Submitting keeps a visible loading state and blocks duplicate actions.

## Accessibility

- Every interactive target is at least 48 dp.
- Top-bar Back and Close have localized content descriptions.
- The read-only map has one concise semantic description and is not exposed as
  an interactive control.
- Cards use merged semantics only where doing so does not hide field-level
  controls.
- Status is communicated by text and icon, not color alone.
- Content remains reachable with large font scales and compact phone heights.
- Form and Review actions remain inside the scroll so no content is hidden by
  nested fixed footers.

## Testing

### Unit and component tests

- map preview model contains the selected marker and server radius;
- read-only map disables gestures while existing interactive map behavior stays
  enabled by default;
- controlled dropdown displays and updates the caller-owned selection;
- eligibility copy never asserts unsupported schedule-conflict validation;
- status-to-visual mappings remain exhaustive.

### Compose instrumentation tests

- Form exposes location, map, employee, detail, eligibility, and Continue
  sections in one scrollable destination;
- Form can scroll from the first reference section to the second;
- Review shows map and normalized request details;
- Result shows backend-confirmed details and both exit actions;
- Back and Close dispatch the correct navigation behavior;
- transparent page roots do not introduce an opaque background;
- existing stable WFA test tags and navigation graph tests continue to pass.

### Verification gates

- focused WFA unit tests;
- `app:compileDebugAndroidTestKotlin`;
- `app:testDebugUnitTest`;
- `app:lintDebug`;
- `app:assembleDebug`;
- `git diff --check`;
- device or emulator inspection for Google Maps rendering and scroll behavior.

If no online device is available, Google Maps runtime rendering, gesture
suppression, screenshots, and end-to-end authenticated submission are reported
as Needs Verification rather than complete.

## Acceptance Criteria

1. The first two reference panels are represented by one scrollable Form
   destination.
2. Form and Review show a real read-only Google Maps preview.
3. Vertical scrolling is not intercepted by map gestures.
4. All WFA page roots are transparent and expose the root/Home background.
5. Cards and controls visually follow the approved reference using the existing
   Infinite Track design system.
6. Screens use shared components; no new feature-local visual component package
   is introduced.
7. Core typography and theme/design tokens are used consistently.
8. Unsupported schedule-conflict validation is not shown as successful.
9. Existing transaction, validation, navigation, and failure behavior remains
   unchanged.
10. Focused and full local gates pass; device-only evidence is reported
    separately.

## Non-goals

- changing WFA request API payloads or backend contracts;
- changing the location-selection flow;
- adding a live device-location layer to the preview map;
- allowing map pan, zoom, or location editing from the request screen;
- creating a new root/Home background;
- introducing a new schedule-conflict API;
- redesigning unrelated Attendance, Home, or booking-history screens.
