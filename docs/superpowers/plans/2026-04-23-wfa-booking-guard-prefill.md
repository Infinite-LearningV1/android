# WFA Booking Guard & Prefill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the WFA booking screen duplicate-safe during in-flight submit and change booking prefill to a one-shot user snapshot instead of a long-lived profile stream.

**Architecture:** Keep the change local to the booking flow. Add a focused unit test suite for `WfaBookingViewModel`, then minimally change the ViewModel so user prefill is read once during init and repeated submit attempts are ignored while `isLoading` is true. Only touch booking UI files if needed to keep submit behavior aligned with the ViewModel guard.

**Tech Stack:** Kotlin, Android ViewModel, StateFlow, JUnit4, Gradle Kotlin DSL

---

## File map

### Production files
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`
  - Replace streaming user prefill with a bounded snapshot read
  - Add early-return submit guard using existing `isLoading`
- Review-only unless needed: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt`
  - Confirm submit callback path remains aligned with `isLoading`
- Review-only unless needed: `app/src/main/java/com/example/infinite_track/presentation/components/dialog/WfaBookingDialog.kt`
  - Only change if submit UI path can bypass loading alignment in a meaningful way

### Test and test-support files
- Modify: `app/build.gradle.kts`
  - Add the minimum local unit-test support dependencies needed to instantiate and test the ViewModel predictably
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt`
  - Add focused unit tests for snapshot prefill and duplicate-submit guard behavior

---

### Task 1: Add the minimum unit-test support for ViewModel tests

**Files:**
- Modify: `app/build.gradle.kts:180-188`

- [ ] **Step 1: Write the dependency change in `app/build.gradle.kts`**

Add the minimum local unit-test dependencies directly under the existing `testImplementation(libs.junit)` line:

```kotlin
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
```

Keep the rest of the test block unchanged.

- [ ] **Step 2: Verify Gradle can resolve the new test dependencies**

Run:

```bash
./gradlew :app:testDebugUnitTest --dry-run
```

Expected:
- Gradle prints the `:app:testDebugUnitTest` task graph
- No dependency-resolution error for `kotlinx-coroutines-test` or `core-testing`

- [ ] **Step 3: Commit the dependency setup**

Run:

```bash
git add app/build.gradle.kts
git commit -m "test: add booking viewmodel unit test support"
```

Expected:
- New commit created with only the Gradle test dependency change

---

### Task 2: Add failing tests for snapshot prefill and duplicate-submit guard

**Files:**
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/domain/model/auth/UserModel.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/domain/model/location/LocationResult.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/GetLoggedInUserUseCase.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/SubmitWfaBookingUseCase.kt`
- Read for reference: `app/src/main/java/com/example/infinite_track/domain/use_case/location/ReverseGeocodeUseCase.kt`

- [ ] **Step 1: Create the test file with fakes, dispatcher rule, and the first failing test**

Create `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt` with this content:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.booking

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.LocationRepository
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.SubmitWfaBookingUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class WfaBookingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_prefillsUserDataFromSingleSnapshot() = runTest {
        val authRepository = FakeAuthRepository(
            flow {
                emit(testUser(fullName = "Febri", divisionName = "Android"))
                emit(testUser(fullName = "Updated", divisionName = "Changed"))
            }
        )
        val bookingRepository = FakeBookingRepository()
        val locationRepository = FakeLocationRepository()

        val viewModel = buildViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
            locationRepository = locationRepository
        )

        runCurrent()

        assertEquals("Febri", viewModel.uiState.value.fullName)
        assertEquals("Android", viewModel.uiState.value.division)
    }

    @Test
    fun submit_ignoresRepeatedTapWhileRequestIsInFlight() = runTest {
        val authRepository = FakeAuthRepository(MutableStateFlow(testUser()))
        val bookingRepository = FakeBookingRepository(submitDelayMs = 1_000)
        val locationRepository = FakeLocationRepository()

        val viewModel = buildViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
            locationRepository = locationRepository
        )

        runCurrent()
        viewModel.onScheduleDateChanged("2026-04-23")
        viewModel.onDescriptionChanged("WFH dekat kantor")
        viewModel.onNotesChanged("Catatan")

        viewModel.onSubmitBooking()
        viewModel.onSubmitBooking()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, bookingRepository.submitCalls)
    }

    @Test
    fun submit_allowsRetryAfterFailureCompletes() = runTest {
        val authRepository = FakeAuthRepository(MutableStateFlow(testUser()))
        val bookingRepository = FakeBookingRepository(
            submitResults = mutableListOf(
                Result.failure(Exception("gagal pertama")),
                Result.success(Unit)
            )
        )
        val locationRepository = FakeLocationRepository()

        val viewModel = buildViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
            locationRepository = locationRepository
        )

        runCurrent()
        viewModel.onScheduleDateChanged("2026-04-23")
        viewModel.onDescriptionChanged("WFH dekat kantor")

        viewModel.onSubmitBooking()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, bookingRepository.submitCalls)

        viewModel.onSubmitBooking()
        runCurrent()

        assertEquals(2, bookingRepository.submitCalls)
    }

    private fun buildViewModel(
        authRepository: AuthRepository,
        bookingRepository: BookingRepository,
        locationRepository: LocationRepository
    ): WfaBookingViewModel {
        return WfaBookingViewModel(
            submitWfaBookingUseCase = SubmitWfaBookingUseCase(bookingRepository),
            getLoggedInUserUseCase = GetLoggedInUserUseCase(authRepository),
            reverseGeocodeUseCase = ReverseGeocodeUseCase(locationRepository),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "latitude" to 1.23f,
                    "longitude" to 4.56f
                )
            )
        )
    }

    private fun testUser(
        fullName: String = "Febriyadi",
        divisionName: String? = "Engineering"
    ) = UserModel(
        id = 1,
        fullName = fullName,
        email = "febri@example.com",
        roleName = "Staff",
        positionName = "Android Developer",
        programName = null,
        divisionName = divisionName,
        nipNim = "12345",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = null
    )

    private class FakeAuthRepository(
        private val userFlow: Flow<UserModel?>
    ) : AuthRepository {
        override suspend fun login(loginRequest: com.example.infinite_track.data.soucre.network.request.LoginRequest): Result<UserModel> {
            throw UnsupportedOperationException()
        }

        override suspend fun syncUserProfile(): Result<UserModel> {
            throw UnsupportedOperationException()
        }

        override suspend fun logout(): Result<Unit> {
            throw UnsupportedOperationException()
        }

        override fun getLoggedInUser(): Flow<UserModel?> = userFlow

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
            throw UnsupportedOperationException()
        }
    }

    private class FakeLocationRepository : LocationRepository {
        override suspend fun getCurrentAddress(): Result<String> {
            throw UnsupportedOperationException()
        }

        override suspend fun getCurrentCoordinates(): Result<Pair<Double, Double>> {
            throw UnsupportedOperationException()
        }

        override suspend fun searchLocation(
            query: String,
            userLatitude: Double?,
            userLongitude: Double?
        ): Result<List<LocationResult>> {
            throw UnsupportedOperationException()
        }

        override suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<LocationResult> {
            return Result.success(
                LocationResult(
                    placeName = "Test Place",
                    address = "Test Address",
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    private class FakeBookingRepository(
        private val submitDelayMs: Long = 0,
        private val submitResults: MutableList<Result<Unit>> = mutableListOf(Result.success(Unit))
    ) : BookingRepository {
        var submitCalls: Int = 0
            private set

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<com.example.infinite_track.domain.model.booking.BookingHistoryPage> {
            throw UnsupportedOperationException()
        }

        override suspend fun submitBooking(
            scheduleDate: String,
            latitude: Double,
            longitude: Double,
            radius: Int,
            description: String,
            notes: String
        ): Result<Unit> {
            submitCalls += 1
            if (submitDelayMs > 0) {
                delay(submitDelayMs)
            }
            return if (submitResults.isNotEmpty()) {
                submitResults.removeAt(0)
            } else {
                Result.success(Unit)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 2: Run the new test file and confirm the first two behaviors fail against current production code**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingViewModelTest"
```

Expected:
- `init_prefillsUserDataFromSingleSnapshot` fails because the current ViewModel keeps collecting and ends up with the later emitted user value
- `submit_ignoresRepeatedTapWhileRequestIsInFlight` fails because repeated submit increments `submitCalls` beyond 1
- `submit_allowsRetryAfterFailureCompletes` may already pass or fail depending on current state timing; do not change the test yet

- [ ] **Step 3: Commit the failing test suite**

Run:

```bash
git add app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt
git commit -m "test: cover booking prefill and submit guard"
```

Expected:
- New commit created with the test file only

---

### Task 3: Implement bounded prefill snapshot and duplicate-submit guard

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt:10-13`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt:58-73`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt:114-150`
- Review-only: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt`
- Review-only: `app/src/main/java/com/example/infinite_track/presentation/components/dialog/WfaBookingDialog.kt`

- [ ] **Step 1: Replace the long-lived `collect` import usage with one-shot Flow retrieval imports**

In `WfaBookingViewModel.kt`, replace the current Flow import block:

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

with:

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
```

- [ ] **Step 2: Rewrite `loadUserData()` to use a one-shot snapshot**

Replace the current `loadUserData()` implementation:

```kotlin
    private suspend fun loadUserData() {
        try {
            getLoggedInUserUseCase().collect { user ->
                user?.let {
                    _uiState.value = _uiState.value.copy(
                        fullName = it.fullName,
                        division = it.divisionName?: "",
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load user data: ${e.message}"
            )
        }
    }
```

with this bounded snapshot version:

```kotlin
    private suspend fun loadUserData() {
        try {
            val user = getLoggedInUserUseCase().firstOrNull()
            user?.let {
                _uiState.value = _uiState.value.copy(
                    fullName = it.fullName,
                    division = it.divisionName ?: ""
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load user data: ${e.message}"
            )
        }
    }
```

- [ ] **Step 3: Add an early-return guard at the top of `onSubmitBooking()`**

Replace the current method body start:

```kotlin
    fun onSubmitBooking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val currentState = _uiState.value
```

with:

```kotlin
    fun onSubmitBooking() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                _uiState.value = currentState.copy(isLoading = true, error = null)
```

This keeps the guard outside the coroutine launch and uses a single captured state snapshot before submit.

- [ ] **Step 4: Verify no other production change is needed in the booking UI path**

Read these files without editing unless necessary:
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/dialog/WfaBookingDialog.kt`

Acceptance for this step:
- Confirm the UI path still routes submit through `viewModel::onSubmitBooking`
- Do not add extra UI state unless a concrete bug is found that bypasses the ViewModel guard

- [ ] **Step 5: Run the targeted ViewModel test suite**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingViewModelTest"
```

Expected:
- `init_prefillsUserDataFromSingleSnapshot` passes
- `submit_ignoresRepeatedTapWhileRequestIsInFlight` passes
- `submit_allowsRetryAfterFailureCompletes` passes

- [ ] **Step 6: Run the broader local unit test task**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected:
- Existing local unit tests still pass
- No new failure outside the booking test suite

- [ ] **Step 7: Commit the production implementation**

Run:

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt
git commit -m "fix: harden wfa booking submit and prefill state"
```

If UI files changed after the review-only check, include them explicitly in `git add`:

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt \
        app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt \
        app/src/main/java/com/example/infinite_track/presentation/components/dialog/WfaBookingDialog.kt
```

Expected:
- New commit created containing only the WFA booking production fix

---

### Task 4: Final verification against the approved scope

**Files:**
- Review: `docs/superpowers/specs/2026-04-23-wfa-booking-guard-prefill-design.md`
- Review: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`
- Review: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt`

- [ ] **Step 1: Re-check the final code against the spec boundaries**

Verify these conditions by reading the final code:
- prefill is snapshot-only, not stream-based
- duplicate submit is ignored while loading
- retry is still possible after a completed failure
- no accidental changes were made to `booking_id` behavior
- no accidental changes were made to downstream refresh behavior after success

- [ ] **Step 2: Run the booking-focused test command one more time**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingViewModelTest"
```

Expected:
- PASS for the full WFA booking ViewModel test class

- [ ] **Step 3: Commit any final plan-alignment fix if needed**

If no additional code changes were needed after verification, do not create an extra commit.

If a tiny alignment fix was needed, run:

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt \
        app/src/test/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModelTest.kt
git commit -m "test: align booking guard implementation with spec"
```

Expected:
- Either no commit because everything already aligns, or one tiny cleanup commit
