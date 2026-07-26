package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitAttendanceUseCaseTest {

    private val gpsCoordinate = GeoCoordinate(-0.842239, 119.892637)

    private fun target(mode: WorkMode, wfa: ApprovedWfaTargetContext? = null) =
        AuthoritativeTargetLocation(
            targetId = TargetLocationId("t-1"),
            mode = mode,
            source = when (mode) {
                WorkMode.WFO -> TargetLocationSource.STATUS_TODAY
                WorkMode.WFH -> TargetLocationSource.ADMIN_PROFILE
                WorkMode.WFA -> TargetLocationSource.APPROVED_WFA_BOOKING
            },
            coordinate = GeoCoordinate(-0.89, 119.87),
            radius = DistanceMeters(100.0),
            displayName = "Target",
            approvedWfaContext = wfa
        )

    private fun useCase(
        repository: RecordingAttendanceRepository,
        locationResult: CurrentLocationResult = successLocation(),
        loggedInUser: UserModel? = userModel()
    ) = SubmitAttendanceUseCase(
        attendanceRepository = repository,
        getCurrentLocationUseCase = GetCurrentLocationUseCase(
            object : CurrentLocationRepository {
                override suspend fun getCurrentLocation(): CurrentLocationResult = locationResult
            }
        ),
        getLoggedInUserUseCase = GetLoggedInUserUseCase(FakeAuthRepository(loggedInUser))
    )

    private fun successLocation() = CurrentLocationResult.Success(
        CurrentLocation(
            coordinate = gpsCoordinate,
            accuracy = DistanceMeters(5.0),
            capturedAtEpochMillis = 0L,
            provider = "fused",
            isMock = false
        )
    )

    // ---- Check-in ----

    @Test
    fun `WFO check-in maps category 1 and invokes repository exactly once with fresh GPS`() = runTest {
        val repository = RecordingAttendanceRepository()
        val result = useCase(repository).invoke(
            AttendanceSubmitCommand.CheckIn(WorkMode.WFO, target(WorkMode.WFO))
        )

        assertTrue(result is AttendanceSubmitResult.Success)
        assertEquals(1, repository.checkInCalls.size)
        val request = repository.checkInCalls.single()
        assertEquals(1, request.categoryId)
        assertEquals(null, request.bookingId)
        assertEquals("checkin", request.type)
        assertEquals(gpsCoordinate.latitude, request.latitude, 0.000001)
        assertEquals(gpsCoordinate.longitude, request.longitude, 0.000001)
    }

    @Test
    fun `WFH check-in maps category 2`() = runTest {
        val repository = RecordingAttendanceRepository()
        useCase(repository).invoke(
            AttendanceSubmitCommand.CheckIn(WorkMode.WFH, target(WorkMode.WFH))
        )
        assertEquals(2, repository.checkInCalls.single().categoryId)
    }

    @Test
    fun `WFA check-in maps category 3 with approved booking id`() = runTest {
        val repository = RecordingAttendanceRepository()
        useCase(repository).invoke(
            AttendanceSubmitCommand.CheckIn(
                WorkMode.WFA,
                target(WorkMode.WFA, ApprovedWfaTargetContext(bookingId = 42, scheduleDate = "2026-07-27"))
            )
        )
        val request = repository.checkInCalls.single()
        assertEquals(3, request.categoryId)
        assertEquals(42, request.bookingId)
    }

    @Test
    fun `WFA without approved booking fails before repository call`() = runTest {
        val repository = RecordingAttendanceRepository()
        val result = useCase(repository).invoke(
            AttendanceSubmitCommand.CheckIn(WorkMode.WFA, target(WorkMode.WFA, wfa = null))
        )

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.WfaBookingRequired
            ),
            result
        )
        assertTrue(repository.checkInCalls.isEmpty())
    }

    @Test
    fun `target mode mismatch fails before repository call`() = runTest {
        val repository = RecordingAttendanceRepository()
        val result = useCase(repository).invoke(
            AttendanceSubmitCommand.CheckIn(WorkMode.WFO, target(WorkMode.WFH))
        )

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.TargetModeMismatch
            ),
            result
        )
        assertTrue(repository.checkInCalls.isEmpty())
    }

    @Test
    fun `fresh location failure returns CurrentLocationUnavailable`() = runTest {
        val repository = RecordingAttendanceRepository()
        val result = useCase(
            repository,
            locationResult = CurrentLocationResult.Failure.Unavailable
        ).invoke(AttendanceSubmitCommand.CheckIn(WorkMode.WFO, target(WorkMode.WFO)))

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.CurrentLocationUnavailable
            ),
            result
        )
        assertTrue(repository.checkInCalls.isEmpty())
    }

    @Test
    fun `missing session returns SessionUnavailable for both intents`() = runTest {
        val repository = RecordingAttendanceRepository()
        val useCase = useCase(repository, loggedInUser = null)

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.SessionUnavailable
            ),
            useCase.invoke(AttendanceSubmitCommand.CheckIn(WorkMode.WFO, target(WorkMode.WFO)))
        )
        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.SessionUnavailable
            ),
            useCase.invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = 7))
        )
        assertTrue(repository.checkInCalls.isEmpty())
        assertTrue(repository.checkOutCalls.isEmpty())
    }

    // ---- Checkout ----

    @Test
    fun `checkout uses caller attendance id first without status refresh`() = runTest {
        val repository = RecordingAttendanceRepository()
        useCase(repository).invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = 7))

        assertEquals(7, repository.checkOutCalls.single().first)
        assertEquals(0, repository.todayStatusCalls.size)
    }

    @Test
    fun `checkout falls back to force-refreshed status-today id`() = runTest {
        val repository = RecordingAttendanceRepository(
            todayStatusResult = Result.success(todayStatus(activeAttendanceId = 9))
        )
        useCase(repository).invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = null))

        assertEquals(9, repository.checkOutCalls.single().first)
        assertEquals(listOf(true), repository.todayStatusCalls)
    }

    @Test
    fun `checkout falls back to preference only after status lookup`() = runTest {
        val repository = RecordingAttendanceRepository(
            todayStatusResult = Result.success(todayStatus(activeAttendanceId = null)),
            preferenceAttendanceId = 11
        )
        useCase(repository).invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = null))

        assertEquals(11, repository.checkOutCalls.single().first)
        assertEquals(listOf(true), repository.todayStatusCalls)
    }

    @Test
    fun `missing checkout id returns ActiveAttendanceUnavailable`() = runTest {
        val repository = RecordingAttendanceRepository(
            todayStatusResult = Result.success(todayStatus(activeAttendanceId = null)),
            preferenceAttendanceId = null
        )
        val result = useCase(repository)
            .invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = null))

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.ActiveAttendanceUnavailable
            ),
            result
        )
        assertTrue(repository.checkOutCalls.isEmpty())
    }

    @Test
    fun `checkout invokes repository exactly once with fresh GPS`() = runTest {
        val repository = RecordingAttendanceRepository()
        val result = useCase(repository)
            .invoke(AttendanceSubmitCommand.CheckOut(activeAttendanceId = 7))

        assertTrue(result is AttendanceSubmitResult.Success)
        assertEquals(1, repository.checkOutCalls.size)
        val (_, latitude, longitude) = repository.checkOutCalls.single()
        assertEquals(gpsCoordinate.latitude, latitude, 0.000001)
        assertEquals(gpsCoordinate.longitude, longitude, 0.000001)
    }
}

private fun userModel() = UserModel(
    id = 42,
    fullName = "Test User",
    email = "user@example.com",
    roleName = "Employee",
    positionName = null,
    programName = null,
    divisionName = null,
    nipNim = "1234",
    phone = null,
    photoUrl = null,
    photoUpdatedAt = null,
    latitude = null,
    longitude = null,
    radius = null,
    locationDescription = null,
    locationCategoryName = null
)

private fun todayStatus(activeAttendanceId: Int?) = TodayStatus(
    canCheckIn = activeAttendanceId == null,
    canCheckOut = activeAttendanceId != null,
    checkedInAt = null,
    checkedOutAt = null,
    activeMode = "WFO",
    activeLocation = null,
    todayDate = "2026-07-27",
    isHoliday = false,
    holidayCheckinEnabled = false,
    currentTime = "2026-07-27T08:00:00+08:00",
    checkinWindow = CheckinWindow("07:00:00", "22:00:00"),
    checkoutAutoTime = "23:50:00",
    activeAttendanceId = activeAttendanceId
)

private fun activeSession() = ActiveAttendanceSession(
    idAttendance = 1,
    userId = 42,
    categoryId = 1,
    statusId = 1,
    timeIn = "08:00:00",
    timeOut = null,
    workHour = null,
    attendanceDate = "2026-07-27",
    notes = null
)

private class RecordingAttendanceRepository(
    private val todayStatusResult: Result<TodayStatus> =
        Result.failure(IllegalStateException("status not configured")),
    private val preferenceAttendanceId: Int? = null
) : AttendanceRepository {
    val checkInCalls = mutableListOf<AttendanceRequestModel>()
    val checkOutCalls = mutableListOf<Triple<Int, Double, Double>>()
    val todayStatusCalls = mutableListOf<Boolean>()

    override suspend fun getTodayStatus(forceRefresh: Boolean): Result<TodayStatus> {
        todayStatusCalls += forceRefresh
        return todayStatusResult
    }

    override suspend fun clearTodayStatusCache() = error("Not used")

    override suspend fun checkIn(request: AttendanceRequestModel): AttendanceSubmitResult {
        checkInCalls += request
        return AttendanceSubmitResult.Success(AttendanceActionIntent.CHECK_IN, activeSession())
    }

    override suspend fun checkOut(
        attendanceId: Int,
        latitude: Double,
        longitude: Double
    ): AttendanceSubmitResult {
        checkOutCalls += Triple(attendanceId, latitude, longitude)
        return AttendanceSubmitResult.Success(AttendanceActionIntent.CHECK_OUT, activeSession())
    }

    override suspend fun getActiveAttendanceId(): Int? = preferenceAttendanceId

    override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> =
        error("Not used")
}

private class FakeAuthRepository(private val user: UserModel?) : AuthRepository {
    override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
    override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
    override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used")

    @Deprecated("Use LogoutUseCase for user-initiated logout orchestration")
    override suspend fun logout(): Result<Unit> = error("Not used")

    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(user)

    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> =
        error("Not used")
}
