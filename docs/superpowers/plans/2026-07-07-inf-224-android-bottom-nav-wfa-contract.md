# INF-224 — Android Bottom Navigation WFA Contract

## Status

Implementation note for the Android navigation source-of-truth update.

## Product decision

The primary Android bottom bar contract is now:

```text
Home | History | WFA | Profile
```

This supersedes the older `Home | History | Contact | Profile` interpretation from INF-163 and the older INF-214 idea that WFA requests should be surfaced inside the Contact tab.

## Tab ownership

### Home

Personal cockpit / Today dashboard.

### History

My Attendance Report / attendance history.

### WFA

WFA booking/request history and status list. The WFA tab is a read-oriented shell surface for:

- WFA booking history
- WFA request list/status
- approved / pending / rejected request states
- detail booking/request browsing when available
- request tracking after submission

The WFA tab must not own active attendance action or request creation.

### Profile

Identity, account, document, settings, and support/contact access. Contact/support is a secondary Profile/Help surface, not a primary bottom tab.

## Attendance ownership

`AttendanceScreen` remains the owner of:

- attendance screen
- work-mode selection
- `LocationSearch`
- `FaceScanner`
- `WfaBooking` creation flow
- check-in/check-out action

The active WFA creation path remains:

```text
AttendanceScreen -> WfaBooking(latitude, longitude)
```

The WFA bottom tab must not navigate directly to `WfaBooking`, because that route requires location arguments and represents an action/creation flow.

## Current Android implementation evidence

- `WfaShellNavigationContract.staffItems()` and `internshipItems()` define the shell items as `Home | History | WFA | Profile`.
- `Screen.Wfa` is registered in `MainContentNavGraph` and opens `WfaHistoryScreen`.
- `WfaHistoryScreen` uses the existing booking history state and status filter surface.
- `Screen.Contact` remains available from Profile, but it is not a primary bottom tab.
- `WfaShellNavigationContractTest` covers role item order, WFA selected state, Profile-owned Contact routes, and hiding the bottom bar in attendance/action routes.

## Explicit non-goals for INF-224

- Do not move WFA back under Contact.
- Do not make Contact a primary bottom tab.
- Do not make WFA tab own check-in/check-out or WFA creation.
- Do not redesign WFA UI visually.
- Do not change backend booking/history contracts.
- Do not change auth/session behavior.
- Do not fold full INF-209 navigation graph hardening into this issue.

## Follow-up notes

Remaining technical navigation hardening belongs to INF-209 or a dedicated follow-up, including:

- replacing blacklist-style bottom-bar visibility with a whitelist policy
- separating root main container route from the Home dashboard route
- removing raw route strings from attendance `NavigationTarget`
- centralizing navigation result keys
