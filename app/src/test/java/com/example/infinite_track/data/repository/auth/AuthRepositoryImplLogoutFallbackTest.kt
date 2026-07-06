package com.example.infinite_track.data.repository.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.LogoutRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File

class AuthRepositoryImplLogoutFallbackTest {
    @Test
    fun `logout remote sends refresh token body without clearing local session`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(
                token = "expired-access-redacted",
                userId = "147",
                refreshToken = "refresh-token-redacted",
                lastRefreshAt = 1L
            )
        }
        val apiService = LogoutFallbackFakeApiService()
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = apiService,
            authSessionApiService = LogoutFallbackFakeAuthSessionApiService(),
            userDao = LogoutFallbackFakeUserDao()
        )

        val result = repository.logoutRemote()

        assertTrue(result.isSuccess)
        assertEquals("refresh-token-redacted", apiService.lastLogoutRequest?.refreshToken)
        assertEquals("expired-access-redacted", userPreference.getAuthToken().first())
        assertEquals("refresh-token-redacted", userPreference.getRefreshToken().first())
    }

    @Test
    fun `logout remote returns failure when backend fallback rejects expired access and preserves local session`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(
                token = "expired-access-redacted",
                userId = "147",
                refreshToken = "refresh-token-redacted",
                lastRefreshAt = 1L
            )
        }
        val apiService = LogoutFallbackFakeApiService(
            logoutBlock = { throw httpException(401) }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = apiService,
            authSessionApiService = LogoutFallbackFakeAuthSessionApiService(),
            userDao = LogoutFallbackFakeUserDao()
        )

        val result = repository.logoutRemote()

        assertTrue(result.isFailure)
        assertEquals("refresh-token-redacted", apiService.lastLogoutRequest?.refreshToken)
        assertEquals("expired-access-redacted", userPreference.getAuthToken().first())
        assertEquals("refresh-token-redacted", userPreference.getRefreshToken().first())
    }

    @Test
    fun `logout remote rethrows cancellation from backend logout and preserves local session`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(
                token = "expired-access-redacted",
                userId = "147",
                refreshToken = "refresh-token-redacted",
                lastRefreshAt = 1L
            )
        }
        val cancellation = CancellationException("cancelled")
        val apiService = LogoutFallbackFakeApiService(
            logoutBlock = { throw cancellation }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = apiService,
            authSessionApiService = LogoutFallbackFakeAuthSessionApiService(),
            userDao = LogoutFallbackFakeUserDao()
        )

        try {
            repository.logoutRemote()
            org.junit.Assert.fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertTrue(e === cancellation)
        }

        assertEquals("refresh-token-redacted", apiService.lastLogoutRequest?.refreshToken)
        assertEquals("expired-access-redacted", userPreference.getAuthToken().first())
        assertEquals("refresh-token-redacted", userPreference.getRefreshToken().first())
    }

    private fun createUserPreference(): UserPreference {
        val testFile = File.createTempFile("logout", ".preferences_pb").also { it.delete() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
        return UserPreference(dataStore)
    }

    private fun httpException(code: Int): HttpException {
        return HttpException(Response.error<Any>(code, "{}".toResponseBody(null)))
    }
}

private class LogoutFallbackFakeApiService(
    private val logoutBlock: suspend () -> LogoutResponse = { LogoutResponse(message = "ok") }
) : ApiService {
    var lastLogoutRequest: LogoutRequest? = null

    override suspend fun logoutWithRefresh(request: LogoutRequest): LogoutResponse {
        lastLogoutRequest = request
        return logoutBlock()
    }

    override suspend fun login(loginRequest: LoginRequest): LoginResponse = throw NotImplementedError()
    override suspend fun getUserProfile(bootstrapAuthRequest: String?): LoginResponse = throw NotImplementedError()
    override suspend fun logout(): LogoutResponse = throw NotImplementedError()
    override suspend fun refresh(request: RefreshRequest): RefreshResponse = throw NotImplementedError()
    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse = throw NotImplementedError()
    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse = throw NotImplementedError()
    override suspend fun getTodayStatus(): TodayStatusResponse = throw NotImplementedError()
    override suspend fun getAttendanceHistory(period: String, page: Int, limit: Int): AttendanceHistoryResponse = throw NotImplementedError()
    override suspend fun updateUserProfile(userId: Int, request: ProfileUpdateRequest): ProfileUpdateResponse = throw NotImplementedError()
    override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> = throw NotImplementedError()
    override suspend fun getWfaRecommendations(latitude: Double, longitude: Double): WfaRecommendationResponse = throw NotImplementedError()
    override suspend fun getBookingHistory(status: String?, page: Int, limit: Int, sortBy: String, sortOrder: String): BookingHistoryResponse = throw NotImplementedError()
    override suspend fun submitWfaBooking(request: BookingRequest): BookingResponse = throw NotImplementedError()
}

private class LogoutFallbackFakeAuthSessionApiService : AuthSessionApiService {
    override suspend fun refreshSession(request: RefreshRequest): RefreshResponse = throw NotImplementedError()
    override suspend fun logout(): LogoutResponse = LogoutResponse(message = "legacy ok")
}

private class LogoutFallbackFakeUserDao : UserDao {
    override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit
    override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)
    override suspend fun getUserProfile(): UserEntity? = null
    override suspend fun clearUserProfile() = Unit
}
