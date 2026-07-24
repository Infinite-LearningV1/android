package com.example.infinite_track.domain.use_case.geofence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSessionState
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.geofence.BackendTruthSource
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceReconcileReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.RefreshAttendanceProfileUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import com.example.infinite_track.domain.use_case.booking.ResolveTodayWfaBookingStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshAndReconcileGeofenceRuntimeUseCaseTest {

    @Test
    fun `foreground refresh obtains status profile booking then reconciles`() = runTest {
        val dependencies = dependencies(status = reminderEligibleStatus())

        val result = dependencies.coordinator(GeofenceReconcileReason.FOREGROUND_REFRESH)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.Reconciled)
        result as RefreshAndReconcileGeofenceRuntimeResult.Reconciled
        assertEquals(WfaBookingForDate.NotRequested, result.wfaBooking)
        assertTrue(result.resolution.mode is GeofenceRuntimeMode.Reminder)
        assertEquals(
            listOf("status:true", "profile", "booking:2026-07-25", "readiness", "reconcile"),
            dependencies.events
        )
        assertEquals(1, dependencies.runtime.reconciledModes.size)
    }

    @Test
    fun `check in success reason reaches mode resolver`() = runTest {
        val dependencies = dependencies(status = activeWfoStatus())

        val result = dependencies.coordinator(GeofenceReconcileReason.CHECK_IN_SUCCEEDED)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.Reconciled)
        val mode = (result as RefreshAndReconcileGeofenceRuntimeResult.Reconciled).resolution.mode
        assertTrue(mode is GeofenceRuntimeMode.ActiveMonitoring)
        assertEquals(41, (mode as GeofenceRuntimeMode.ActiveMonitoring).attendanceId)
        assertEquals(1, dependencies.runtime.reconciledModes.size)
    }

    @Test
    fun `check out success reason resolves completed without reminders`() = runTest {
        val dependencies = dependencies(status = postCheckoutStatus())

        val result = dependencies.coordinator(GeofenceReconcileReason.CHECK_OUT_SUCCEEDED)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.Reconciled)
        val mode = (result as RefreshAndReconcileGeofenceRuntimeResult.Reconciled).resolution.mode
        assertTrue(mode is GeofenceRuntimeMode.Completed)
        assertFalse(mode is GeofenceRuntimeMode.Reminder)
        assertEquals(1, dependencies.runtime.reconciledModes.size)
    }

    @Test
    fun `status failure returns backend unavailable and does not reconcile`() = runTest {
        val dependencies = dependencies(statusResult = Result.failure(IllegalStateException("offline")))

        val result = dependencies.coordinator(GeofenceReconcileReason.FOREGROUND_REFRESH)

        assertEquals(
            RefreshAndReconcileGeofenceRuntimeResult.BackendUnavailable(
                GeofenceRuntimeFailure.BackendTruthUnavailable(BackendTruthSource.STATUS_TODAY)
            ),
            result
        )
        assertEquals(listOf("status:true"), dependencies.events)
        assertTrue(dependencies.runtime.reconciledModes.isEmpty())
    }

    @Test
    fun `boot temporary session failure returns retryable failure`() = runTest {
        val dependencies = dependencies(
            validation = ForegroundSessionValidationResult.TemporaryFailure("offline", null)
        )

        val result = dependencies.coordinator(GeofenceReconcileReason.BOOT_RECOVERY)

        assertEquals(
            RefreshAndReconcileGeofenceRuntimeResult.BackendUnavailable(
                GeofenceRuntimeFailure.BackendTruthUnavailable(BackendTruthSource.SESSION_VALIDATION)
            ),
            result
        )
        assertEquals(1, dependencies.validation.calls)
        assertTrue(dependencies.events.isEmpty())
        assertEquals(0, dependencies.runtime.clearCalls)
        assertTrue(dependencies.runtime.reconciledModes.isEmpty())
    }

    @Test
    fun `boot reauth clears runtime and returns auth unavailable`() = runTest {
        val dependencies = dependencies(
            validation = ForegroundSessionValidationResult.ReauthRequired(ReauthReason.REFRESH_REVOKED)
        )

        val result = dependencies.coordinator(GeofenceReconcileReason.BOOT_RECOVERY)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable)
        assertEquals(1, dependencies.validation.calls)
        assertEquals(1, dependencies.runtime.clearCalls)
        assertTrue(dependencies.events.isEmpty())
        assertTrue(dependencies.runtime.reconciledModes.isEmpty())
    }

    @Test
    fun `profile temporary failure uses cached profile and records warning`() = runTest {
        val cachedUser = sampleUser(id = 22)
        val dependencies = dependencies(
            status = activeWfhStatus(),
            profile = ProfileSyncResult.TemporaryFailure(message = "offline"),
            cachedUser = cachedUser
        )

        val result = dependencies.coordinator(GeofenceReconcileReason.FOREGROUND_REFRESH)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.Reconciled)
        result as RefreshAndReconcileGeofenceRuntimeResult.Reconciled
        assertSame(cachedUser, result.profile)
        assertEquals(
            listOf(GeofenceRuntimeFailure.BackendTruthUnavailable(BackendTruthSource.PROFILE)),
            result.resolution.warnings
        )
        assertTrue(result.resolution.mode is GeofenceRuntimeMode.ActiveMonitoring)
        assertEquals(1, dependencies.auth.cachedUserReads)
        assertEquals(1, dependencies.runtime.reconciledModes.size)
    }

    @Test
    fun `profile unauthorized clears runtime without reading cached profile`() = runTest {
        val dependencies = dependencies(
            profile = ProfileSyncResult.Unauthorized(),
            cachedUser = sampleUser(id = 33)
        )

        val result = dependencies.coordinator(GeofenceReconcileReason.FOREGROUND_REFRESH)

        assertTrue(result is RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable)
        assertEquals(0, dependencies.auth.cachedUserReads)
        assertEquals(1, dependencies.runtime.clearCalls)
        assertEquals(listOf("status:true", "profile"), dependencies.events)
        assertTrue(dependencies.runtime.reconciledModes.isEmpty())
    }

    private fun dependencies(
        status: TodayStatus = reminderEligibleStatus(),
        statusResult: Result<TodayStatus> = Result.success(status),
        profile: ProfileSyncResult = ProfileSyncResult.Success(sampleUser()),
        cachedUser: UserModel? = null,
        validation: ForegroundSessionValidationResult = ForegroundSessionValidationResult.Valid
    ): Dependencies {
        val events = mutableListOf<String>()
        val attendance = FakeAttendanceRepository(statusResult, events)
        val auth = FakeAuthRepository(profile, cachedUser, events)
        val booking = FakeBookingRepository(events)
        val runtime = FakeGeofenceRuntimeRepository(events)
        val session = FakeValidateForegroundSessionUseCase(validation)
        val targetResolver = ResolveAuthoritativeTargetLocationUseCase()
        val modeResolver = ResolveGeofenceRuntimeModeUseCase(
            buildCandidates = BuildReminderGeofenceCandidatesUseCase(targetResolver),
            resolveTarget = targetResolver
        )

        return Dependencies(
            coordinator = RefreshAndReconcileGeofenceRuntimeUseCase(
                attendanceRepository = attendance,
                refreshProfile = RefreshAttendanceProfileUseCase(auth),
                getLoggedInUser = GetLoggedInUserUseCase(auth),
                resolveBooking = ResolveTodayWfaBookingStateUseCase(booking),
                validateSession = session,
                resolveMode = modeResolver,
                runtimeRepository = runtime
            ),
            events = events,
            auth = auth,
            runtime = runtime,
            validation = session
        )
    }

    private data class Dependencies(
        val coordinator: RefreshAndReconcileGeofenceRuntimeUseCase,
        val events: List<String>,
        val auth: FakeAuthRepository,
        val runtime: FakeGeofenceRuntimeRepository,
        val validation: FakeValidateForegroundSessionUseCase
    )

    private fun reminderEligibleStatus() = status(
        canCheckIn = true,
        activeMode = "",
        attendanceSessionState = AttendanceSessionState(id = 1, key = "not_started", label = "Not started")
    )

    private fun activeWfoStatus() = status(
        canCheckIn = false,
        canCheckOut = true,
        activeMode = "WFO",
        activeAttendanceId = 41,
        attendanceSessionState = AttendanceSessionState(id = 2, key = "active", label = "Active")
    )

    private fun activeWfhStatus() = status(
        canCheckIn = false,
        canCheckOut = true,
        activeMode = "WFH",
        activeAttendanceId = 43,
        attendanceSessionState = AttendanceSessionState(id = 2, key = "active", label = "Active")
    )

    private fun postCheckoutStatus() = status(
        canCheckIn = false,
        canCheckOut = false,
        activeMode = "",
        activeAttendanceId = null,
        attendanceSessionState = null
    )

    private fun status(
        canCheckIn: Boolean,
        canCheckOut: Boolean = false,
        activeMode: String,
        activeAttendanceId: Int? = null,
        attendanceSessionState: AttendanceSessionState?
    ) = TodayStatus(
        canCheckIn = canCheckIn,
        canCheckOut = canCheckOut,
        checkedInAt = null,
        checkedOutAt = null,
        activeMode = activeMode,
        activeLocation = Location(
            locationId = 7,
            description = "Office",
            coordinate = GeoCoordinate(-0.89, 119.87),
            radius = 100,
            category = "Office"
        ),
        todayDate = "2026-07-25",
        isHoliday = false,
        holidayCheckinEnabled = false,
        currentTime = "09:00:00",
        checkinWindow = CheckinWindow(startTime = "08:00:00", endTime = "10:00:00"),
        checkoutAutoTime = "17:00:00",
        attendanceSessionState = attendanceSessionState,
        activeAttendanceId = activeAttendanceId
    )

    private fun sampleUser(id: Int = 21) = UserModel(
        id = id,
        fullName = "Test User",
        email = "test.user@example.test",
        roleName = "Employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "test-$id",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = -0.90,
        longitude = 119.86,
        radius = 100,
        locationDescription = "Home",
        locationCategoryName = "WFH"
    )

    private class FakeAttendanceRepository(
        private val todayStatus: Result<TodayStatus>,
        private val events: MutableList<String>
    ) : AttendanceRepository {
        override suspend fun getTodayStatus(forceRefresh: Boolean): Result<TodayStatus> {
            events += "status:$forceRefresh"
            return todayStatus
        }

        override suspend fun clearTodayStatusCache() = error("Not used")
        override suspend fun checkIn(request: AttendanceRequestModel): Result<ActiveAttendanceSession> = error("Not used")
        override suspend fun checkOut(
            attendanceId: Int,
            latitude: Double,
            longitude: Double
        ): Result<ActiveAttendanceSession> = error("Not used")

        override suspend fun getActiveAttendanceId(): Int? = error("Not used")
        override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> = error("Not used")
    }

    private class FakeAuthRepository(
        private val profileResult: ProfileSyncResult,
        private val cachedUser: UserModel?,
        private val events: MutableList<String>
    ) : AuthRepository {
        var cachedUserReads = 0

        override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
        override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")

        override suspend fun syncUserProfile(): ProfileSyncResult {
            events += "profile"
            return profileResult
        }

        @Suppress("DEPRECATION")
        override suspend fun logout(): Result<Unit> = error("Not used")

        override fun getLoggedInUser(): Flow<UserModel?> {
            cachedUserReads += 1
            return flowOf(cachedUser)
        }

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used")
    }

    private class FakeBookingRepository(
        private val events: MutableList<String>
    ) : BookingRepository {
        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> {
            events += "booking:2026-07-25"
            return Result.success(BookingHistoryPage(emptyList()))
        }

        override suspend fun submitBooking(
            scheduleDate: String,
            latitude: Double,
            longitude: Double,
            radius: Int,
            description: String,
            notes: String
        ): Result<Unit> = error("Not used")
    }

    private class FakeGeofenceRuntimeRepository(
        private val events: MutableList<String>
    ) : GeofenceRuntimeRepository {
        val reconciledModes = mutableListOf<GeofenceRuntimeMode>()
        var clearCalls = 0

        override suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult {
            events += "reconcile"
            reconciledModes += mode
            return GeofenceRuntimeResult.Applied(mode, generation = 1, logicalIds = emptySet())
        }

        override suspend fun clearForLogout(): GeofenceRuntimeResult {
            clearCalls += 1
            return GeofenceRuntimeResult.Applied(
                mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
                generation = 1,
                logicalIds = emptySet()
            )
        }

        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> {
            events += "readiness"
            return flowOf(
                GeofenceRuntimeReadiness(
                    registration = RegistrationReadiness.Ready,
                    notification = NotificationReadiness.READY
                )
            )
        }
    }

    private class FakeValidateForegroundSessionUseCase(
        private val result: ForegroundSessionValidationResult
    ) : ValidateForegroundSessionUseCase(
        authRepository = object : AuthRepository {
            override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
            override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
            override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used")
            @Suppress("DEPRECATION")
            override suspend fun logout(): Result<Unit> = error("Not used")
            override fun getLoggedInUser(): Flow<UserModel?> = emptyFlow()
            override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used")
        },
        userPreference = UserPreference(UnusedDataStore),
        sessionManager = SessionManager()
    ) {
        var calls = 0

        override suspend fun invoke(): ForegroundSessionValidationResult {
            calls += 1
            return result
        }
    }

    private object UnusedDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = emptyFlow()

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = error("Not used")
    }
}
