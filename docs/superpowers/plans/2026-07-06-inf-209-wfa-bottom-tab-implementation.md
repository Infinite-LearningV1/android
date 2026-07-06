# INF-209 WFA Bottom Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finalize the Android shell so the primary bottom bar is `Home | History | WFA | Profile`, with `WFA` replacing `Contact` as a browse/history/detail tab while `AttendanceScreen` remains the only entry point for WFA request creation.

**Architecture:** The current worktree already contains most of the target structure: `Screen.Wfa`, `WfaShellNavigationContract`, `WfaHistoryScreen`, and shared booking-history content. This plan therefore treats INF-209 as a completion and cleanup task: verify the shell contract, tighten route ownership, preserve the existing reusable booking-history stack, and explicitly protect the attendance-owned `WfaBooking(latitude, longitude)` action flow.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Hilt, JUnit4, Gradle

## Global Constraints

- The bottom bar contract must remain exactly `Home | History | WFA | Profile`.
- `WFA` replaces `Contact` as the third primary tab.
- The `WFA` tab is browse/history/detail only.
- `AttendanceScreen` must remain the owner of WFA request creation.
- Preserve the `WfaBooking(latitude, longitude)` route argument contract.
- Reuse the existing `HomeViewModel` booking-history state and the existing WFA booking-history UI stack.
- Do not add a Notifications tab.
- Do not move WFA request creation into the bottom tab.
- If emulator/device runtime checks cannot run, final status must remain `Needs Verification`.

---

## File map

- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt` — keep the stable `Screen.Wfa` root route and preserve the `WfaBooking` argument route.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContract.kt` — canonical source of truth for tab order, hidden shell routes, and selected-state ownership.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt` — shell route ownership for `Home`, `History`, `Wfa`, `Profile`, and deeper detail/action screens.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/AppNavGraph.kt` — remove any duplicate ownership of nested app-shell routes if no longer needed at the root graph.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt` — shell composition, role-based bottom bar, FAB behavior, and bottom-bar visibility.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt` — role shell must consume `WfaShellNavigationContract` and preserve state-restoring navigation.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt` — same as staff shell.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaHistoryScreen.kt` — WFA browse root only.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaBookingHistoryContent.kt` — shared WFA history/detail body with empty/loading/error/retry/pagination behavior.
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/home/details/DetailsMyBooking.kt` — reuse the shared WFA booking-history content while remaining a detail screen.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt` — still owns navigation into the request flow.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt` — still emits `Screen.WfaBooking.createRoute(...)`.
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt` — request screen remains action-only.
- Verify/Modify: `app/src/main/res/values/strings.xml` — shell labels must stay aligned with `Home | History | WFA | Profile`.
- Verify/Modify: `app/src/main/res/values-in/string.xml` — Indonesian shell labels must stay aligned with `Home | History | WFA | Profile`.
- Verify/Create: `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt` — contract unit coverage for tab order, bottom-bar visibility, and selected-state.

## Existing reusable implementation to preserve

- `WfaShellNavigationContract.staffItems(): List<NavigationItem>`
- `WfaShellNavigationContract.internshipItems(): List<NavigationItem>`
- `WfaShellNavigationContract.shouldShowBottomBar(route: String?): Boolean`
- `WfaShellNavigationContract.isSelected(item: NavigationItem, currentRoute: String?): Boolean`
- `WfaHistoryScreen(...)`
- `WfaBookingHistoryContent(...)`
- `InitializeBookingHistoryEffect(...)`
- `BookingHistoryPaginationEffect(...)`
- `HomeViewModel.BookingHistoryDetailsState`
- `AttendanceViewModel.onBookingClicked(...)`
- `Screen.WfaBooking.createRoute(latitude: Double, longitude: Double): String`

### Task 1: Audit and lock the shell contract

**Files:**
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContract.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- Verify/Create: `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt`

**Interfaces:**
- Consumes:
  - `Screen.Home.route: String`
  - `Screen.History.route: String`
  - `Screen.Wfa.route: String`
  - `Screen.Profile.route: String`
  - `Screen.DetailsMyBooking.route: String`
  - `Screen.WfaBooking.route: String`
- Produces:
  - `object WfaShellNavigationContract`
  - `fun staffItems(): List<NavigationItem>`
  - `fun internshipItems(): List<NavigationItem>`
  - `fun shouldShowBottomBar(route: String?): Boolean`
  - `fun isSelected(item: NavigationItem, currentRoute: String?): Boolean`

- [ ] **Step 1: Inspect the existing shell contract and confirm the exact active tab order**

Read and verify that the current code still resolves to these routes in this exact order:

```kotlin
listOf(
    Screen.Home.route,
    Screen.History.route,
    Screen.Wfa.route,
    Screen.Profile.route
)
```

Check in:
- `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContract.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`

- [ ] **Step 2: Write or update the contract unit test so it locks the intended shell behavior**

Use this exact test body in `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt`:

```kotlin
package com.example.infinite_track.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaShellNavigationContractTest {

    @Test
    fun `staff items use Home History WFA Profile order`() {
        val routes = WfaShellNavigationContract.staffItems().map { it.screen.route }

        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.History.route,
                Screen.Wfa.route,
                Screen.Profile.route
            ),
            routes
        )
    }

    @Test
    fun `internship items use Home History WFA Profile order`() {
        val routes = WfaShellNavigationContract.internshipItems().map { it.screen.route }

        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.History.route,
                Screen.Wfa.route,
                Screen.Profile.route
            ),
            routes
        )
    }

    @Test
    fun `wfa tab stays selected for WFA browse routes only`() {
        val wfaItem = WfaShellNavigationContract.staffItems()[2]

        assertTrue(WfaShellNavigationContract.isSelected(wfaItem, Screen.Wfa.route))
        assertTrue(WfaShellNavigationContract.isSelected(wfaItem, Screen.DetailsMyBooking.route))
        assertFalse(WfaShellNavigationContract.isSelected(wfaItem, Screen.Attendance.route))
        assertFalse(WfaShellNavigationContract.isSelected(wfaItem, Screen.WfaBooking.route))
    }

    @Test
    fun `bottom bar stays hidden on action-only routes`() {
        assertFalse(WfaShellNavigationContract.shouldShowBottomBar(Screen.Attendance.route))
        assertFalse(WfaShellNavigationContract.shouldShowBottomBar(Screen.LocationSearch.route))
        assertFalse(WfaShellNavigationContract.shouldShowBottomBar(Screen.WfaBooking.route))
        assertTrue(WfaShellNavigationContract.shouldShowBottomBar(Screen.Wfa.route))
    }
}
```

- [ ] **Step 3: Run the focused contract test**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.WfaShellNavigationContractTest"
```

Expected: `BUILD SUCCESSFUL` and 4 passing tests.

- [ ] **Step 4: If the test fails, apply the minimal contract fix only**

The allowed minimal fix surface is:
- `staffItems()`
- `internshipItems()`
- `hiddenRoutes`
- `wfaSelectedRoutes`
- `isSelected(...)`

Keep the intended logic in this exact shape:

```kotlin
fun isSelected(item: NavigationItem, currentRoute: String?): Boolean {
    return when (item.screen) {
        Screen.History -> currentRoute == Screen.History.route ||
            currentRoute == Screen.HistoryFlow.route ||
            currentRoute == Screen.DetailMyAttendance.route

        Screen.Profile -> currentRoute == Screen.Profile.route ||
            currentRoute == Screen.ProfileFlow.route ||
            currentRoute == Screen.EditProfile.route ||
            currentRoute == Screen.Contact.route ||
            currentRoute == Screen.ContactUs.route ||
            currentRoute == Screen.PaySlip.route ||
            currentRoute == Screen.MyDocument.route

        Screen.Wfa -> currentRoute in setOf(
            Screen.Wfa.route,
            Screen.DetailsMyBooking.route
        )

        else -> currentRoute == item.screen.route
    }
}
```

- [ ] **Step 5: Commit the shell contract lock**

```bash
git add \
  app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt \
  app/src/main/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContract.kt \
  app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt

git commit -m "test: lock INF-209 WFA shell contract" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 2: Verify the WFA tab root remains browse-only and reuses the shared history stack

**Files:**
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaHistoryScreen.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaBookingHistoryContent.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/home/details/DetailsMyBooking.kt`

**Interfaces:**
- Consumes:
  - `HomeViewModel.BookingHistoryDetailsState`
  - `fun HomeViewModel.onBookingStatusFilterChanged(newStatus: String)`
  - `fun HomeViewModel.loadMoreBookings()`
  - `fun HomeViewModel.retryBookingHistoryLoad()`
  - `@Composable fun InitializeBookingHistoryEffect(...)`
  - `@Composable fun BookingHistoryPaginationEffect(...)`
- Produces:
  - `@Composable fun WfaHistoryScreen(viewModel: HomeViewModel = hiltViewModel())`
  - `@Composable fun WfaBookingHistoryContent(...)`
  - `@Composable fun DetailsMyBooking(viewModel: HomeViewModel, onBackClick: () -> Unit)` reusing the same shared WFA history body

- [ ] **Step 1: Inspect `WfaHistoryScreen.kt` and verify it exposes browse-only behavior**

Confirm the screen only:
- loads booking history,
- renders shared history content,
- supports filter/retry/pagination,
- does not navigate into request creation.

The expected root call shape is:

```kotlin
WfaBookingHistoryContent(
    uiState = uiState,
    listState = listState,
    onStatusSelected = viewModel::onBookingStatusFilterChanged,
    onRetryClick = viewModel::retryBookingHistoryLoad,
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
)
```

- [ ] **Step 2: Inspect `DetailsMyBooking.kt` and verify it reuses the shared WFA history content instead of duplicating list logic**

The expected wrapper shape is:

```kotlin
Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    topBar = {
        InfiniteTracButtonBack(
            title = "Riwayat Booking",
            navigationBack = onBackClick,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
) { paddingValues ->
    WfaBookingHistoryContent(
        uiState = uiState,
        listState = listState,
        onStatusSelected = viewModel::onBookingStatusFilterChanged,
        onRetryClick = viewModel::retryBookingHistoryLoad,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    )
}
```

- [ ] **Step 3: If either screen diverges, apply only the minimal reuse fix**

Keep these helpers shared between both screens:

```kotlin
InitializeBookingHistoryEffect(
    uiState = uiState,
    onLoadAllBookings = { viewModel.onBookingStatusFilterChanged("all") }
)
BookingHistoryPaginationEffect(
    uiState = uiState,
    listState = listState,
    onLoadMoreBookings = viewModel::loadMoreBookings
)
```

- [ ] **Step 4: Compile the WFA history stack**

Run:

```bash
./gradlew app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL` with no unresolved imports or compose/type errors.

- [ ] **Step 5: Commit the WFA history-stack cleanup if any file changed**

```bash
git add \
  app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaHistoryScreen.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/wfa/WfaBookingHistoryContent.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/home/details/DetailsMyBooking.kt

git commit -m "refactor: align WFA history screens with browse-only contract" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 3: Tighten route ownership between the root graph and the main shell graph

**Files:**
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/AppNavGraph.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Verify only: `app/src/main/java/com/example/infinite_track/presentation/main/InfiniteTrackApp.kt`

**Interfaces:**
- Consumes:
  - `fun NavGraphBuilder.appNavGraph(...)`
  - `fun NavGraphBuilder.mainContentNavGraph(...)`
  - `Screen.LocationSearch.route`
  - `Screen.WfaBooking.route`
  - `Screen.Attendance.route`
- Produces:
  - one authoritative owner for shell-internal action routes
  - preserved `Attendance -> WfaBooking(latitude, longitude)` behavior

- [ ] **Step 1: Inspect `AppNavGraph.kt` and `MainContentNavGraph.kt` for duplicate route registration**

Specifically check whether these appear in both files:

```kotlin
composable(Screen.LocationSearch.route) { ... }
composable(route = Screen.WfaBooking.route, arguments = listOf(...)) { ... }
```

- [ ] **Step 2: Choose the single owner for shell-internal attendance/WFA action routes**

Use this rule:
- if the route is only reachable from `MainScreen`/shell flow, it should live in `MainContentNavGraph`
- root graph should keep only true app-entry or cross-shell routes (`Splash`, auth graph, and any route that must exist outside the shell)

For INF-209, the recommended ownership is:

```text
MainContentNavGraph owns:
- Attendance
- LocationSearch
- WfaBooking
- FaceScanner
- DetailsMyBooking

AppNavGraph owns:
- Splash
- auth graph
```

- [ ] **Step 3: If duplication still exists, remove the duplicate registration from `AppNavGraph.kt` and keep the implementation in `MainContentNavGraph.kt`**

The root graph should remain in this shape:

```kotlin
fun NavGraphBuilder.appNavGraph(
    navController: NavHostController,
    splashViewModel: SplashViewModel
) {
    composable(Screen.Splash.route) {
        SplashScreen(
            navController = navController,
            splashViewModel = splashViewModel
        )
    }

    navigation(
        startDestination = Screen.Login.route,
        route = "auth_graph"
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                navigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 4: Compile after route-ownership cleanup**

Run:

```bash
./gradlew app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL` and no duplicate/ambiguous route errors.

- [ ] **Step 5: Commit the route-ownership cleanup if files changed**

```bash
git add \
  app/src/main/java/com/example/infinite_track/presentation/navigation/AppNavGraph.kt \
  app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt

git commit -m "refactor: centralize INF-209 shell route ownership" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 4: Verify shell composition, role bottom bars, and resource labels

**Files:**
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt`
- Verify/Modify: `app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt`
- Verify/Modify: `app/src/main/res/values/strings.xml`
- Verify/Modify: `app/src/main/res/values-in/string.xml`

**Interfaces:**
- Consumes:
  - `WfaShellNavigationContract.shouldShowBottomBar(route: String?): Boolean`
  - `WfaShellNavigationContract.staffItems(): List<NavigationItem>`
  - `WfaShellNavigationContract.internshipItems(): List<NavigationItem>`
  - `WfaShellNavigationContract.isSelected(item: NavigationItem, currentRoute: String?): Boolean`
- Produces:
  - shell rendering that keeps `Home | History | WFA | Profile` across role bottom bars
  - resource labels aligned with the contract

- [ ] **Step 1: Inspect `MainScreen.kt` and confirm bottom-bar visibility is delegated to `WfaShellNavigationContract`**

The expected visibility call is:

```kotlin
val isBottomBarVisible = WfaShellNavigationContract.shouldShowBottomBar(currentRoute)
```

- [ ] **Step 2: Inspect both bottom-bar composables and confirm they iterate over the shell contract items**

The expected item loop shape is:

```kotlin
WfaShellNavigationContract.staffItems().forEach { item ->
    val selected = WfaShellNavigationContract.isSelected(item, currentRoute)
    NavigationBarItem(
        selected = selected,
        onClick = {
            navController.navigate(item.screen.route) {
                if (item.screen.route == Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                } else {
                    popUpTo(Screen.Home.route) {
                        saveState = true
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }
        }
    )
}
```

Use the same pattern for internship via `internshipItems()`.

- [ ] **Step 3: Verify that shell label resources still match the approved contract**

Expected keys and values in `app/src/main/res/values/strings.xml`:

```xml
<string name="bottom_menu_home">Home</string>
<string name="bottom_menu_history">History</string>
<string name="bottom_menu_wfa">WFA</string>
<string name="bottom_menu_profile">Profile</string>
```

Expected keys and values in `app/src/main/res/values-in/string.xml`:

```xml
<string name="bottom_menu_home">Beranda</string>
<string name="bottom_menu_history">Riwayat</string>
<string name="bottom_menu_wfa">WFA</string>
<string name="bottom_menu_profile">Profil</string>
```

- [ ] **Step 4: If any shell file is out of alignment, make the smallest fix and run a compile pass**

Run:

```bash
./gradlew app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the shell-composition alignment if files changed**

```bash
git add \
  app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt \
  app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarStaff.kt \
  app/src/main/java/com/example/infinite_track/presentation/components/navigation/BottomBarInternship.kt \
  app/src/main/res/values/strings.xml \
  app/src/main/res/values-in/string.xml

git commit -m "feat: finalize INF-209 WFA bottom tab shell" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 5: Run final verification and prepare the handoff

**Files:**
- Verify: all files touched in Tasks 1–4

**Interfaces:**
- Consumes: the completed shell contract, WFA browse root, route-ownership cleanup, and attendance-owned request flow
- Produces: verification evidence and explicit `Needs Verification` note if runtime smoke cannot run

- [ ] **Step 1: Run the required static verification suite**

Run:

```bash
./gradlew app:assembleDebug
./gradlew app:test
./gradlew app:lint
```

Expected: all commands finish with `BUILD SUCCESSFUL`.

- [ ] **Step 2: Re-run the focused INF-209 contract test**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.WfaShellNavigationContractTest"
```

Expected: `BUILD SUCCESSFUL` and 4 passing tests.

- [ ] **Step 3: Capture the final diff summary**

Run:

```bash
git diff --stat origin/develop...HEAD
```

Expected: changes are limited to shell contract, shell graphs, WFA history reuse, string resources, tests, and spec/plan docs.

- [ ] **Step 4: If an emulator or device is available, run shell smoke verification**

Run:

```bash
adb devices -l
```

Then manually verify this exact flow:

```text
Splash -> Login/Main
Bottom tabs show Home | History | WFA | Profile
Tap WFA -> WFA history surface opens
Navigate to booking detail from WFA-owned browse flow -> WFA remains selected
Open Attendance -> WFA tab is not selected
Start WFA request from Attendance -> WfaBooking(latitude, longitude) opens
Profile child routes still work after tab changes
```

Expected: shell behavior matches the approved contract and request creation remains attendance-owned.

- [ ] **Step 5: If runtime smoke cannot run, record the exact gap**

Use this exact handoff note:

```markdown
## Needs Verification
- Emulator/device shell smoke was not run in this worktree, so WFA selected-state, route ownership, and attendance-owned request creation still require manual runtime confirmation.
```

- [ ] **Step 6: Check for unexpected local changes before handoff**

Run:

```bash
git status --short
```

Expected: only intended INF-209 files are modified or committed.

- [ ] **Step 7: Prepare the review summary in this exact structure**

```markdown
## Fact
- Bottom bar is `Home | History | WFA | Profile`.
- `WFA` replaces `Contact` as the third primary tab.
- `WFA` is browse/history/detail only.
- `AttendanceScreen` remains the owner of WFA request creation.

## Risk
- Route ownership was previously split between the root graph and the shell graph, so runtime verification must confirm no regression in deeper action routes.

## Needs Verification
- Emulator/device shell smoke for WFA selected-state and attendance-owned request flow if runtime checks were not executed.

## Verification
- `./gradlew app:assembleDebug`
- `./gradlew app:test`
- `./gradlew app:lint`
- `./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.WfaShellNavigationContractTest"`
```

## Self-review against spec

- Spec coverage: the plan covers the shell contract (`Home | History | WFA | Profile`), browse-only WFA ownership, protected attendance-owned request creation, route ownership cleanup, selected-state rules, and runtime/static verification.
- Placeholder scan: no `TBD`, `TODO`, or implicit “figure it out later” steps remain.
- Type consistency: all referenced interfaces and route/helper names match the current codebase (`Screen.Wfa`, `WfaShellNavigationContract`, `WfaHistoryScreen`, `WfaBookingHistoryContent`, `AttendanceViewModel.onBookingClicked`, `Screen.WfaBooking.createRoute`).
