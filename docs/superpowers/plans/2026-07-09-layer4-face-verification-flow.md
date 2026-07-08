# 2026-07-09 — Layer 4 Face Verification Flow

## Scope

Layer 4 Face Verification hardening for Android attendance flow:

- scanner result handoff is guarded so a final result is published once from the scanner screen
- final scanner result contract remains explicit: success, failed, timeout, cancelled
- low-light is introduced as scanner runtime guidance, not as a final attendance result
- low-light blocks liveness/matching until lighting improves
- face verification success returns Attendance to submitting; backend check-in/check-out remains the final attendance authority

## Contract notes

`FaceVerificationResult.SUCCESS` is the only scanner result that permits attendance submission. It does not mean attendance succeeded. Attendance success is only shown after the backend check-in/check-out use case succeeds.

`FaceVerificationResult.FAILED` and `FaceVerificationResult.TIMEOUT` stay retryable and do not submit attendance.

`LOW_LIGHT` is intentionally kept inside `LivenessState`. If the user closes the scanner while low-light guidance is visible, the scanner exit maps to `FaceVerificationResult.CANCELLED`, not failed or timeout.

## Runtime flow

```text
Attendance Ready
→ VerifyingFace
→ FaceScanner
→ face detected and positioned
→ lighting gate
  - low light: guide user, no matching, no backend submit
  - acceptable: continue liveness/matching
→ scanner success
→ Attendance Submitting
→ backend check-in/check-out
→ backend success/failure state
```

## Non-goals

- no backend endpoint changes
- no backend validation authority changes
- no full Attendance screen redesign
- no full FaceScanner redesign
- no geofence architecture changes

## Verification status

Gradle verification passes when run through PowerShell with the project Java/Android SDK environment configured for this machine:

```powershell
$env:JAVA_HOME="D:\Java_Home\java 1.8.2"
$env:ANDROID_HOME="C:\Users\Febriyadi\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
Set-Location "C:\Users\Febriyadi\.claude\worktrees\android-layer4-face-verification-flow"
```

Commands run:

```powershell
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.FaceVerificationResultContractTest" --tests "com.example.infinite_track.presentation.screen.attendance.face.FaceLightingQualityTest"
.\gradlew.bat --no-daemon app:test
.\gradlew.bat --no-daemon app:lint
```

Runtime/emulator evidence is still needed for scanner camera behavior, low-light guidance, success-to-submitting handoff, timeout, failure, and cancellation paths.
