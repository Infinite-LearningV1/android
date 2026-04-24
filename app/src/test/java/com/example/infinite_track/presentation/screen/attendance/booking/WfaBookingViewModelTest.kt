package com.example.infinite_track.presentation.screen.attendance.booking

import androidx.lifecycle.SavedStateHandle
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
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
import kotlinx.coroutines.test.advanceUntilIdle
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
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, bookingRepository.submitCalls)

        viewModel.onSubmitBooking()
        advanceUntilIdle()

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
        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
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
        ): Result<BookingHistoryPage> {
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
