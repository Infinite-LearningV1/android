# Gradle Cache Relocation Design

## Context

The Android repository currently relies on the default Windows Gradle user home because `GRADLE_USER_HOME` is not set in the active environment. As a result, Gradle wrapper distributions and dependency caches resolve under the default user-home path on `C:`.

The user wants Gradle cache storage to move persistently to:

```text
D:\Java_Home
```

The goal is not just a one-time terminal override. The target behavior is persistent usage for both terminal-driven Gradle commands and Android Studio usage going forward. The user also wants the old cache location cleaned up, but only with the recommended, lower-risk scope rather than deleting the entire old Gradle home.

## Goals

- Persist Gradle user home at `D:\Java_Home` for future terminal and Android Studio usage.
- Make Gradle wrapper distributions and dependency caches resolve from the new location.
- Clean up the relevant legacy cache folders from the old default Gradle home.
- Rebuild the Android project after relocation to verify the new cache location is active.
- Keep cleanup scoped tightly enough to avoid unnecessary deletion of unrelated Gradle state.

## Non-Goals

- Reconfigure project-level Gradle build cache semantics.
- Rewrite `settings.gradle.kts` or module build scripts just to force a new cache location.
- Delete the entire old Gradle home directory.
- Optimize or migrate unrelated Android Studio settings outside what is required for consistent Gradle home usage.

## Recommended Approach

Use a persistent Windows environment configuration for `GRADLE_USER_HOME` rather than a repo-local Gradle script change.

This is the recommended path because the requirement is cross-entrypoint consistency:

- terminal commands should use `D:\Java_Home`
- Gradle wrapper should use `D:\Java_Home`
- Android Studio should converge on the same Gradle home after environment refresh / IDE restart

A repo-local Gradle script approach would not reliably guarantee the same behavior across terminal and IDE, and it would also couple a machine-specific path into repository configuration.

## Design

### 1. Persistent Gradle Home Configuration

Set the Windows user environment variable:

```text
GRADLE_USER_HOME=D:\Java_Home
```

This makes the relocation machine-persistent without baking a developer-specific absolute path into the repository.

The repository itself should remain unchanged unless later verification proves a repo-level override is still required.

### 2. Legacy Cache Cleanup Scope

After the new Gradle home is active, delete only the recommended legacy cache folders from the old location:

```text
C:\Users\Febriyadi\.gradle\caches
C:\Users\Febriyadi\.gradle\wrapper\dists
```

Do not delete the entire old directory:

```text
C:\Users\Febriyadi\.gradle
```

This narrower cleanup reduces the chance of removing unrelated historical state that is not relevant to the current relocation objective.

### 3. Rebuild Verification

After configuration and cleanup, rebuild the Android project from the repository root.

Primary verification should confirm:

1. `GRADLE_USER_HOME` resolves to `D:\Java_Home` in the active shell.
2. Gradle commands for this repository complete successfully.
3. Gradle repopulates or reuses data under `D:\Java_Home` rather than the old cache folders.

Recommended verification commands:

```bash
./gradlew clean
./gradlew app:assembleDebug
```

If the first rebuild indicates additional confidence is needed, a broader project verification can also include:

```bash
./gradlew app:testDebugUnitTest
```

## Operational Notes

- Existing terminals may need to be reopened before they inherit the new persistent environment variable.
- Android Studio may need a restart before it consistently reads the updated environment.
- If Android Studio still points to old Gradle state after restart, the issue should be treated as `Needs Verification` rather than silently assumed fixed.

## Risk Assessment

### Risk: wider machine impact

Changing `GRADLE_USER_HOME` at the user-environment level can affect other Gradle projects run by the same Windows user.

### Risk: premature cleanup

If old cache folders are deleted before verifying the new path is active, fallback artifacts are lost and the next build may need to fully re-download dependencies.

### Risk: IDE/environment drift

Terminal and Android Studio may temporarily disagree until sessions are restarted.

## Verification Outcome Rules

### Done

The task can be treated as done only when:

- the persistent environment variable is set,
- the old recommended cache folders are removed,
- the Android project rebuild succeeds,
- and the active build behavior points at `D:\Java_Home`.

### Needs Verification

Use `Needs Verification` when:

- Android Studio has not yet been restarted,
- the shell has not been refreshed,
- or the build succeeds but IDE-side inheritance of the new Gradle home is not yet confirmed.

## Docs / ADR Note

This change affects local developer environment behavior, not application runtime architecture, auth/session behavior, attendance semantics, navigation shell, or release policy. A focused spec is sufficient; no ADR is required.

## PR / Release Notes

If implementation proceeds, the change should be described as local build-environment maintenance:

- persist Gradle cache home to `D:\Java_Home`
- remove legacy Gradle cache folders from the default `C:` path
- rebuild project to verify the new cache location is in effect
