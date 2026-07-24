# INF-238 Runtime Verification

Date: 2026-07-24

Branch: `codex/inf-238-work-mode-feedback-ux`

Source state: Task 10 cleanup based on `2754873`

## Automated evidence

| Gate | Status | Evidence |
|---|---|---|
| Debug unit suite | PASS | `app:testDebugUnitTest` completed in the JBR 17 full gate |
| Android test source compilation | PASS | `app:compileDebugAndroidTestKotlin` completed in the JBR 17 full gate |
| Debug lint | PASS | `app:lintDebug` completed in the JBR 17 full gate |
| Debug APK assembly | PASS | `app:assembleDebug` completed in the JBR 17 full gate |
| User-standard Java environment assembly | PASS | `D:\Java_Home\java 1.8.2` reports Corretto 18.0.2 and `app:assembleDebug` exited 0 |
| Legacy target contract scan | PASS | No `SelectedTargetLocation`, `ResolveSelectedTargetLocationUseCase`, or `EvaluateWorkModeEligibilityUseCase` reference remains in app sources/tests |

The exact full gate was run with command-local JBR 17 and completed with
`BUILD SUCCESSFUL`. The debug APK is available at
`app/build/outputs/apk/debug/app-debug.apk`.

## Instrumentation limitation

`emulator-5554` was online and reported Android API 37. The command
`app:connectedDebugAndroidTest` installed the test artifacts and started all 54
tests, but 53 failed at Espresso synchronization before their test bodies could
provide feature evidence:

```text
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
at androidx.test.espresso.Espresso.onIdle(Espresso.java:357)
```

This is an Android 17/API 37 and current Espresso 3.6.1 compatibility limitation,
not evidence that any INF-238 runtime scenario passed or failed. Per the issue
gate, dependency/toolchain changes are outside this branch. Runtime verification
therefore still needs a compatible emulator or physical device.

## Runtime matrix

| Runtime row | Status | Required evidence |
|---|---|---|
| WFO ready with assigned `status-today.active_location` | NEEDS DEVICE | Compatible-device walkthrough and screenshot/log |
| WFO unavailable recovery | NEEDS DEVICE | Compatible-device walkthrough and screenshot/log |
| WFO inside-range state | NEEDS DEVICE | Compatible-device location evidence |
| WFO outside-range recovery | NEEDS DEVICE | Compatible-device location evidence |
| WFH ready from admin-provisioned `/api/auth/me` location | NEEDS DEVICE | Compatible-device walkthrough and sanitized response evidence |
| WFH missing/invalid profile recovery and no employee edit action | NEEDS DEVICE | Compatible-device walkthrough and screenshot/log |
| WFA recommendation loading state | NEEDS DEVICE | Compatible-device walkthrough |
| WFA recommendation content and row-marker synchronization | NEEDS DEVICE | Compatible-device row and map interaction recording |
| WFA recommendation empty state | NEEDS DEVICE | Compatible-device walkthrough |
| WFA recommendation failure and retry | NEEDS DEVICE | Compatible-device walkthrough |
| WFA not-requested lifecycle | NEEDS DEVICE | Compatible-device walkthrough |
| WFA pending lifecycle | NEEDS DEVICE | Compatible-device walkthrough |
| WFA rejected lifecycle | NEEDS DEVICE | Compatible-device walkthrough |
| WFA approved target preserves booking/date authority | NEEDS DEVICE | Compatible-device walkthrough and sanitized request evidence |
| Search/recommendation/pick remains preview-only | NEEDS DEVICE | Compatible-device map and booking-draft walkthrough |
| Rapid WFA-to-WFO and WFH-to-WFO switching ignores stale results | NEEDS DEVICE | Compatible-device rapid-switch recording |
| Checkout continues without mode reselection | NEEDS DEVICE | Compatible-device active-session checkout walkthrough |
| Login failure uses timed inline feedback | NEEDS DEVICE | Compatible-device walkthrough |
| Login success navigates once and shows root snackbar | NEEDS DEVICE | Compatible-device walkthrough |
| Logout success shows root snackbar without stale re-auth copy | NEEDS DEVICE | Compatible-device walkthrough |
| Remote logout warning still clears local session and navigates | NEEDS DEVICE | Compatible-device controlled-network walkthrough |
| Local logout cleanup failure remains recoverable on Profile | NEEDS DEVICE | Compatible-device controlled-failure walkthrough |
| Transient Success/Info alert shows X and auto-dismisses at 4 seconds | NEEDS DEVICE | Compatible-device timed recording |
| Transient Warning/Error alert shows X and auto-dismisses at 8 seconds | NEEDS DEVICE | Compatible-device timed recording |
| Persistent recovery has no X/timer and remains until recovery | NEEDS DEVICE | Compatible-device walkthrough |
| Large-font presentation keeps target, status, and action visible | NEEDS DEVICE | Compatible-device font-scale walkthrough |
| Narrow-screen presentation keeps one primary action usable | NEEDS DEVICE | Compatible 320 dp device walkthrough |

No runtime row is marked PASS because the connected API 37 environment could
not execute Espresso interactions reliably.
