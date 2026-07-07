# INF-163 Shell Navigation Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring Android main shell navigation into explicit INF-163 compliance on top of `fix/inf-163-bottom-bar-role-navigation-develop` after syncing that branch with `develop`.

**Architecture:** Keep main shell behavior centralized in `MainShellNavigationPolicy` and continue driving UI through the existing `MainBottomBar` and `MainScreen` integration points. Close the remaining contract gaps with tests first: Management must have no FAB, profile child routes that keep the bottom bar visible must keep the Profile tab selected, and English menu labels must be corrected at the string-resource source instead of being reformatted in UI code.

**Tech Stack:** Kotlin, Jetpack Compose, Jetpack Navigation, Hilt, JUnit4, Gradle

---

## File map

- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicy.kt` — single source of truth for bottom-bar routes, selected-state behavior, and role-to-FAB mapping.
- Modify: `app/src/test/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicyTest.kt` — contract tests for bottom-bar order, nested selected-state behavior, and explicit FAB policy.
- Modify: `app/src/main/res/values/strings.xml` — English bottom-bar labels; fix capitalization at the resource source.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt` — should keep consuming `fabConfigForRole()` and `shouldShowBottomBar()` with no new role branching.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/MainBottomBar.kt` — should keep using `stringResource(id = item.labelRes)` directly with no runtime label formatter.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/navigation/model/Screen.kt` and `app/src/main/java/com/example/infinite_track/presentation/navigation/model/NavigationItem.kt` — these should remain the only navigation model definitions after the branch is synced with `develop`.
- Do not modify unless merge conflicts force it: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt`, `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt`.

### Task 1: Sync the INF-163 branch with `develop` and verify the shell source of truth

**Files:**
- Verify: `app/src/main/java/com/example/infinite_track/presentation/navigation/model/Screen.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/navigation/model/NavigationItem.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/MainBottomBar.kt`

- [ ] **Step 1: Create an isolated workspace from the target branch**

```bash
git fetch origin
git worktree add ".worktrees/inf-163-shell" "fix/inf-163-bottom-bar-role-navigation-develop"
```

Expected: a new worktree exists at `.worktrees/inf-163-shell` and is checked out on `fix/inf-163-bottom-bar-role-navigation-develop`.

- [ ] **Step 2: Merge the latest `develop` into the target branch inside the worktree**

```bash
git -C ".worktrees/inf-163-shell" merge develop
```

Expected: merge completes cleanly or stops with explicit conflicts. If imports conflict, resolve them in favor of the model-package layout shown below:

```kotlin
import com.example.infinite_track.presentation.navigation.model.NavigationItem
import com.example.infinite_track.presentation.navigation.model.Screen
```

- [ ] **Step 3: Verify that only one `Screen` and one `NavigationItem` definition remain**

```bash
git -C ".worktrees/inf-163-shell" grep -n "sealed class Screen" -- "app/src/main/java/com/example/infinite_track/presentation/navigation"
git -C ".worktrees/inf-163-shell" grep -n "class NavigationItem" -- "app/src/main/java/com/example/infinite_track/presentation/navigation"
```

Expected: exactly one `Screen` definition and one `NavigationItem` definition, both under `presentation/navigation/model/`.

- [ ] **Step 4: Run the existing shell policy test as the post-merge baseline**

```bash
cd ".worktrees/inf-163-shell" && ./gradlew.bat testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicyTest"
```

Expected: `BUILD SUCCESSFUL`. If this fails before any new edits, stop and fix the merge damage before changing the contract.

### Task 2: Lock the remaining INF-163 contract gaps with failing tests

**Files:**
- Modify: `app/src/test/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicyTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicyTest.kt`

- [ ] **Step 1: Replace the old Management FAB expectation and expand the Profile selected-state test**

Update `MainShellNavigationPolicyTest.kt` so the Profile test covers all Profile child destinations that currently keep the bottom bar visible, and replace the old “preserve current repo behavior” FAB test with the explicit INF-163 rule.

```kotlin
@Test
fun `profile tab stays selected for profile child routes that keep the bottom bar visible`() {
    val profileItem = MainShellNavigationPolicy.bottomBarItemsForRole("Employee")[3]

    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.Profile.route))
    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.ProfileFlow.route))
    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.EditProfile.route))
    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.ContactUs.route))
    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.PaySlip.route))
    assertTrue(MainShellNavigationPolicy.isSelected(profileItem, Screen.MyDocument.route))
    assertFalse(MainShellNavigationPolicy.isSelected(profileItem, Screen.Home.route))
}

@Test
fun `fab mapping keeps only internship attendance action under INF-163`() {
    assertEquals(
        Screen.Attendance.route,
        MainShellNavigationPolicy.fabConfigForRole("Internship")?.destination?.route
    )
    assertNull(MainShellNavigationPolicy.fabConfigForRole("Management"))
    assertNull(MainShellNavigationPolicy.fabConfigForRole("Employee"))
    assertNull(MainShellNavigationPolicy.fabConfigForRole("Admin"))
}
```

- [ ] **Step 2: Run the shell policy test to prove the new contract fails on the old implementation**

```bash
cd ".worktrees/inf-163-shell" && ./gradlew.bat testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicyTest"
```

Expected: `FAIL` because `fabConfigForRole("Management")` still returns `Screen.TimeOffReq`, and `isSelected()` still returns `false` for at least one of `Screen.ContactUs`, `Screen.PaySlip`, or `Screen.MyDocument`.

- [ ] **Step 3: Implement the minimal policy change to satisfy the new contract**

Update `MainShellNavigationPolicy.kt` so the Profile selected-state set includes the visible Profile child routes and only Internship receives a FAB.

```kotlin
private val profileSelectedRoutes = setOf(
    Screen.Profile.route,
    Screen.ProfileFlow.route,
    Screen.EditProfile.route,
    Screen.ContactUs.route,
    Screen.PaySlip.route,
    Screen.MyDocument.route
)

fun fabConfigForRole(userRole: String): FabConfig? {
    return when (userRole) {
        "Internship" -> FabConfig(
            iconRes = R.drawable.ic_intern_fab,
            destination = Screen.Attendance
        )
        else -> null
    }
}
```

- [ ] **Step 4: Re-run the shell policy test to verify the contract now passes**

```bash
cd ".worktrees/inf-163-shell" && ./gradlew.bat testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicyTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the contract fix**

```bash
cd ".worktrees/inf-163-shell" && git add \
  app/src/main/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicy.kt \
  app/src/test/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicyTest.kt && \
  git commit -m "fix: finalize INF-163 shell navigation contract"
```

### Task 3: Correct the English shell labels at the resource source

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/MainBottomBar.kt`

- [ ] **Step 1: Confirm the UI is not formatting bottom-bar labels at runtime**

```bash
cd ".worktrees/inf-163-shell" && grep -n "formatBottomBarLabel" app/src/main/java/com/example/infinite_track/presentation/components/navigation/MainBottomBar.kt
```

Expected: no output. The bottom bar should rely on string resources directly.

- [ ] **Step 2: Fix the English bottom-bar strings in `values/strings.xml`**

Replace the existing lowercase English values with the contract labels.

```xml
<string name="bottom_menu_home">Home</string>
<string name="bottom_menu_history">History</string>
<string name="bottom_menu_contact">Contact</string>
<string name="bottom_menu_profile">Profile</string>
```

- [ ] **Step 3: Run resource processing to verify the resource edit compiles cleanly**

```bash
cd ".worktrees/inf-163-shell" && ./gradlew.bat :app:processDebugResources
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit the resource cleanup**

```bash
cd ".worktrees/inf-163-shell" && git add app/src/main/res/values/strings.xml && git commit -m "fix: normalize INF-163 bottom bar labels"
```

### Task 4: Run final verification and prepare the closure evidence note

**Files:**
- Verify: `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/MainBottomBar.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicy.kt`
- Verify: `app/src/test/java/com/example/infinite_track/presentation/navigation/main/MainShellNavigationPolicyTest.kt`

- [ ] **Step 1: Run the focused regression suite for the shell contract**

```bash
cd ".worktrees/inf-163-shell" && ./gradlew.bat testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicyTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Capture the implementation diff against `develop` for the closure note**

```bash
cd ".worktrees/inf-163-shell" && git diff --stat develop...HEAD
```

Expected: the diff is limited to INF-163 shell-navigation files plus the merge commit from syncing with `develop`.

- [ ] **Step 3: Prepare the final Linear evidence note using the exact template below**

```markdown
## INF-163 closure

### Fact
- Main shell bottom bar is driven from `MainShellNavigationPolicy`.
- Historical note: this INF-163 bottom-bar contract used `Home | History | Contact | Profile`, but it is superseded by INF-224. Current Android shell source of truth is `Home | History | WFA | Profile`.
- `MyLeave` is no longer a primary bottom-bar tab.
- `Internship` keeps the Attendance FAB.
- `Employee`, `Admin`, and `Management` now have no shell FAB.
- Profile child routes that keep the bottom bar visible keep the Profile tab selected.

### Verification
- `./gradlew.bat testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicyTest"`
- `./gradlew.bat :app:processDebugResources`

### Risk
- Manual emulator verification is still needed if product wants visual proof of the FAB disappearing for Management.

### Needs Verification
- If the branch merge from `develop` pulled in unrelated shell-adjacent edits, inspect the final diff before opening the PR.
```

- [ ] **Step 4: Report completion only after the verification commands have succeeded**

```bash
cd ".worktrees/inf-163-shell" && git status --short
```

Expected: no unexpected modified files beyond the intended INF-163 work.
