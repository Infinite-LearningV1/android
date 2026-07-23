package com.example.infinite_track.presentation.screen.attendance

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.ResolvedAddress
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.location.AddressResolver
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.WfaRepository
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateWorkModeEligibilityUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.attendance.ResolveSelectedTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingIdUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AttendanceScreenFaceResultRescueTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val FACE_RESULT_SINK_ROUTE = "face_result_sink"
        const val FAILED_MESSAGE = "Verifikasi wajah gagal. Silakan coba lagi."
        const val TIMEOUT_MESSAGE = "Waktu verifikasi wajah habis. Silakan coba lagi."
        const val SUCCESS_MESSAGE = "Check-in berhasil! Selamat bekerja hari ini."
        const val UNKNOWN_MESSAGE = "Hasil verifikasi wajah tidak dikenali. Silakan coba lagi."
        const val ATTENDANCE_ERROR_MESSAGE = "Absensi belum berhasil. Silakan coba lagi."
        const val SENTINEL_MESSAGE = "SECRET-SERVER-ID-9384"
    }

    @Before
    fun setUp() = clearAttendancePreferenceState()

    @After
    fun tearDown() = clearAttendancePreferenceState()

    @Test
    fun attendanceScreen_forwardsFailedFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.FAILED.savedStateValue)
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(FAILED_MESSAGE).assertCountEquals(1)
        composeRule.onNodeWithText(FAILED_MESSAGE).assertIsDisplayed()
        composeRule.runOnIdle {
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_forwardsTimeoutFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.TIMEOUT.savedStateValue)
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(TIMEOUT_MESSAGE).assertCountEquals(1)
        composeRule.onNodeWithText(TIMEOUT_MESSAGE).assertIsDisplayed()
        composeRule.runOnIdle {
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_forwardsSuccessFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.SUCCESS.savedStateValue)
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(SUCCESS_MESSAGE).assertCountEquals(1)
        composeRule.onNodeWithText(SUCCESS_MESSAGE).assertIsDisplayed()
        composeRule.runOnIdle {
            assertNotNull(viewModel.uiState.value.todayStatus?.checkedInAt)
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_ignoresCancelledFaceVerificationResultAfterClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.CANCELLED.savedStateValue)
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(FAILED_MESSAGE).assertDoesNotExist()
        composeRule.onNodeWithText(TIMEOUT_MESSAGE).assertDoesNotExist()
        composeRule.onNodeWithText(SUCCESS_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle {
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_surfacesUnknownFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, "unexpected_result")
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(UNKNOWN_MESSAGE).assertCountEquals(1)
        composeRule.onNodeWithText(UNKNOWN_MESSAGE).assertIsDisplayed()
        composeRule.runOnIdle {
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_doesNotReplayProcessedFaceVerificationResult() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.FAILED.savedStateValue)
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(FAILED_MESSAGE).assertIsDisplayed()
        composeRule.runOnIdle {
            navController.navigate(FACE_RESULT_SINK_ROUTE)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            navController.popBackStack()
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(FAILED_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle {
            check(navController.currentBackStackEntry?.destination?.route == Screen.Attendance.route)
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    @Test
    fun attendanceScreen_neverSurfacesRepositoryExceptionMessage() {
        val repository = FakeAttendanceRepository(
            checkInFailure = IllegalStateException(SENTINEL_MESSAGE)
        )
        val viewModel = createAttendanceViewModel(repository)
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, FaceVerificationResult.SUCCESS.savedStateValue)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.actionState is AttendanceActionState.RetryableFailure
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(ATTENDANCE_ERROR_MESSAGE).assertCountEquals(1)
        composeRule.onNodeWithText(ATTENDANCE_ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(SENTINEL_MESSAGE, substring = true).assertDoesNotExist()
        composeRule.runOnIdle {
            check(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(FACE_VERIFICATION_RESULT_KEY)
                    == null
            )
        }
    }

    private fun setAttendanceContent(viewModel: AttendanceViewModel): NavHostController {
        lateinit var navController: NavHostController

        composeRule.setContent {
            navController = rememberNavController()
            Infinite_TrackTheme {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Attendance.route
                ) {
                    composable(Screen.Attendance.route) {
                        AttendanceScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    composable(FACE_RESULT_SINK_ROUTE) { }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.actionState is AttendanceActionState.Ready
        }

        return navController
    }

    private fun clearAttendancePreferenceState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val attendancePreference = AttendancePreference(context)
        attendancePreference.clearActiveAttendanceId()
        attendancePreference.setUserInsideGeofence(false)
        attendancePreference.clearLastGeofenceParams()
        attendancePreference.clearReminderGeofences()
    }

    private fun createAttendanceViewModel(
        attendanceRepository: FakeAttendanceRepository = FakeAttendanceRepository()
    ): AttendanceViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val attendancePreference = AttendancePreference(context)
        val geofenceManager = GeofenceManager(context, attendancePreference)
        val addressResolver = FakeAddressResolver()
        val bookingRepository = FakeBookingRepository()
        val getCurrentLocationUseCase = GetCurrentLocationUseCase(FakeCurrentLocationRepository())

        return AttendanceViewModel(
            getTodayStatusUseCase = GetTodayStatusUseCase(attendanceRepository),
            getCurrentAddressUseCase = GetCurrentAddressUseCase(
                getCurrentLocationUseCase,
                addressResolver
            ),
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            getWfaRecommendationsUseCase = GetWfaRecommendationsUseCase(FakeWfaRepository()),
            reverseGeocodeUseCase = ReverseGeocodeUseCase(addressResolver),
            attendancePreference = attendancePreference,
            geofenceManager = geofenceManager,
            getLoggedInUserUseCase = GetLoggedInUserUseCase(FakeAuthRepository()),
            resolveTodayApprovedWfaBookingIdUseCase = ResolveTodayApprovedWfaBookingIdUseCase(
                bookingRepository
            ),
            resolveTodayApprovedWfaBookingUseCase = ResolveTodayApprovedWfaBookingUseCase(
                bookingRepository
            ),
            resolveSelectedTargetLocationUseCase = ResolveSelectedTargetLocationUseCase(),
            evaluateWorkModeEligibilityUseCase = EvaluateWorkModeEligibilityUseCase(
                ResolveTodayApprovedWfaBookingIdUseCase(bookingRepository)
            ),
            checkInUseCase = CheckInUseCase(
                attendanceRepository = attendanceRepository,
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                geofenceManager = geofenceManager,
                attendancePreference = attendancePreference
            ),
            checkOutUseCase = CheckOutUseCase(
                attendanceRepository = attendanceRepository,
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                geofenceManager = geofenceManager,
                attendancePreference = attendancePreference
            )
        )
    }

    private class FakeAttendanceRepository(
        private val checkInFailure: Throwable? = null
    ) : AttendanceRepository {
        private var checkedIn = false

        override suspend fun getTodayStatus(forceRefresh: Boolean): Result<TodayStatus> {
            return Result.success(
                TodayStatus(
                    canCheckIn = !checkedIn,
                    canCheckOut = checkedIn,
                    checkedInAt = if (checkedIn) "08:00:00" else null,
                    checkedOutAt = null,
                    activeMode = "WFO",
                    activeLocation = Location(
                        locationId = 1,
                        description = "Test office",
                        latitude = -6.2,
                        longitude = 106.8,
                        radius = 100,
                        category = "WFO"
                    ),
                    todayDate = "2026-04-30",
                    isHoliday = false,
                    holidayCheckinEnabled = false,
                    currentTime = "08:00:00",
                    checkinWindow = CheckinWindow(
                        startTime = "07:00:00",
                        endTime = "09:00:00"
                    ),
                    checkoutAutoTime = "17:00:00"
                )
            )
        }

        override suspend fun checkIn(
            request: AttendanceRequestModel
        ): Result<ActiveAttendanceSession> {
            checkInFailure?.let { return Result.failure(it) }
            checkedIn = true

            return Result.success(
                ActiveAttendanceSession(
                    idAttendance = 101,
                    userId = 1,
                    categoryId = request.categoryId,
                    statusId = 1,
                    timeIn = "08:00:00",
                    timeOut = null,
                    workHour = null,
                    attendanceDate = "2026-04-30",
                    notes = request.notes
                )
            )
        }

        override suspend fun checkOut(
            attendanceId: Int,
            latitude: Double,
            longitude: Double
        ): Result<ActiveAttendanceSession> {
            throw UnsupportedOperationException("False face verification must not check out")
        }

        override suspend fun getActiveAttendanceId(): Int? = null

        override suspend fun clearTodayStatusCache() = Unit

        override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> {
            throw UnsupportedOperationException("Location events are outside this regression")
        }
    }

    private class FakeBookingRepository : BookingRepository {
        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> = Result.success(BookingHistoryPage(emptyList()))

        override suspend fun submitBooking(
            scheduleDate: String,
            latitude: Double,
            longitude: Double,
            radius: Int,
            description: String,
            notes: String
        ): Result<Unit> = Result.success(Unit)
    }

    private class FakeAddressResolver : AddressResolver {
        override suspend fun resolve(coordinate: GeoCoordinate): AddressResolutionResult {
            return AddressResolutionResult.Resolved(
                ResolvedAddress(
                    coordinate = coordinate,
                    name = "Test location",
                    formattedAddress = "Test address"
                )
            )
        }
    }

    private class FakeCurrentLocationRepository : CurrentLocationRepository {
        override suspend fun getCurrentLocation(): CurrentLocationResult {
            return CurrentLocationResult.Success(
                CurrentLocation(
                    coordinate = GeoCoordinate(-6.2, 106.8),
                    accuracy = null,
                    capturedAtEpochMillis = 0L,
                    provider = "test",
                    isMock = true
                )
            )
        }
    }

    private class FakeWfaRepository : WfaRepository {
        override suspend fun getRecommendations(
            latitude: Double,
            longitude: Double
        ): Result<List<WfaRecommendation>> = Result.success(emptyList())
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            throw UnsupportedOperationException("Session refresh is outside this regression")
        }

        override suspend fun login(credentials: LoginCredentials): Result<UserModel> {
            throw UnsupportedOperationException("Login is outside this regression")
        }

        override suspend fun syncUserProfile(): ProfileSyncResult {
            throw UnsupportedOperationException("Profile sync is outside this regression")
        }

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(
            UserModel(
                id = 1,
                fullName = "Test User",
                email = "test@example.com",
                roleName = "Employee",
                positionName = "Engineer",
                programName = null,
                divisionName = null,
                nipNim = "123456789",
                phone = "08123456789",
                photoUrl = null,
                photoUpdatedAt = null,
                latitude = null,
                longitude = null,
                radius = null,
                locationDescription = null,
                locationCategoryName = null,
                faceEmbedding = null
            )
        )

        override suspend fun saveFaceEmbedding(
            userId: Int,
            embedding: ByteArray
        ): Result<Unit> = Result.success(Unit)
    }

    private class FakeUserDao : UserDao {
        private val userEntity = UserEntity(
            id = 1,
            fullName = "Test User",
            email = "test@example.com",
            roleName = "Employee",
            positionName = "Engineer",
            programName = null,
            divisionName = null,
            nipNim = "123456789",
            phone = "08123456789",
            photo = null,
            photoUpdatedAt = null,
            latitude = -6.2,
            longitude = 106.8,
            radius = 100,
            locationDescription = "Test office",
            locationCategoryName = "WFO",
            faceEmbedding = null
        )

        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit

        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(userEntity)

        override suspend fun getUserProfile(): UserEntity = userEntity

        override suspend fun clearUserProfile() = Unit
    }
}
