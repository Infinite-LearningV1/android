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
