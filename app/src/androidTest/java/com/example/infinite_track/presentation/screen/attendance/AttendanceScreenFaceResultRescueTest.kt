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
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.preferences.dataUserStore
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.ResolvedAddress
import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import com.example.infinite_track.domain.repository.location.AddressResolver
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.WfaRepository
import com.example.infinite_track.domain.use_case.attendance.SubmitAttendanceUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateAttendancePreparationUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateTargetRangeUseCase
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.RefreshAttendanceProfileUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayWfaBookingStateUseCase
import com.example.infinite_track.domain.use_case.geofence.BuildReminderGeofenceCandidatesUseCase
import com.example.infinite_track.domain.use_case.geofence.RefreshAndReconcileGeofenceRuntimeUseCase
import com.example.infinite_track.domain.use_case.geofence.ResolveGeofenceRuntimeModeUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentAddressUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.location.ReverseGeocodeUseCase
import com.example.infinite_track.domain.use_case.wfa.GetWfaRecommendationsUseCase
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test
    fun attendanceScreen_projectsResolvedTargetFromPreparation() {
        val viewModel = createAttendanceViewModel()

        setAttendanceContent(viewModel)

        composeRule.onNodeWithText("Test office").assertIsDisplayed()
    }

    @Test
    fun attendanceViewModel_keepsWfoSelectedWhenLateWfaResolutionCompletes() {
        val controllableResolver = ControllableFakeWfaBookingResolver()
        val viewModel = createAttendanceViewModel(
            wfaBookingResolver = controllableResolver
        )

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.preparation.isResolvedFor(WorkMode.WFO)
        }

        composeRule.runOnIdle {
            viewModel.onWorkModeSelected(WorkMode.WFA)
        }
        runBlocking { controllableResolver.awaitRequestStarted() }

        composeRule.runOnIdle {
            viewModel.onWorkModeSelected(WorkMode.WFO)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.preparation.isResolvedFor(WorkMode.WFO)
        }

        controllableResolver.completeApprovedWfa()
        runBlocking { controllableResolver.awaitRequestCompleted() }
        composeRule.waitForIdle()

        val preparation = viewModel.uiState.value.preparation
        assertEquals(WorkMode.WFO, preparation.selectedMode)
        assertTrue(preparation.isResolvedFor(WorkMode.WFO))
    }

    private fun AttendancePreparationState.isResolvedFor(mode: WorkMode): Boolean {
        val target = (targetResolution as? TargetLocationResolution.Resolved)?.target
        return selectedMode == mode && target?.mode == mode
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

    private fun createAttendanceViewModel(
        attendanceRepository: FakeAttendanceRepository = FakeAttendanceRepository(),
        wfaBookingResolver: BookingRepository = FakeBookingRepository()
    ): AttendanceViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val addressResolver = FakeAddressResolver()
        val bookingRepository = FakeBookingRepository()
        val getCurrentLocationUseCase = GetCurrentLocationUseCase(FakeCurrentLocationRepository())
        val authRepository = FakeAuthRepository()
        val targetResolver = ResolveAuthoritativeTargetLocationUseCase()
        val geofenceRuntimeRepository = FakeGeofenceRuntimeRepository()

        return AttendanceViewModel(
            getCurrentAddressUseCase = GetCurrentAddressUseCase(
                getCurrentLocationUseCase,
                addressResolver
            ),
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            getWfaRecommendationsUseCase = GetWfaRecommendationsUseCase(FakeWfaRepository()),
            reverseGeocodeUseCase = ReverseGeocodeUseCase(addressResolver),
            getLoggedInUserUseCase = GetLoggedInUserUseCase(authRepository),
            resolveTodayWfaBookingStateUseCase = ResolveTodayWfaBookingStateUseCase(
                wfaBookingResolver
            ),
            resolveAuthoritativeTargetLocationUseCase = targetResolver,
            evaluateTargetRangeUseCase = EvaluateTargetRangeUseCase(),
            evaluateAttendancePreparationUseCase = EvaluateAttendancePreparationUseCase(),
            refreshAndReconcileGeofenceRuntimeUseCase = RefreshAndReconcileGeofenceRuntimeUseCase(
                attendanceRepository = attendanceRepository,
                refreshProfile = RefreshAttendanceProfileUseCase(authRepository),
                getLoggedInUser = GetLoggedInUserUseCase(authRepository),
                resolveBooking = ResolveTodayWfaBookingStateUseCase(bookingRepository),
                validateSession = ValidateForegroundSessionUseCase(
                    authRepository = authRepository,
                    userPreference = UserPreference(context.dataUserStore),
                    sessionManager = com.example.infinite_track.domain.manager.SessionManager()
                ),
                resolveMode = ResolveGeofenceRuntimeModeUseCase(
                    buildCandidates = BuildReminderGeofenceCandidatesUseCase(targetResolver),
                    resolveTarget = targetResolver
                ),
                runtimeRepository = geofenceRuntimeRepository
            ),
            geofenceRuntimeUiMapper = GeofenceRuntimeUiMapper(),
            submitAttendanceUseCase = SubmitAttendanceUseCase(
                attendanceRepository = attendanceRepository,
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                getLoggedInUserUseCase = GetLoggedInUserUseCase(authRepository)
            )
        )
    }

    internal fun createDefaultAttendanceViewModelForCameraTest(): AttendanceViewModel =
        createAttendanceViewModel()

    internal fun createAttendanceViewModelForCameraTest(
        wfaBookingResolver: BookingRepository
    ): AttendanceViewModel = createAttendanceViewModel(
        wfaBookingResolver = wfaBookingResolver
    )

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
        ): AttendanceSubmitResult {
            checkInFailure?.let {
                return AttendanceSubmitResult.Failure(
                    AttendanceActionIntent.CHECK_IN,
                    AttendanceSubmitFailure.Unknown
                )
            }
            checkedIn = true

            return AttendanceSubmitResult.Success(
                AttendanceActionIntent.CHECK_IN,
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
        ): AttendanceSubmitResult {
            throw UnsupportedOperationException("False face verification must not check out")
        }

        override suspend fun getActiveAttendanceId(): Int? = null

        override suspend fun clearTodayStatusCache() = Unit

        override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> {
            throw UnsupportedOperationException("Location events are outside this regression")
        }
    }

    private class FakeBookingRepository : BookingRepository {
        override suspend fun getWfaRequestConfig() =
            com.example.infinite_track.domain.model.booking.WfaRequestConfigResult.Failure(
                com.example.infinite_track.domain.model.booking.WfaRequestFailure.ConfigUnavailable
            )

        override suspend fun submitWfaRequest(
            command: com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
        ) = com.example.infinite_track.domain.model.booking.WfaRequestResult.Failure(
            com.example.infinite_track.domain.model.booking.WfaRequestFailure.Unknown
        )

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> = Result.success(BookingHistoryPage(emptyList()))

    }

    internal class ControllableFakeWfaBookingResolver : BookingRepository {
        override suspend fun getWfaRequestConfig() =
            com.example.infinite_track.domain.model.booking.WfaRequestConfigResult.Failure(
                com.example.infinite_track.domain.model.booking.WfaRequestFailure.ConfigUnavailable
            )

        override suspend fun submitWfaRequest(
            command: com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
        ) = com.example.infinite_track.domain.model.booking.WfaRequestResult.Failure(
            com.example.infinite_track.domain.model.booking.WfaRequestFailure.Unknown
        )

        private val requestStarted = CompletableDeferred<Unit>()
        private val response = CompletableDeferred<Result<BookingHistoryPage>>()
        private val requestCompleted = CompletableDeferred<Unit>()

        suspend fun awaitRequestStarted() = requestStarted.await()

        suspend fun awaitRequestCompleted() = requestCompleted.await()

        fun completeApprovedWfa() {
            response.complete(
                Result.success(
                    BookingHistoryPage(
                        bookings = listOf(
                            BookingHistoryItem(
                                id = "88",
                                locationDescription = "Late WFA target",
                                scheduleDate = "2026-04-30",
                                scheduleDateRaw = "2026-04-30",
                                status = "Approved",
                                statusRaw = "approved",
                                statusKey = "approved",
                                notes = "",
                                suitabilityLabel = "Sesuai",
                                bookingId = 88,
                                latitude = -6.21,
                                longitude = 106.81,
                                radiusMeters = 100f
                            )
                        )
                    )
                )
            )
        }

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> = withContext(NonCancellable) {
            requestStarted.complete(Unit)
            response.await().also { requestCompleted.complete(Unit) }
        }

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
                    capturedAtEpochMillis = System.currentTimeMillis(),
                    provider = "test",
                    isMock = true
                )
            )
        }
    }

    private class FakeWfaRepository : WfaRepository {
        override suspend fun getRecommendations(
            query: WfaRecommendationQuery
        ): WfaRecommendationResult = WfaRecommendationResult.Success(
            scheduleDate = query.scheduleDate,
            timezone = "Asia/Jakarta",
            recommendations = emptyList(),
            meta = null
        )
    }

    private class FakeGeofenceRuntimeRepository : GeofenceRuntimeRepository {
        override suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult =
            GeofenceRuntimeResult.Applied(
                mode = mode,
                generation = 1,
                logicalIds = emptySet()
            )

        override suspend fun clearForLogout(): GeofenceRuntimeResult =
            GeofenceRuntimeResult.Applied(
                mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
                generation = 0,
                logicalIds = emptySet()
            )

        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = flowOf(
            GeofenceRuntimeReadiness(
                registration = RegistrationReadiness.Ready,
                notification = NotificationReadiness.READY
            )
        )
    }

    private class FakeAuthRepository : AuthRepository {
        private val user = UserModel(
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

        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            throw UnsupportedOperationException("Session refresh is outside this regression")
        }

        override suspend fun login(credentials: LoginCredentials): Result<UserModel> {
            throw UnsupportedOperationException("Login is outside this regression")
        }

        override suspend fun syncUserProfile(): ProfileSyncResult = ProfileSyncResult.Success(user)

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(user)

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
