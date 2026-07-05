# INF-209 WFA Bottom Tab Design

## Status

Approved design for the Android navigation update where the primary bottom tab contract changes from `Home | History | Contact | Profile` to `Home | History | WFA | Profile`.

## Context

The current worktree still uses the legacy shell implementation in:

- `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt`
- `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`

Current repo evidence shows:

- `Contact` is still a normal bottom tab destination.
- Staff bottom bar is still `Home | Contact | MyLeave | Profile`.
- Internship bottom bar is still `Home | Contact | History | Profile`.
- `WfaBooking` is not a bottom-tab-safe root route because it requires `latitude` and `longitude` arguments.
- WFA request creation currently belongs to the attendance action flow, not to the bottom navigation shell.

The product decision for this update is narrower than a full WFA redesign:

- Replace the `Contact` bottom tab with `WFA`.
- Keep WFA request creation inside `AttendanceScreen`.
- Reuse the existing booking/history/detail area as much as possible for the new WFA tab.
- Do not add a Notifications tab.
- Do not move WFA request creation into the bottom tab.

## Goals

- Change the primary bottom navigation contract to `Home | History | WFA | Profile`.
- Make the new `WFA` tab a read-oriented shell destination for existing WFA booking/history/detail data.
- Preserve `AttendanceScreen` as the only place that initiates WFA request creation.
- Reuse existing WFA booking/history/detail UI and data flow wherever practical.
- Keep shell ownership explicit so bottom-tab navigation and attendance action flow do not overlap.
- Keep the change focused on navigation/shell behavior instead of altering booking business logic.

## Non-Goals

- Redesigning the WFA domain model, booking payload, approval semantics, or backend contract.
- Moving WFA request submission out of `AttendanceScreen`.
- Turning `WfaBooking(latitude, longitude)` into a root tab route.
- Introducing a new Notifications tab or reviving `Contact` as another primary shell item.
- Refactoring unrelated auth/session, face verification, location, or profile logic.
- Rebuilding all shell navigation around the newer `MainShellNavigationPolicy` branch structure as part of this issue.

## Product Contract

### Primary bottom bar

The bottom bar contract becomes:

```text
Home | History | WFA | Profile
```

This applies to the role-specific shell variants covered by the current legacy bottom bars.

### WFA tab meaning

The `WFA` tab is a browse/history surface, not a request-creation surface.

The `WFA` tab owns:

- WFA history
- WFA status
- WFA booking detail browsing
- Existing booking/detail surfaces that can be safely reused as read flows

The `WFA` tab does not own:

- new WFA request creation
- location selection for request submission
- face verification
- request submission side effects

### Attendance meaning

`AttendanceScreen` remains the action-oriented flow for attendance and WFA request creation.

Attendance continues to own:

- entry into the WFA booking request flow
- location selection and argument gathering for WFA request submission
- navigation into `WfaBooking` with route arguments
- request submission success/error handling already tied to the attendance path

## Recommended Approach

Create a WFA bottom-tab root destination that is read-oriented and reuses the existing booking/history/detail area, while keeping `Attendance -> WfaBooking` unchanged.

This is intentionally not a direct tab-to-`WfaBooking` mapping. `WfaBooking` currently requires route arguments and represents an action flow, so it should remain a subflow launched from attendance rather than a shell root.

The recommended shape is:

```text
Bottom Tab WFA
  -> WFA root/history screen
      -> reused booking/history/detail flow
```

while request creation remains:

```text
AttendanceScreen
  -> WfaBooking(latitude, longitude)
```

## Navigation Design

### Root and shell flow

The shell continues to follow the existing structure:

```text
Splash
  -> Login
  -> Main screen / main content nav host
```

Within the main shell, the primary destinations become:

```text
Home -> History -> WFA -> Profile
```

### WFA read flow

The WFA tab should open a root WFA browse destination. That root may be a new dedicated screen or a safe adapter/root wrapper around existing WFA booking-history content, but it must behave like a bottom-tab root.

Expected read flow:

```text
WFA tab
  -> WFA root/history surface
      -> booking detail surface
```

### WFA request flow

The request flow remains action-oriented and separate from shell ownership:

```text
Home / attendance action
  -> AttendanceScreen
      -> WFA request entry
          -> WfaBooking(latitude, longitude)
```

This separation is mandatory. The WFA tab must not become an alternate entry point for request creation.

## Selected-State Rules

Selected-state must follow shell ownership rather than broad domain similarity.

### WFA selected routes

The `WFA` tab is selected when the user is on:

- the WFA root/history tab destination
- booking/detail routes that are owned by the WFA tab browse flow

### WFA not selected routes

The `WFA` tab is not selected when the user is on:

- `AttendanceScreen`
- `WfaBooking(latitude, longitude)` request creation flow
- other attendance-owned action routes

This keeps the shell honest:

- `WFA tab` = read / browse ownership
- `Attendance` = write / request ownership

## Error Handling Rules

### WFA tab empty state

If the user has no WFA history/detail data available, the WFA tab remains open and shows an empty state.

It must not redirect to `AttendanceScreen` or `Home` merely because there is no WFA data yet.

### WFA tab load failure

If the WFA history/detail surface fails to load, the user stays on the WFA tab surface and receives an inline error state or retry affordance.

The fallback is local to the WFA read flow, not a cross-navigation redirect.

### WFA detail resolution failure

If a booking/detail item cannot be resolved, the flow should surface a light error and return to the WFA read surface rather than jumping into attendance/request flow.

### Attendance request failure

If a WFA request fails during submission from attendance, error handling remains in the existing attendance/WFA request path.

This design does not move submission error handling into the WFA tab.

## File and Boundary Impact

### Likely modified files

Primary shell/navigation files:

- `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt`
- `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- `app/src/main/res/values/strings.xml`

Potential reuse points for WFA read flow:

- `app/src/main/java/com/example/infinite_track/presentation/screen/home/details/DetailsMyBooking.kt`
- any existing WFA booking/history/detail cards, adapters, or screens already used in the current booking area

### Files that should remain stable unless a direct integration gap is discovered

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt`
- attendance face/location flow files
- domain and data-layer booking logic

### Boundary rule

Allowed change surface:

- shell route contract
- bottom bar items and labels
- selected-state policy
- WFA root browse destination wiring
- safe reuse wiring for WFA history/detail browsing

Protected change surface:

- attendance-owned WFA request creation
- WFA request argument contract
- booking submit business logic
- backend semantics

## Verification Plan

### Static verification

Run at minimum:

```bash
./gradlew app:assembleDebug
./gradlew app:test
./gradlew app:lint
```

### Shell verification

Verify both current shell variants render the intended order:

```text
Home | History | WFA | Profile
```

Checks:

- `Contact` is no longer a primary tab.
- `MyLeave` is no longer a primary tab.
- `WFA` appears as the third primary tab.
- The tab order is stable across the role variants that still use the legacy bottom bars.

### Selected-state verification

Check:

- `Home` route selects Home.
- `History` route selects History.
- WFA root/history route selects WFA.
- WFA-owned detail browsing keeps WFA selected.
- `Profile` routes select Profile.
- `AttendanceScreen` and request-creation routes do not falsely select WFA.

### WFA read-flow verification

Check:

- tapping `WFA` opens the read/history-oriented WFA surface
- existing booking/history/detail data is reachable from that tab
- empty state stays within the WFA tab
- load failure stays within the WFA tab with retry/error UI

### Attendance request-flow verification

Check:

- WFA request creation still starts from `AttendanceScreen`
- WFA booking request still receives and uses its existing route arguments
- WFA request success/failure handling remains functionally unchanged unless an intentional shell-related adjustment is made

### Regression smoke

If emulator/device runtime is available, verify:

- Splash -> Login/Main still works
- Home, History, WFA, and Profile shell navigation all open the expected roots
- WFA browse/detail navigation works without leaking into request flow
- Attendance can still create a WFA request
- Profile child routes still behave correctly after the shell change

If runtime verification cannot be run, final status must remain `Needs Verification` for emulator/device shell validation.

## Acceptance Criteria

- The bottom bar contract is `Home | History | WFA | Profile`.
- `WFA` replaces `Contact` as the third primary tab.
- `WFA` tab is read/history/detail oriented.
- WFA request creation remains owned by `AttendanceScreen`.
- Existing booking/history/detail flows are reused rather than reimplemented wherever practical.
- Selected-state follows shell ownership correctly.
- Empty/error states for WFA browsing stay within the WFA surface.
- Build, test, and lint checks pass.
- Runtime shell verification is either recorded or explicitly left as `Needs Verification`.
