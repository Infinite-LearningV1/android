# INF-223 Runtime Verification Evidence

Date: 2026-07-25

Branch: `codex/inf-223-geofence-runtime-spec-plan`

Feature source verified before documentation commit:
`abfa9b6006fbad669782840ffc7ce2a4c5d52d70`.

## Verdict

`Needs Verification`

The complete automated gate passed on a clean worktree.  An Android 17/API 37
Google Play services emulator accepted the debug APK, so the earlier
PackageManager/StorageManager install failure did not recur.  The required
end-to-end geofence scenarios were not exercised: this task did not have a
sanitized authenticated test account and must not create or mutate a real
backend attendance session.  Therefore no runtime row below is inferred from
unit tests, source inspection, APK installation, or Android-test compilation.

## Verification Environment

The commands used process-local environment variables only:

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\jbr-17.0.14'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

`java -version` reported JBR 17.0.14.  No repository configuration or
`local.properties` was changed.

## Clean-Tree Automated Gates

At 2026-07-25T05:01:30+08:00, `git status --short` produced no output and
`git diff --check` exited `0` with no output.  The Gradle gates then ran against
the source commit named above.

| Start time (UTC+08:00) | Command | Exit | Exact result |
| --- | --- | ---: | --- |
| 05:01:30 | `git status --short` | 0 | clean: 0 status lines |
| 05:01:30 | `git diff --check` | 0 | 0 whitespace errors |
| 05:01:30 | `.\gradlew.bat --no-daemon app:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL in 17s`; 112 XML suites, 550 tests, 0 failures, 0 errors, 2 skipped |
| 05:01:48 | `.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin` | 0 | `BUILD SUCCESSFUL in 12s`; 0 compilation failures |
| 05:02:01 | `.\gradlew.bat --no-daemon app:lintDebug` | 0 | `BUILD SUCCESSFUL in 41s`; 0 lint errors and 320 warnings in the generated lint XML/text report |
| 05:02:43 | `.\gradlew.bat --no-daemon app:assembleDebug` | 0 | `BUILD SUCCESSFUL in 12s`; 0 assembly failures; debug APK produced |

The 320 lint warnings are reported here rather than hidden; the task gate had
no error-level lint blocker and Gradle returned zero.  They are repository
warnings outside this documentation-only task unless separately triaged.

## Architecture and Cleanup Audit

All commands ran at 2026-07-25T05:03:07+08:00.

| Command | Exit / matches | Assessment |
| --- | --- | --- |
| `rg -n 'presentation\.geofencing|GeofenceManager|ReminderGeofenceCandidate' app/src/main app/src/test app/src/androidTest` | exit 0; 33 lines | No legacy presentation runtime implementation remains.  `presentation.geofencing` and `GeofenceManager` appear only as forbidden-string assertions in contract tests.  The remaining `ReminderGeofenceCandidate` matches are the new domain model/use case and its tests, not the deleted presentation candidate. |
| `rg -n 'AttendancePreference' app/src/main/java/com/example/infinite_track/domain app/src/main/java/com/example/infinite_track/presentation/screen/attendance` | exit 1; 0 lines | No domain or attendance-screen consumer remains. |
| `rg -n -i 'firebase.messaging|FirebaseMessagingService|RemoteMessage|onNewToken|google-services' app build.gradle.kts gradle .github` | exit 0; 3 lines | All three are forbidden-string assertions in `LegacyGeofenceConfigurationContractTest`; no production, build, catalog, or workflow FCM/Google Services path remains. |
| `rg -n 'NotificationManager|NotificationCompat' app/src/main/java/com/example/infinite_track` | exit 0; 12 lines | Matches are local Android platform notification code in `utils/NotificationHelper.kt`. |

The additional `app:dependencies --configuration debugRuntimeClasspath` audit
completed at 05:05:28+08:00 with exit `0` and 0 `firebase-messaging` matches.

Firebase App Distribution remains master-only: the distribution workflow is
triggered on `master`, while `develop` and branch verification do not distribute
to Firebase.  This is source/workflow evidence; repository branch-protection
enforcement still requires GitHub-side review.

## Device and Runtime Evidence

At 2026-07-25T05:03:41+08:00, `adb devices -l` reported one online device:
`emulator-5554`, model `sdk_gphone16k_x86_64`, Android 17/API 37.  The device
contains `com.google.android.gms`.  `adb install -r
app\build\outputs\apk\debug\app-debug.apk` completed successfully at
05:03:53+08:00.  `adb logcat -c` exited 0 and a five-second filtered capture for
`GeofenceRuntime`, `GeofenceReceiver`, `WM-WorkerWrapper`, and
`LocationEventWorker` contained no event because no scenario was triggered.

The first bounded logcat helper attempt failed before launching `adb` because
PowerShell does not permit `RedirectStandardOutput` and `RedirectStandardError`
to target the same file.  The error was limited to the observation wrapper; a
retry with separate files launched `adb logcat` successfully and produced no
filtered output.  No emulator reset, app-data clear, account inspection, or
backend mutation was performed.

| Runtime scenario | Status | Evidence / missing prerequisite |
| --- | --- | --- |
| WFO reminder applied IDs | Needs Verification | Requires a disposable authenticated WFO account with authoritative target data and an Extended Controls location route. |
| WFH reminder ENTER/DWELL notification | Needs Verification | Requires a disposable authenticated WFH profile target plus controlled ENTER and DWELL events. |
| approved WFA reminder notification | Needs Verification | Requires a disposable backend-approved WFA booking for the effective date. |
| 45-minute duplicate suppression | Needs Verification | Requires a controlled reminder event sequence and elapsed-time/device-clock evidence. |
| active registration after backend-confirmed check-in only | Needs Verification | Requires a safe test attendance check-in and backend truth observation; not performed to avoid a real mutation. |
| active identity after process recreation | Needs Verification | Requires the safe active-session setup above, then controlled app process recreation. |
| active ENTER/EXIT inside-state update | Needs Verification | Requires active registration and controlled location transitions. |
| seven-minute active duplicate suppression | Needs Verification | Requires controlled active events and elapsed-time/device-clock evidence. |
| unique `LocationEventWorker` enqueue | Needs Verification | Requires an accepted active event and WorkManager inspection from a safe session. |
| checkout removes active and does not restore reminders when completed | Needs Verification | Requires a disposable active attendance session and completed checkout; no backend mutation was authorized. |
| reboot refreshes and rejects stale snapshot | Needs Verification | Requires a disposable test snapshot, controlled reboot, network, and backend truth refresh. |
| notification denial keeps runtime/evidence and skips notification | Needs Verification | Requires Android 13+ notification-permission denial plus a safe geofence event. |
| no FCM message/token path | Needs Verification | Static source/build/dependency audits found no FCM path, but no runtime message interception scenario was exercised. |

## PR Review Notes

### Fact

The branch changes 73 implementation/test/configuration/documentation files
relative to `origin/develop`, centered on `domain/model/geofence`,
`domain/use_case/geofence`, `data/platform/geofence`, receivers/workers,
attendance boundary/UI projection, DI, manifest, cleanup contracts, Gradle
catalog/build files, and Android workflows.  The new ADR is
`docs/adr/ADR-INF-223-geofence-runtime-reconciliation.md`; this evidence file
is `docs/linear-sync/INF-223-runtime-verification.md`.

### Assumption

The backend resolver returns current attendance, work mode, target, WFA, and
session truth before reconciliation.  This is exercised in unit/contract tests
but needs an authenticated integration environment for runtime proof.

### Mismatch

An Android 17/API 37 GMS emulator is available and accepts the APK, but it was
not used for authenticated geofence scenarios.  APK installation is not
evidence that Google Play services registrations, callbacks, WorkManager, or
notifications behave correctly.

### Risk

OS-delivered geofencing and boot behavior are runtime-sensitive.  The 320
lint warnings are non-error-level and preclude neither the successful gate nor
future warning triage.  Static removal of FCM cannot prove a device receives
no message without an explicit integration observation.

### Needs Verification

Every device-matrix row remains `Needs Verification` pending a disposable
authenticated test account, backend fixtures, controlled Google Play services
location simulation, notification-permission coverage, and a reboot exercise.

### Recommendation

Before any Done/merge promotion, run this matrix on a disposable GMS-capable
emulator or physical device using backend fixtures that cannot affect real
attendance.  Retain Firebase App Distribution as master-only and do not add an
FCM fallback for these local geofence notifications.
