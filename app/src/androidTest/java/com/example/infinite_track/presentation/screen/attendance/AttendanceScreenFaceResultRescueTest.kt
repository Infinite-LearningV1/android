package com.example.infinite_track.presentation.screen.attendance

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.LocationRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.repository.WfaRepository
import com.example.infinite_track.domain.use_case.attendance.CheckInUseCase
import com.example.infinite_track.domain.use_case.attendance.CheckOutUseCase
import com.example.infinite_track.domain.use_case.attendance.GetTodayStatusUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentCoordinatesUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AttendanceScreenFaceResultRescueTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val FACE_RESULT_SINK_ROUTE = "face_result_sink"
        const val FACE_VERIFICATION_RESULT_KEY = "face_verification_result"
    }

    @Test
    fun attendanceScreen_forwardsFalseFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, false)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(
                DialogState.Error("Verifikasi wajah gagal. Silakan coba lagi."),
                viewModel.uiState.value.activeDialog
            )
            assertNull(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<Boolean>(FACE_VERIFICATION_RESULT_KEY)
            )
        }
    }

    @Test
    fun attendanceScreen_forwardsTrueFaceVerificationResultBeforeClearingIt() {
        val viewModel = createAttendanceViewModel()
        val navController = setAttendanceContent(viewModel)

        composeRule.runOnIdle {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(FACE_VERIFICATION_RESULT_KEY, true)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(
                DialogState.Success("Check-in berhasil! Selamat bekerja hari ini."),
                viewModel.uiState.value.activeDialog
            )
            assertNotNull(viewModel.uiState.value.todayStatus?.checkedInAt)
            assertNull(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<Boolean>(FACE_VERIFICATION_RESULT_KEY)
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
                ?.set(FACE_VERIFICATION_RESULT_KEY, false)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(
                DialogState.Error("Verifikasi wajah gagal. Silakan coba lagi."),
                viewModel.uiState.value.activeDialog
            )
            viewModel.onDialogDismissed()
            navController.navigate(FACE_RESULT_SINK_ROUTE)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            navController.popBackStack()
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(Screen.Attendance.route, navController.currentBackStackEntry?.destination?.route)
            assertNull(viewModel.uiState.value.activeDialog)
            assertNull(
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<Boolean>(FACE_VERIFICATION_RESULT_KEY)
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

        return navController
    }

    private fun createAttendanceViewModel(): AttendanceViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val attendancePreference = AttendancePreference(context)
        val geofenceManager = GeofenceManager(context, attendancePreference)
        val attendanceRepository = FakeAttendanceRepository()
        val locationRepository = FakeLocationRepository()
        val getCurrentCoordinatesUseCase = GetCurrentCoordinatesUseCase(
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
            userDao = FakeUserDao()
        )

        return AttendanceViewModel(
            getTodayStatusUseCase = GetTodayStatusUseCase(attendanceRepository),
            getCurrentAddressUseCase = GetCurrentAddressUseCase(locationRepository),
            getCurrentCoordinatesUseCase = getCurrentCoordinatesUseCase,
            getWfaRecommendationsUseCase = GetWfaRecommendationsUseCase(FakeWfaRepository()),
            reverseGeocodeUseCase = ReverseGeocodeUseCase(locationRepository),
            attendancePreference = attendancePreference,
            geofenceManager = geofenceManager,
            getLoggedInUserUseCase = GetLoggedInUserUseCase(FakeAuthRepository()),
            checkInUseCase = CheckInUseCase(
                attendanceRepository = attendanceRepository,
                getCurrentCoordinatesUseCase = getCurrentCoordinatesUseCase,
                geofenceManager = geofenceManager,
                attendancePreference = attendancePreference
            ),
            checkOutUseCase = CheckOutUseCase(
                attendanceRepository = attendanceRepository,
                getCurrentCoordinatesUseCase = getCurrentCoordinatesUseCase,
                geofenceManager = geofenceManager,
                attendancePreference = attendancePreference
            )
        )
    }

    private class FakeAttendanceRepository : AttendanceRepository {
        private var checkedIn = false

        override suspend fun getTodayStatus(): Result<TodayStatus> {
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

        override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> {
            throw UnsupportedOperationException("Location events are outside this regression")
        }
    }

    private class FakeLocationRepository : LocationRepository {
        override suspend fun getCurrentAddress(): Result<String> = Result.success("Test address")

        override suspend fun getCurrentCoordinates(): Result<Pair<Double, Double>> {
            return Result.success(-6.2 to 106.8)
        }

        override suspend fun searchLocation(
            query: String,
            userLatitude: Double?,
            userLongitude: Double?
        ): Result<List<LocationResult>> = Result.success(emptyList())

        override suspend fun reverseGeocode(
            latitude: Double,
            longitude: Double
        ): Result<LocationResult> {
            throw UnsupportedOperationException("Reverse geocoding is outside this regression")
        }
    }

    private class FakeWfaRepository : WfaRepository {
        override suspend fun getRecommendations(
            latitude: Double,
            longitude: Double
        ): Result<List<WfaRecommendation>> = Result.success(emptyList())
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun refreshSession(): RefreshSessionResult = RefreshSessionResult.Success

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            throw UnsupportedOperationException("Login is outside this regression")
        }

        override suspend fun syncUserProfile(): Result<UserModel> {
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
