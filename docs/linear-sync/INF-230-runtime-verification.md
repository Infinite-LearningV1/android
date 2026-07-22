# INF-230 Verification Evidence

Date: 2026-07-22

Feature branch: `codex/inf-230-permission-readiness-refinement`

Verified source commit: `5435ef47333290193e2144e24e5606dccd0d0158`

## Verdict

`Needs Verification`

The non-runtime unit-test and debug-assembly gates pass. The full lint gate is not clean because of one unchanged `origin/develop` error in `HistoryScreen.kt`. Runtime and instrumentation evidence is intentionally deferred to branch `develop` by the operator. Therefore INF-230 is not recorded as Done, and the conditional legacy permission-helper cleanup has not been performed.

The initial full gates and lint classification ran at `9ee404a9e63f5f1182d983e0b37e1d171801dcb2`. Commit `8fd0129ab62e165270ab04df971949455b07022d` aligned permission feedback typography. Whole-branch review fixes were then committed at `5435ef47333290193e2144e24e5606dccd0d0158`: content-sized glass decoration, untruncated large-font snackbar text, crash-safe Face Settings recovery, and source-compatible `AttendanceMap` parameters. `app:test`, compile-only Android tests, and `app:assembleDebug` were rerun on that final source commit. Android test compilation proves only that instrumentation sources compile; it is not connected runtime evidence.

No ADB, APK installation, connected test, application-data reset, or device command was run during this Task 11 verification.

## Verification environment

The local gates used this exact PowerShell environment:

```powershell
$env:JAVA_HOME="D:\Java_Home\java 1.8.2"
$env:ANDROID_HOME="C:\Users\Febriyadi\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

The folder name is recorded exactly as configured. It is not evidence of the runtime vendor or reported Java version.

## Fresh local gates

| Gate | Result | Evidence |
| --- | --- | --- |
| `.\gradlew.bat --no-daemon app:test` | Pass; exit `0` in 142.7 seconds at `5435ef4` | 103 JUnit XML suites, 562 tests, 0 failures, 0 errors, 5 skipped |
| `.\gradlew.bat --no-daemon app:assembleDebug` | Pass; exit `0` at `5435ef4` | `BUILD SUCCESSFUL in 44s` after a clean serialized rebuild; 45 actionable tasks, 20 executed |
| `.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin` | Pass; exit `0` in 139.4 seconds at `5435ef4` | Instrumentation sources and source-compatibility fixtures compiled only, with no emulator/device connection and no instrumentation test execution |
| `.\gradlew.bat --no-daemon app:lint` | Fail; exit `1` | `1 errors, 302 warnings`; `HistoryScreen.kt:131` reports `UnusedMaterial3ScaffoldPaddingParameter` |

The lint error is not introduced by INF-230. `git diff origin/develop -- app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryScreen.kt` is empty, and the same `) { _ ->` source exists on `origin/develop`. This classification does not turn the full lint gate into a pass; it remains a pre-existing repository failure.

Lint report generated locally at:

```text
app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt
```

## Architecture and scope audits

The following commands were run from the feature worktree:

```powershell
rg -n "LocalContext|rememberLauncherForActivityResult|shouldShowRequestPermissionRationale|ACTION_APPLICATION_DETAILS_SETTINGS|ACTION_LOCATION_SOURCE_SETTINGS|NavController|SnackbarHostState" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt
```

Result: pass; exit `1` with no matches. The stateless Screen does not own platform launchers, Settings, navigation, or snackbar state.

```powershell
rg -n "Context|Activity|NavController|ImageVector|SnackbarDuration|Manifest.permission" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt app/src/main/java/com/example/infinite_track/domain/model/attendance/permission app/src/main/java/com/example/infinite_track/domain/use_case/attendance/permission
```

Result: pass; exit `1` with no matches. The ViewModel and domain boundary do not contain the forbidden Android, navigation, icon, or snackbar types.

```powershell
rg -n "\.blur\(|RenderEffect|dropShadow|innerShadow|FontWeight\.SemiBold" app/src/main/java/com/example/infinite_track/presentation/design/components/status app/src/main/java/com/example/infinite_track/presentation/design/components/state app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission
```

Initial result at `9ee404a`: not clean; exit `0`. It found the following two `FontWeight.SemiBold` usages, both inherited from `origin/develop` but retained in files modified by INF-230:

- `PermissionHeroCard.kt:56`
- `PermissionProgressHeader.kt:41`

Final result at `8fd0129`: pass; exit `1` with no matches. The correction replaces both uses with the registered `FontWeight.Medium` feedback weight; no blur, render effect, shadow API, or `SemiBold` match remains in the audited paths.

```powershell
git diff origin/develop -- app/src/main/java/com/example/infinite_track/di/NetworkModule.kt app/src/main/res/xml/network_security_config.xml
```

Result: pass; exit `0` with empty output. INF-230 does not change either sensitive network configuration file.

## Runtime and instrumentation decision

The operator explicitly chose to run runtime verification after the work is integrated into branch `develop`. All rows below therefore remain `Needs Verification`; none are inferred from unit tests or compilation.

A prior Task 9 diagnostic on the feature branch found an Android 17 / API 37 emulator. A targeted connected run started seven tests but failed before product assertions in Espresso with `NoSuchMethodException: android.hardware.input.InputManager.getInstance`. That diagnostic is historical context only: it is not counted as a runtime pass, and it was not repeated during Task 11.

## Deferred runtime matrix

For every row, the build commit is `Pending develop commit`; `8fd0129ab62e165270ab04df971949455b07022d` is only the feature-source commit used for the final non-runtime verification. Evidence paths must be filled with sanitized screenshots or concise logs from a disposable emulator. Do not attach credentials, tokens, personal data, raw authentication logs, or large recordings.

| Scenario | Required emulator/API | Exact setup on `develop` | Expected result | Observed result | Status / evidence |
| --- | --- | --- | --- | --- | --- |
| Fresh install and normal request ownership | Disposable images below Android 10, Android 10, Android 11+, Android 13+ | Fresh install and cleared app state on the selected disposable emulator | First-time location, camera, notification, and background requests originate from Permission Readiness; downstream paths do not auto-request | Not executed by operator decision | `Needs Verification`; evidence pending |
| Location denied | Same four version groups | Deny foreground location from readiness | Required access remains blocked with inline recovery; no navigation to Attendance | Not executed | `Needs Verification`; evidence pending |
| Coarse-only location | Supported image in each relevant version group | Grant approximate/coarse and deny precise/fine | Precise-location requirement remains blocked | Not executed | `Needs Verification`; evidence pending |
| Camera denied | Same four version groups | Grant precise location, deny camera from readiness | Camera stays action-required and required navigation remains blocked | Not executed | `Needs Verification`; evidence pending |
| Permanent denial | Android 11+ and Android 13+ | Request from readiness, deny until rationale is unavailable, then use recovery | UI offers application Settings recovery and refreshes OS truth after return | Not executed | `Needs Verification`; evidence pending |
| Device location off | Same four version groups | Grant required runtime permissions, disable device location | Device-location row blocks entry and opens location-source Settings only from Route | Not executed | `Needs Verification`; evidence pending |
| Notification skipped | Android 13+ | Leave notification permission denied while all required access is ready | Optional row is degraded/actionable but Attendance remains enterable | Not executed | `Needs Verification`; evidence pending |
| Background location skipped | Android 10 and Android 11+ | Leave background location denied while all required access is ready | Automatic monitoring is degraded; manual Attendance remains available | Not executed | `Needs Verification`; evidence pending |
| Return from application Settings | Android 11+ and Android 13+ | Open app Settings from permanent-denial recovery, change access, return | Route refreshes from OS state exactly once and clears stale recovery state | Not executed | `Needs Verification`; evidence pending |
| Return from location-source Settings | Same four version groups | Open location Settings, enable device location, return | Route refreshes device-location truth and unlocks only when all required items are ready | Not executed | `Needs Verification`; evidence pending |
| Downstream precise-location revocation | Android 11+ and Android 13+ | Enter ready flow, revoke precise location, resume Attendance | Persistent recovery is shown, map/location updates do not start, and no normal request auto-launches | Not executed | `Needs Verification`; evidence pending |
| Downstream camera revocation | Android 11+ and Android 13+ | Enter ready flow, revoke camera before Face Scanner, choose explicit recovery | Face Scanner offers user-triggered exceptional recovery; permanent denial opens app Settings | Not executed | `Needs Verification`; evidence pending |
| Feedback and accessibility surfaces | At least API 26 plus Android 13+ | Exercise core readiness states, inline Retry, snackbar action/dismiss, confirmation/status dialog, 320 dp and font scale 2.0 | API-26-safe glass has no forbidden blur; content remains readable, unclipped, semantically coherent, and targets remain at least 48 dp | Not executed | `Needs Verification`; sanitized visual evidence pending |
| Face-result rescue | Supported `develop` runtime image | Execute successful, recoverable failure, cancellation, unknown-result, and navigation-away/back cases | One sanitized snackbar is shown per result, state is cleared, and feedback does not replay | Not executed | `Needs Verification`; evidence pending |

## Conditional helper cleanup

Cleanup is deliberately not performed. The runtime gate required by Phase 4 has not passed, so these defensive paths remain in the source tree:

- `LocationPermissionHelper`
- `LocalLocationPermissionHelper`
- `LocationPermissionDialog`
- the related `AttendanceScreenState`, `AttendanceViewModel`, `MainActivity`, and `InfiniteTrackApp` adapters

The preservation audit still finds these references, as required while runtime verification is deferred:

```powershell
rg -n "LocationPermissionHelper|LocalLocationPermissionHelper|LocationPermissionDialog|showPermissionDialog|permissionResult|permissionMessage|onPermissionDialogResult" app/src/main app/src/test app/src/androidTest
```

Deletion may occur only after every six-item Phase 4 runtime gate check passes on disposable emulator coverage from `develop`. Until then, the issue remains `Needs Verification` and must not be promoted to Done.

## Integration requirements remaining

1. Integrate or reproduce the feature source on `develop` without changing the approved permission semantics.
2. Run the targeted instrumentation suites and the full runtime matrix on disposable, compatible emulator images.
3. Record the exact `develop` commit, emulator/API, setup, observations, verdict, and sanitized evidence for every row.
4. Resolve or baseline the unrelated `HistoryScreen.kt:131` lint error through its owning work, then rerun `app:lint` before claiming a clean lint gate.
5. Delete the legacy permission helper only if all six Phase 4 runtime checks pass; otherwise retain it and record the failed row.
6. Obtain final code-review approval before any Done transition, merge, push, or Linear status update.
