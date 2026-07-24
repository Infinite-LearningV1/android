# ADR-INF-223: Geofence Runtime Reconciliation

## Status

Status: Accepted.

## Context

Attendance state, reminder registrations, and active-session geofences had
duplicated runtime ownership.  That duplication allowed Android to act on stale
local state.  The old boot path could also restore persisted geofences without
first establishing current attendance truth, creating the risk of stale alerts
after checkout, a date change, or a changed backend session.

## Decision

Android uses a backend-truth resolver and one Android reconciler with a
two-phase apply.  The resolver derives the current attendance/runtime intent
from authoritative backend data; the reconciler is the only component allowed
to persist and apply the corresponding platform registrations.  The first
phase saves the intended snapshot before the platform change, and the second
phase records the result after the platform operation completes.

The persisted runtime snapshot is schema version 2.  It contains a generation
and effective date, and it records one of `Applying`, `Applied`, or `Degraded`
application states.  Receivers validate this snapshot before accepting a
transition so a stale request ID or prior generation cannot produce a current
attendance side effect.

Notifications are generated locally through Android OS and Google Play
services geofencing only.  This feature has no Firebase Cloud Messaging message
or token path.

At boot Android enqueues one uniquely named, connected-network WorkManager
refresh.  The worker obtains current backend truth and reconciles it; it never
blindly restores persisted geometry or checkout reminders.

## Registration Identity and Event Boundaries

Every physical Google Play services registration uses the canonical request ID
format `gf2:<base36-generation>:<r|a>:<20-lowercase-hex>`.  The final segment
is the first 20 hexadecimal characters of the SHA-256 digest of the logical
ID, and `r` and `a` distinguish reminder and active registrations.  Receivers
decode this canonical form and reject IDs whose generation, kind, snapshot
state, or effective date is stale.

Reminder registrations accept ENTER and DWELL transitions, with a 120-second
loitering delay for DWELL and a 45-minute notification cooldown per logical
ID.  Active registrations accept ENTER and EXIT transitions and have a
seven-minute notification cooldown per attendance ID, transition, and logical
ID.  An active event updates inside-state and schedules its evidence before
the notification-permission decision.

Notification permission is independent from registration and reconciliation:
denial suppresses only the local notification display, not the platform
registration chosen from backend truth.  The local broadcast-receiver path
validates the event and invokes the local notification gateway.  WorkManager is
used only for uniquely named, connected evidence work; it does not restore
geofences, decide registrations, or deliver the notification.

## Cleanup Boundaries

Google Maps Platform configuration and the master-only Firebase App
Distribution workflow remain retained.  After all bounded consumers had moved
to the reconciler, the legacy presentation geofence implementation, FCM
runtime, Google Services plugin/configuration, and their obsolete consumers
were removed.  This cleanup does not change the local-only notification
decision into an FCM fallback.

## Consequences

When current truth cannot be established or the platform apply is degraded,
temporary monitoring unavailability is preferred to stale alerts.  The
generation/effective-date checks and the single reconciler make callback
handling more defensive, while WorkManager makes boot recovery retryable only
when a connected device can refresh truth.

## Rollback

Rollback is performed by reverting the responsible PR commit.  A rollback must
never restore the old checkout reminder behavior or reintroduce blind boot
registration; any follow-up must preserve backend truth as the authority.

## References

- INF-223
- `docs/superpowers/specs/2026-07-24-inf-223-geofence-runtime-hardening-design.md`
