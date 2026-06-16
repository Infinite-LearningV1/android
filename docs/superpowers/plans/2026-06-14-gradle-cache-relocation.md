# Gradle Cache Relocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist Gradle user home to `D:\Java_Home`, remove the legacy Gradle cache folders from `C:\Users\Febriyadi\.gradle`, and rebuild the Android project so Gradle runs from the new location.

**Architecture:** Treat this as a machine-level environment migration, not an Android application code change. Persist `GRADLE_USER_HOME` at the Windows user-environment level, mirror it into the current shell so verification can run immediately, stop old Gradle daemons, clean only the old cache folders that are safe to remove, then rebuild the repo and confirm new Gradle activity appears under `D:\Java_Home`.

**Tech Stack:** Windows user environment variables, PowerShell, Gradle Wrapper (`gradlew.bat`), Android Gradle Plugin, existing Android repo at `E:\skrisi\android`

---

## File map

- Verify: `docs/superpowers/specs/2026-06-14-gradle-cache-relocation-design.md` — approved design for this environment migration.
- Verify: `gradle/wrapper/gradle-wrapper.properties` — confirms wrapper distributions resolve from `GRADLE_USER_HOME`.
- Verify: `gradle.properties` — confirms the repo does not already override Gradle user home.
- No application source files should be modified during execution.
- No Android runtime code, DI modules, or build scripts should be edited unless the plan fails and a new design is approved.

### Task 1: Persist `GRADLE_USER_HOME` to `D:\Java_Home`

**Files:**
- Verify: `docs/superpowers/specs/2026-06-14-gradle-cache-relocation-design.md`
- Verify: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Confirm the approved design file exists before changing machine environment**

Run:

```powershell
if (Test-Path "E:\skrisi\android\docs\superpowers\specs\2026-06-14-gradle-cache-relocation-design.md") { 'DESIGN_READY' } else { throw 'Design spec missing' }
```

Expected: `DESIGN_READY`

- [ ] **Step 2: Confirm the wrapper is configured to use `GRADLE_USER_HOME`**

Run:

```powershell
Select-String -Path "E:\skrisi\android\gradle\wrapper\gradle-wrapper.properties" -Pattern "distributionBase=GRADLE_USER_HOME","zipStoreBase=GRADLE_USER_HOME"
```

Expected: two matches, one for `distributionBase=GRADLE_USER_HOME` and one for `zipStoreBase=GRADLE_USER_HOME`

- [ ] **Step 3: Ensure the target Gradle home directory exists**

Run:

```powershell
if (-not (Test-Path "D:\Java_Home")) { New-Item -ItemType Directory -Path "D:\Java_Home" | Out-Null }
Get-ChildItem "D:\Java_Home"
```

Expected: command succeeds without error; the directory exists even if it is empty

- [ ] **Step 4: Persist the user-level environment variable**

Run:

```powershell
[Environment]::SetEnvironmentVariable('GRADLE_USER_HOME', 'D:\Java_Home', 'User')
[Environment]::GetEnvironmentVariable('GRADLE_USER_HOME', 'User')
```

Expected: the second line prints exactly `D:\Java_Home`

- [ ] **Step 5: Mirror the same value into the current shell so verification can run immediately**

Run:

```powershell
$env:GRADLE_USER_HOME = 'D:\Java_Home'
$env:GRADLE_USER_HOME
```

Expected: prints exactly `D:\Java_Home`

### Task 2: Stop old Gradle daemons and verify the new home is active for this shell

**Files:**
- Verify: `gradle.properties`

- [ ] **Step 1: Confirm the repo does not already force another Gradle home**

Run:

```powershell
Select-String -Path "E:\skrisi\android\gradle.properties" -Pattern "org.gradle.caching","gradle.user.home","GRADLE_USER_HOME"
```

Expected: no match for a repo-level Gradle user home override; empty output is acceptable

- [ ] **Step 2: Stop any running Gradle daemons before cleanup**

Run:

```powershell
& "E:\skrisi\android\gradlew.bat" --stop
```

Expected: output such as `Stopping Daemon(s)` or `No Gradle daemons are running`

- [ ] **Step 3: Run a lightweight Gradle command that should initialize the new home if needed**

Run:

```powershell
& "E:\skrisi\android\gradlew.bat" -version
```

Expected: Gradle version output appears successfully and does not fail on wrapper initialization

- [ ] **Step 4: Verify wrapper artifacts now exist under the new Gradle home**

Run:

```powershell
Get-ChildItem "D:\Java_Home\wrapper\dists" -Recurse -ErrorAction Stop | Select-Object -First 20 FullName
```

Expected: output contains at least one Gradle wrapper distribution path, typically including `gradle-8.7-bin`

### Task 3: Remove the recommended legacy cache folders from `C:`

**Files:**
- No repo files changed

- [ ] **Step 1: Verify the old cache folders exist before deleting them**

Run:

```powershell
foreach ($path in @('C:\Users\Febriyadi\.gradle\caches', 'C:\Users\Febriyadi\.gradle\wrapper\dists')) {
  if (Test-Path $path) { "FOUND: $path" } else { "MISSING: $path" }
}
```

Expected: ideally both paths print as `FOUND`; if one is already missing, note it and continue without treating that as a failure

- [ ] **Step 2: Delete the old `caches` folder only**

Run:

```powershell
if (Test-Path "C:\Users\Febriyadi\.gradle\caches") {
  Remove-Item "C:\Users\Febriyadi\.gradle\caches" -Recurse -Force -Confirm:$false
}
Test-Path "C:\Users\Febriyadi\.gradle\caches"
```

Expected: final line prints `False`

- [ ] **Step 3: Delete the old wrapper distributions folder only**

Run:

```powershell
if (Test-Path "C:\Users\Febriyadi\.gradle\wrapper\dists") {
  Remove-Item "C:\Users\Febriyadi\.gradle\wrapper\dists" -Recurse -Force -Confirm:$false
}
Test-Path "C:\Users\Febriyadi\.gradle\wrapper\dists"
```

Expected: final line prints `False`

- [ ] **Step 4: Confirm the broader old Gradle home directory was not deleted wholesale**

Run:

```powershell
Test-Path "C:\Users\Febriyadi\.gradle"
```

Expected: usually `True`; if `False`, stop and record that cleanup went wider than planned

### Task 4: Rebuild the Android project from the new Gradle home

**Files:**
- No application files changed
- Verify: `app/build.gradle.kts`

- [ ] **Step 1: Run a clean build using the new `GRADLE_USER_HOME`**

Run:

```powershell
& "E:\skrisi\android\gradlew.bat" clean
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Assemble the debug app from the repository root**

Run:

```powershell
& "E:\skrisi\android\gradlew.bat" app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` and an APK output under `E:\skrisi\android\app\build\outputs\apk\debug\`

- [ ] **Step 3: Verify dependency/cache activity exists under the new Gradle home after the rebuild**

Run:

```powershell
Get-ChildItem "D:\Java_Home\caches" -ErrorAction Stop | Select-Object -First 20 FullName
```

Expected: non-empty output showing Gradle cache directories under `D:\Java_Home\caches`

- [ ] **Step 4: Verify the debug APK exists after the rebuild**

Run:

```powershell
Test-Path "E:\skrisi\android\app\build\outputs\apk\debug\app-debug.apk"
```

Expected: `True`

### Task 5: Record handoff status and IDE follow-up

**Files:**
- No repo files changed unless the execution workflow explicitly records a follow-up note elsewhere

- [ ] **Step 1: Re-read the user-level environment variable from Windows to confirm persistence survived the build**

Run:

```powershell
[Environment]::GetEnvironmentVariable('GRADLE_USER_HOME', 'User')
```

Expected: exactly `D:\Java_Home`

- [ ] **Step 2: State the Android Studio follow-up explicitly**

Use this exact handoff text in the execution summary:

```text
Android Studio should be restarted once so the IDE picks up the updated GRADLE_USER_HOME consistently. Until that restart is confirmed, IDE-side inheritance remains Needs Verification even if terminal builds are already successful.
```

Expected: the final summary includes this exact note

- [ ] **Step 3: Do not create a code commit unless repository files were intentionally changed during execution**

Use this exact handoff text in the execution summary:

```text
No application source or build-script changes were planned for this migration. If execution stays machine-level only, no git commit is required.
```

Expected: the final summary explicitly states whether no commit was needed

---

## Self-review checklist

- Spec coverage: the plan covers persistent `GRADLE_USER_HOME`, immediate-shell activation, daemon shutdown, limited cleanup of `caches` and `wrapper/dists`, rebuild verification, and Android Studio restart follow-up.
- Placeholder scan: no `TODO`, `TBD`, or vague “handle later” wording remains.
- Type consistency: every path, command, and expected output uses the same target `D:\Java_Home` and the same old cache paths under `C:\Users\Febriyadi\.gradle`.
