# INF-223 Attendance Permission Gate Follow-up Design

## Context

The Attendance screen has three required readiness checks before a user can
enter the manual attendance flow:

1. precise foreground location permission;
2. camera permission; and
3. device location services enabled.

Notification permission and background location permission are optional
capabilities. Google Play Services availability and geofence registration
health affect reminder or active-monitoring capability, but they do not block
the manual Work Mode flow.

The merged INF-223 implementation added a second Compose side effect that opens
the permission panel when the geofence runtime reports selected registration
blockers. That effect bypasses the existing `canContinue` decision and can show
the panel even after all three required readiness checks are complete.

## Product Rule

The permission panel is an initial gate only when at least one of the three
required readiness checks is incomplete.

- While required readiness is loading, do not open the panel.
- When required readiness finishes and `canContinue` is `false`, open the panel
  once for that Attendance screen entry.
- When `canContinue` is `true`, show Work Mode as the primary Attendance
  experience without opening the permission panel.
- Optional capability degradation must not open the panel automatically.
- Users can still open the permission panel manually from the Attendance top
  bar.

## Chosen Approach

Use the existing permission-readiness state as the sole authority for automatic
panel visibility. Remove the geofence-runtime-driven auto-open side effect from
`AttendanceScreen`.

The existing runtime reconciliation delivery remains unchanged. Geofence
readiness continues to update monitoring state and can still be inspected or
recovered through a user-initiated permission-panel action. It no longer owns
initial Attendance navigation or overlay visibility.

This approach is preferred over guarding the second side effect because a
single visibility authority prevents the required and optional readiness models
from drifting apart again.

## State and UI Flow

1. `AttendancePermissionReadinessViewModel` refreshes platform readiness.
2. `AttendanceScreen` collects `AttendancePermissionReadinessUiState` with the
   lifecycle-aware Compose collector.
3. The existing initial gate waits until `isLoading` is `false`.
4. If `canContinue` is `false` and the initial check has not been handled, the
   screen opens `AttendancePermissionPanelHost`.
5. Otherwise the panel remains closed and Work Mode stays visible.
6. Manual permission actions continue to set `showPermissionPanel = true`.
7. Geofence runtime changes continue through reconciliation and monitoring UI,
   but cannot set `showPermissionPanel` automatically.

## Testing

Add regression coverage at the pure visibility-decision boundary:

- loading readiness keeps the panel closed;
- all three required checks ready keeps the panel closed;
- any required check missing opens the panel once;
- a geofence runtime blocker with `canContinue == true` does not open the panel;
- manual open behavior remains available.

The focused unit tests must fail against the merged behavior before the
implementation change and pass afterward. The final verification includes:

- focused permission visibility tests;
- `app:testDebugUnitTest`;
- `app:compileDebugAndroidTestKotlin`;
- `app:lintDebug`; and
- `app:assembleDebug`.

Device verification should confirm both entry paths:

1. all three required checks ready leads directly to Work Mode; and
2. one required check missing shows the permission panel first.

## Non-goals

- Changing which three readiness checks are required.
- Making notification or background location mandatory for manual attendance.
- Removing geofence runtime reconciliation or monitoring status.
- Redesigning the permission panel or Work Mode UI.
- Changing backend attendance truth or submission behavior.

## Acceptance Criteria

- With precise location, camera, and device location ready, entering Attendance
  does not display the permission panel.
- With any required readiness check incomplete, entering Attendance displays
  the permission panel before the user proceeds with Work Mode.
- Missing background location, denied notifications, unavailable Play Services,
  or degraded geofence registration cannot auto-open the panel when the three
  required checks are ready.
- The permission panel remains manually accessible.
- Existing geofence reconciliation and monitoring behavior remains intact.
