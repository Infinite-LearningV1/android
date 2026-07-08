# Android Login Back Stack Fix

## Context

After a successful login, pressing the system Back button could return the user to the Login screen. That is incorrect for the authenticated root flow: once Login succeeds and the app enters the main shell, Back should not reveal Login again.

## Root cause

The login-success path navigated to `Screen.Home` with `popUpTo(navController.graph.startDestinationId)`. In the observed flow, Splash can already be removed/replaced before Login, so the start destination is not a reliable anchor for clearing the auth stack. Home was therefore pushed above Login, leaving Login reachable through system Back.

## Fix

Use the same root-stack clearing style already used by forced re-auth navigation:

```kotlin
popUpTo(0) { inclusive = true }
```

Applied to:

- Login success -> Home
- Splash session-valid redirect -> Home
- Splash session-invalid/manual login -> Login

## Expected behavior

- Splash -> Login -> successful login -> Home/Main shell.
- Pressing Back from Home/Main shell should exit the app or follow main-shell behavior, not return to Login.
- Forced re-auth/session-expired navigation to Login remains unchanged and still clears the stack.

## Scope guard

- No backend/auth contract changes.
- No token/session validity changes.
- No login business logic changes.
- No bottom-bar or attendance flow changes.

## Verification needed

- Build: `./gradlew app:assembleDebug`.
- Runtime smoke on emulator/device:
  - fresh unauthenticated launch -> Login -> successful login -> Home -> Back must not show Login
  - authenticated launch via Splash -> Home -> Back must not show Splash/Login
  - session-expired/forced reauth -> Login still works
