package com.example.infinite_track.data.repository.attendance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.CachedTodayStatusPayload
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.mapper.attendance.toDomain
import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.LogoutRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.response.AttendanceData
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceSessionStateDto
import com.example.infinite_track.data.soucre.network.response.CheckinWindow
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusData
import com.example.infinite_track.data.soucre.network.response.TodayStatusMeta
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.time.LocalDate

class AttendanceRepositoryImplSubmitTest {
    private lateinit var tempFiles: List<File>
    private lateinit var scope: CoroutineScope
    private lateinit var userDataStore: DataStore<Preferences>
    private lateinit var attendanceDataStore: DataStore<Preferences>
    private lateinit var todayStatusDataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        tempFiles = List(3) { index ->
            File.createTempFile("attendance-submit-$index", ".preferences_pb").also { it.delete() }
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        userDataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFiles[0] }
        attendanceDataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFiles[1] }
        todayStatusDataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFiles[2] }
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFiles.forEach { it.delete() }
    }

    @Test
    fun `successful check-in returns Success and stores active attendance id and clears cache`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkInBehavior = { successResponse(idAttendance = 777) }
        }
        val repository = createRepository(apiService)
        val todayStatusPreference = createTodayStatusPreference()
        todayStatusPreference.saveTodayStatusCache(cachedPayload())

        val result = repository.checkIn(checkInRequest())

        assertTrue(result is AttendanceSubmitResult.Success)
        result as AttendanceSubmitResult.Success
        assertEquals(AttendanceActionIntent.CHECK_IN, result.intent)
        assertEquals(777, result.session.idAttendance)
        assertEquals(777, createAttendancePreference().getActiveAttendanceId().first())
        assertNull(todayStatusPreference.getTodayStatusCache().first())
    }

    @Test
    fun `successful checkout returns Success and clears active attendance id and cache`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkOutBehavior = { successResponse(idAttendance = 777) }
        }
        val repository = createRepository(apiService)
        val attendancePreference = createAttendancePreference()
        attendancePreference.saveActiveAttendanceId(777)
        val todayStatusPreference = createTodayStatusPreference()
        todayStatusPreference.saveTodayStatusCache(cachedPayload())

        val result = repository.checkOut(attendanceId = 777, latitude = -0.84, longitude = 119.89)

        assertTrue(result is AttendanceSubmitResult.Success)
        result as AttendanceSubmitResult.Success
        assertEquals(AttendanceActionIntent.CHECK_OUT, result.intent)
        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertNull(todayStatusPreference.getTodayStatusCache().first())
    }

    @Test
    fun `duplicate check-in http failure maps to DuplicateAttendance and leaves preference untouched`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkInBehavior = { throw httpException(400, "Anda sudah melakukan check-in hari ini") }
        }
        val repository = createRepository(apiService)

        val result = repository.checkIn(checkInRequest())

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.DuplicateAttendance
            ),
            result
        )
        assertNull(createAttendancePreference().getActiveAttendanceId().first())
    }

    @Test
    fun `outside radius checkout failure maps to OutsideAllowedRadius and keeps active id`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkOutBehavior = {
                throw httpException(400, "Anda berada di luar radius lokasi yang diizinkan untuk check-out")
            }
        }
        val repository = createRepository(apiService)
        val attendancePreference = createAttendancePreference()
        attendancePreference.saveActiveAttendanceId(777)

        val result = repository.checkOut(attendanceId = 777, latitude = -0.84, longitude = 119.89)

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.OutsideAllowedRadius
            ),
            result
        )
        assertEquals(777, attendancePreference.getActiveAttendanceId().first())
    }

    @Test
    fun `already checked out failure maps to AlreadyCheckedOut`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkOutBehavior = { throw httpException(400, "Anda sudah melakukan check-out hari ini") }
        }
        val repository = createRepository(apiService)

        val result = repository.checkOut(attendanceId = 777, latitude = -0.84, longitude = 119.89)

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.AlreadyCheckedOut
            ),
            result
        )
    }

    @Test
    fun `server error maps to ServerUnavailable`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkInBehavior = { throw httpException(500, "internal detail") }
        }
        val repository = createRepository(apiService)

        val result = repository.checkIn(checkInRequest())

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.ServerUnavailable
            ),
            result
        )
    }

    @Test
    fun `io exception maps to NetworkUnavailable for both mutations`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkInBehavior = { throw IOException("no route") }
            checkOutBehavior = { throw IOException("no route") }
        }
        val repository = createRepository(apiService)

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.NetworkUnavailable
            ),
            repository.checkIn(checkInRequest())
        )
        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.NetworkUnavailable
            ),
            repository.checkOut(attendanceId = 777, latitude = -0.84, longitude = 119.89)
        )
    }

    @Test
    fun `non-exception backend rejection maps to BackendRejected with safe message`() = runTest {
        val apiService = SubmitFakeApiService().apply {
            checkInBehavior = {
                successResponse(idAttendance = 1).copy(
                    success = false,
                    message = "Absensi ditolak kebijakan kantor"
                )
            }
        }
        val repository = createRepository(apiService)

        val result = repository.checkIn(checkInRequest())

        assertEquals(
            AttendanceSubmitResult.Failure(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.BackendRejected("Absensi ditolak kebijakan kantor")
            ),
            result
        )
    }

    private suspend fun createRepository(apiService: ApiService): AttendanceRepositoryImpl {
        createUserPreference().saveSession(token = "token", userId = "42", refreshToken = "refresh")
        return AttendanceRepositoryImpl(
            apiService = apiService,
            attendancePreference = createAttendancePreference(),
            todayStatusPreference = createTodayStatusPreference(),
            userPreference = createUserPreference()
        )
    }

    private fun createUserPreference(): UserPreference = UserPreference(userDataStore)

    private fun createAttendancePreference(): AttendancePreference =
        AttendancePreference(attendanceDataStore)

    private fun createTodayStatusPreference(): TodayStatusPreference =
        TodayStatusPreference(dataStore = todayStatusDataStore, gson = Gson())

    private fun cachedPayload(): CachedTodayStatusPayload {
        val statusResponse = todayStatusResponse(activeAttendanceId = 123)
        return CachedTodayStatusPayload(
            userId = "42",
            todayDate = LocalDate.now().toString(),
            attendanceSessionStateId = 2,
            attendanceSessionStateKey = "active",
            activeAttendanceId = 123,
            fetchedAtMillis = System.currentTimeMillis(),
            ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
            status = statusResponse.toDomain()
        )
    }

    private fun checkInRequest() = AttendanceRequestModel(
        categoryId = 1,
        latitude = -0.89,
        longitude = 119.87,
        notes = "Check-in via mobile app",
        bookingId = null,
        type = "checkin"
    )
}

private fun httpException(code: Int, message: String): HttpException {
    val body = """{"success":false,"message":"$message"}"""
        .toResponseBody("application/json".toMediaType())
    return HttpException(Response.error<Any>(code, body))
}

private fun successResponse(idAttendance: Int): AttendanceResponse {
    return AttendanceResponse(
        success = true,
        message = "OK",
        data = AttendanceData(
            idAttendance = idAttendance,
            userId = 42,
            categoryId = 1,
            statusId = 1,
            locationId = 1,
            timeIn = "08:00:00",
            timeOut = null,
            workHour = "00:00:00",
            attendanceDate = "2026-07-27",
            notes = ""
        )
    )
}

private fun todayStatusResponse(activeAttendanceId: Int?): TodayStatusResponse {
    val today = LocalDate.now().toString()
    return TodayStatusResponse(
        success = true,
        data = TodayStatusData(
            canCheckIn = activeAttendanceId == null,
            canCheckOut = activeAttendanceId != null,
            checkedInAt = if (activeAttendanceId != null) "08:00:00" else null,
            checkedOutAt = null,
            activeMode = "Work From Office",
            activeLocation = null,
            todayDate = today,
            isHoliday = false,
            holidayCheckinEnabled = false,
            currentTime = "${today}T18:49:51.000Z",
            checkinWindow = CheckinWindow("07:00:00", "22:00:00"),
            checkoutAutoTime = "23:50:00",
            attendanceSessionState = AttendanceSessionStateDto(id = 2, key = "active", label = "Active"),
            activeAttendanceId = activeAttendanceId
        ),
        meta = TodayStatusMeta(cacheTtlSeconds = 300)
    )
}

private class SubmitFakeApiService : ApiService {
    var checkInBehavior: () -> AttendanceResponse = { error("checkIn not configured") }
    var checkOutBehavior: () -> AttendanceResponse = { error("checkOut not configured") }

    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse = checkInBehavior()

    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse =
        checkOutBehavior()

    override suspend fun getTodayStatus(): TodayStatusResponse =
        todayStatusResponse(activeAttendanceId = null)

    override suspend fun login(loginRequest: LoginRequest): LoginResponse = unsupported()

    override suspend fun getUserProfile(bootstrapAuthRequest: String?): LoginResponse = unsupported()

    override suspend fun logout(): LogoutResponse = unsupported()

    override suspend fun logoutWithRefresh(request: LogoutRequest): LogoutResponse = unsupported()

    override suspend fun refresh(request: RefreshRequest): RefreshResponse = unsupported()

    override suspend fun getAttendanceHistory(period: String, page: Int, limit: Int): AttendanceHistoryResponse =
        unsupported()

    override suspend fun previewAttendanceReportPdf(
        period: String,
        startDate: String?,
        endDate: String?,
        timezone: String?
    ): Response<ResponseBody> = unsupported()

    override suspend fun exportAttendanceReportPdf(
        period: String,
        startDate: String?,
        endDate: String?,
        timezone: String?
    ): Response<ResponseBody> = unsupported()

    override suspend fun updateUserProfile(userId: Int, request: ProfileUpdateRequest): ProfileUpdateResponse =
        unsupported()

    override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> = unsupported()

    override suspend fun getWfaRecommendations(latitude: Double, longitude: Double): WfaRecommendationResponse =
        unsupported()

    override suspend fun getWfaRequestConfig():
        com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto = unsupported()

    override suspend fun getBookingHistory(
        status: String?,
        page: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String
    ): BookingHistoryResponse = unsupported()

    override suspend fun submitWfaBooking(request: BookingRequest): BookingResponse = unsupported()

    private fun unsupported(): Nothing = error("Not supported in this test")
}
