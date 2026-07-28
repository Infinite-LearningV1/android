package com.example.infinite_track.data.repository.attendance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.mapper.attendance.toDomain
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.CachedTodayStatusPayload
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
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
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.time.LocalDate

class AttendanceRepositoryImplTodayStatusCacheTest {
    private lateinit var tempFiles: List<File>
    private lateinit var scope: CoroutineScope
    private lateinit var userDataStore: DataStore<Preferences>
    private lateinit var attendanceDataStore: DataStore<Preferences>
    private lateinit var todayStatusDataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        tempFiles = List(3) { index ->
            File.createTempFile("attendance-repository-$index", ".preferences_pb").also { it.delete() }
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
    fun `returns cached today status within ttl and bypasses cache when force refresh is true`() = runTest {
        val apiService = FakeApiService(
            todayStatusResponses = ArrayDeque(
                listOf(
                    createTodayStatusResponse(activeAttendanceId = 456, stateKey = "active")
                )
            )
        )
        val repository = createRepository(apiService)
        val todayStatusPreference = createTodayStatusPreference()
        todayStatusPreference.saveTodayStatusCache(
            CachedTodayStatusPayload(
                userId = "42",
                todayDate = LocalDate.now().toString(),
                attendanceSessionStateId = 2,
                attendanceSessionStateKey = "active",
                activeAttendanceId = 123,
                fetchedAtMillis = System.currentTimeMillis(),
                ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
                status = createTodayStatusResponse(activeAttendanceId = 123, stateKey = "active").data.toDomainForTest()
            )
        )

        val cached = repository.getTodayStatus(forceRefresh = false)
        val refreshed = repository.getTodayStatus(forceRefresh = true)

        assertTrue(cached.isSuccess)
        assertTrue(refreshed.isSuccess)
        assertEquals(123, cached.getOrThrow().activeAttendanceId)
        assertEquals(456, refreshed.getOrThrow().activeAttendanceId)
        assertEquals(1, apiService.todayStatusCalls)
    }

    @Test
    fun `refreshes when cached status belongs to another user`() = runTest {
        val apiService = FakeApiService(
            todayStatusResponses = ArrayDeque(listOf(createTodayStatusResponse(activeAttendanceId = 456, stateKey = "active")))
        )
        val repository = createRepository(apiService)
        createTodayStatusPreference().saveTodayStatusCache(
            CachedTodayStatusPayload(
                userId = "different-user",
                todayDate = LocalDate.now().toString(),
                attendanceSessionStateId = 2,
                attendanceSessionStateKey = "active",
                activeAttendanceId = 123,
                fetchedAtMillis = System.currentTimeMillis(),
                ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
                status = createTodayStatusResponse(activeAttendanceId = 123, stateKey = "active").data.toDomainForTest()
            )
        )

        val refreshed = repository.getTodayStatus()

        assertTrue(refreshed.isSuccess)
        assertEquals(456, refreshed.getOrThrow().activeAttendanceId)
        assertEquals(1, apiService.todayStatusCalls)
    }

    @Test
    fun `refreshes when cached today date is stale even if ttl is valid`() = runTest {
        val apiService = FakeApiService(
            todayStatusResponses = ArrayDeque(listOf(createTodayStatusResponse(activeAttendanceId = 456, stateKey = "active")))
        )
        val repository = createRepository(apiService)
        createTodayStatusPreference().saveTodayStatusCache(
            CachedTodayStatusPayload(
                userId = "42",
                todayDate = LocalDate.now().minusDays(1).toString(),
                attendanceSessionStateId = 2,
                attendanceSessionStateKey = "active",
                activeAttendanceId = 123,
                fetchedAtMillis = System.currentTimeMillis(),
                ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
                status = createTodayStatusResponse(activeAttendanceId = 123, stateKey = "active").data.toDomainForTest()
            )
        )

        val refreshed = repository.getTodayStatus()

        assertTrue(refreshed.isSuccess)
        assertEquals(456, refreshed.getOrThrow().activeAttendanceId)
        assertEquals(1, apiService.todayStatusCalls)
    }

    @Test
    fun `syncs active attendance id from refreshed status and only clears for terminal empty states`() = runTest {
        val apiService = FakeApiService(
            todayStatusResponses = ArrayDeque(
                listOf(
                    createTodayStatusResponse(activeAttendanceId = 321, stateKey = "active"),
                    createTodayStatusResponse(activeAttendanceId = null, stateKey = "unavailable"),
                    createTodayStatusResponse(activeAttendanceId = null, stateKey = "completed")
                )
            )
        )
        val repository = createRepository(apiService)
        val attendancePreference = createAttendancePreference()

        repository.getTodayStatus(forceRefresh = true).getOrThrow()
        assertEquals(321, attendancePreference.getActiveAttendanceId().first())

        repository.getTodayStatus(forceRefresh = true).getOrThrow()
        assertEquals(321, attendancePreference.getActiveAttendanceId().first())

        repository.getTodayStatus(forceRefresh = true).getOrThrow()
        assertNull(attendancePreference.getActiveAttendanceId().first())
    }

    @Test
    fun `clears today status cache after successful check in and check out mutations`() = runTest {
        val apiService = FakeApiService(
            checkInResponse = createAttendanceResponse(idAttendance = 777),
            checkOutResponse = createAttendanceResponse(idAttendance = 777)
        )
        val repository = createRepository(apiService)
        val todayStatusPreference = createTodayStatusPreference()
        val cachedPayload = CachedTodayStatusPayload(
            userId = "42",
            todayDate = LocalDate.now().toString(),
            attendanceSessionStateId = 2,
            attendanceSessionStateKey = "active",
            activeAttendanceId = 123,
            fetchedAtMillis = System.currentTimeMillis(),
            ttlSeconds = AuthRuntimePolicy.SHARED_TTL_SECONDS,
            status = createTodayStatusResponse(activeAttendanceId = 123, stateKey = "active").data.toDomainForTest()
        )

        todayStatusPreference.saveTodayStatusCache(cachedPayload)
        assertTrue(
            repository.checkIn(createAttendanceRequest())
                is com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult.Success
        )
        assertNull(todayStatusPreference.getTodayStatusCache().first())

        todayStatusPreference.saveTodayStatusCache(cachedPayload)
        assertTrue(
            repository.checkOut(attendanceId = 777, latitude = -0.842239, longitude = 119.892637)
                is com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult.Success
        )
        assertNull(todayStatusPreference.getTodayStatusCache().first())
    }

    private suspend fun createRepository(apiService: FakeApiService): AttendanceRepositoryImpl {
        createUserPreference().saveSession(token = "token", userId = "42", refreshToken = "refresh")
        return AttendanceRepositoryImpl(
            apiService = apiService,
            attendancePreference = createAttendancePreference(),
            todayStatusPreference = createTodayStatusPreference(),
            userPreference = createUserPreference()
        )
    }

    private fun createUserPreference(): UserPreference = UserPreference(userDataStore)

    private fun createAttendancePreference(): AttendancePreference = AttendancePreference(attendanceDataStore)

    private fun createTodayStatusPreference(): TodayStatusPreference {
        return TodayStatusPreference(
            dataStore = todayStatusDataStore,
            gson = Gson()
        )
    }

    private fun createAttendanceRequest(): AttendanceRequestModel {
        return AttendanceRequestModel(
            categoryId = 1,
            latitude = -0.842239,
            longitude = 119.892637,
            notes = "",
            type = "check_in"
        )
    }
}

private fun createTodayStatusResponse(activeAttendanceId: Int?, stateKey: String): TodayStatusResponse {
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
            attendanceSessionState = AttendanceSessionStateDto(id = 2, key = stateKey, label = stateKey),
            activeAttendanceId = activeAttendanceId
        ),
        meta = TodayStatusMeta(cacheTtlSeconds = 300)
    )
}

private fun TodayStatusData.toDomainForTest() = TodayStatusResponse(success = true, data = this).toDomain()

private fun createAttendanceResponse(idAttendance: Int): AttendanceResponse {
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
            attendanceDate = "2026-07-05",
            notes = ""
        )
    )
}

private class FakeApiService(
    val todayStatusResponses: ArrayDeque<TodayStatusResponse> = ArrayDeque(),
    private val checkInResponse: AttendanceResponse = createAttendanceResponse(idAttendance = 1),
    private val checkOutResponse: AttendanceResponse = createAttendanceResponse(idAttendance = 1)
) : ApiService {
    var todayStatusCalls: Int = 0
        private set

    override suspend fun getTodayStatus(): TodayStatusResponse {
        todayStatusCalls += 1
        return todayStatusResponses.removeFirstOrNull() ?: createTodayStatusResponse(activeAttendanceId = 1, stateKey = "active")
    }

    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse = checkInResponse

    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse = checkOutResponse

    override suspend fun login(loginRequest: LoginRequest): LoginResponse = unsupported()

    override suspend fun getUserProfile(bootstrapAuthRequest: String?): LoginResponse = unsupported()

    override suspend fun logout(): LogoutResponse = unsupported()

    override suspend fun logoutWithRefresh(request: LogoutRequest): LogoutResponse = unsupported()

    override suspend fun refresh(request: RefreshRequest): RefreshResponse = unsupported()

    override suspend fun getAttendanceHistory(period: String, page: Int, limit: Int): AttendanceHistoryResponse = unsupported()

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

    override suspend fun updateUserProfile(userId: Int, request: ProfileUpdateRequest): ProfileUpdateResponse = unsupported()

    override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> = unsupported()

    override suspend fun getWfaRecommendations(latitude: Double, longitude: Double): WfaRecommendationResponse = unsupported()

    override suspend fun getWfaRequestConfig():
        com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto = unsupported()

    override suspend fun getBookingHistory(
        status: String?,
        page: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String
    ): BookingHistoryResponse = unsupported()


    override suspend fun submitWfaRequest(
        request: com.example.infinite_track.data.soucre.network.request.WfaRequestDto
    ): com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDto = unsupported()

    private fun unsupported(): Nothing = error("Not supported in this test")
}
