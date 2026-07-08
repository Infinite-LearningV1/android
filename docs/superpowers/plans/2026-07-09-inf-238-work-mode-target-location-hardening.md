# INF-238 — Work Mode & Target Location Hardening

## Scope

Harden the Attendance Work Mode & Target Location layer without redesigning the full Attendance or FaceScanner flow.

## Architecture note

- Work mode identity is represented by canonical domain model `WorkMode`.
- Target resolution is centralized in `ResolveSelectedTargetLocationUseCase`.
- Continue-to-FaceScanner eligibility is centralized in `EvaluateWorkModeEligibilityUseCase`.
- Mode selection updates selected mode, selected target location, eligibility, WFA recommendation flow, and map focus only.
- Mode selection must not remove geofences or register active monitoring geofences.
- Active monitoring geofence ownership remains after successful backend check-in in `CheckInUseCase`.
- Backend remains the final attendance validation authority.

## Verification status

- `git diff --check`: passed.
- Code scan: `AttendanceViewModel.kt` no longer contains `removeAllGeofences`, `setupGeofence`, or `addGeofence` calls.
- Code evidence: `CheckInUseCase` still starts active geofence only after successful backend check-in.
- Plain Git Bash/JBR Gradle attempts were blocked by `java.io.IOException: Unable to establish loopback connection` before Kotlin compile.
- Using the project-local PowerShell environment with `JAVA_HOME="D:\Java_Home\java 1.8.2"` resolved the loopback issue.
- `app:assembleDebug`: passed with the PowerShell env.
- `app:test`: passed with the PowerShell env.
- `app:lint`: passed with the PowerShell env.

## Needs Verification

Runtime evidence still needed on emulator/device:

- WFO selected with office target location.
- WFH selected with registered home target location.
- WFH unavailable state when home location is missing.
- WFA selected and WFA action visible.
- WFA blocked before Face Verification when approved booking is missing.
- WFA eligible flow when approved booking exists.
- Map focus changes across mode switches.
