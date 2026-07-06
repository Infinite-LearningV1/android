package com.example.infinite_track.data.repository.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.data.soucre.network.request.LogoutRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.response.AuthPayload
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.LocationData
import com.example.infinite_track.data.soucre.network.response.RefreshSessionData
import com.example.infinite_track.data.soucre.network.response.RefreshSessionResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.UserData
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.Locale

class AuthRepositoryImplRefreshSessionTest {

    @Test
    fun `login succeeds when refresh token is null in login response`() = runBlocking {
        val userPreference = createUserPreference()
        val userDao = CapturingUserDao()
        val repository = createLoginRepository(
            userPreference = userPreference,
            userDao = userDao,
            userData = createUserData(refreshToken = null)
        )

        val result = repository.login(LoginCredentials(email = "user@example.com", password = "secret"))

        assertTrue(result.isSuccess)
        assertEquals("user@example.com", result.getOrNull()?.email)
        assertEquals("access-token", userPreference.getAuthToken().first())
        assertTrue(userPreference.getRefreshToken().first().isBlank())
        assertEquals("10", userPreference.getUserId().first())
        assertEquals(1, userDao.insertedUsers.size)
    }

    @Test
    fun `login succeeds when refresh token is blank in login response`() = runBlocking {
        val userPreference = createUserPreference()
        val userDao = CapturingUserDao()
        val repository = createLoginRepository(
            userPreference = userPreference,
            userDao = userDao,
            userData = createUserData(refreshToken = "   ")
        )

        val result = repository.login(LoginCredentials(email = "user@example.com", password = "secret"))

        assertTrue(result.isSuccess)
        assertEquals("user@example.com", result.getOrNull()?.email)
        assertEquals("access-token", userPreference.getAuthToken().first())
        assertTrue(userPreference.getRefreshToken().first().isBlank())
        assertEquals("10", userPreference.getUserId().first())
        assertEquals(1, userDao.insertedUsers.size)
    }

    @Test
    fun `login persists primary auth token payload before legacy direct fields`() = runBlocking {
        val userPreference = createUserPreference()
        val userDao = CapturingUserDao()
        val repository = createLoginRepository(
            userPreference = userPreference,
            userDao = userDao,
            userData = createUserData(refreshToken = "legacy-refresh").copy(
                token = "legacy-access",
                auth = AuthPayload(
                    accessToken = "primary-access",
                    refreshToken = "primary-refresh"
                )
            )
        )

        val result = repository.login(LoginCredentials(email = "user@example.com", password = "secret"))

        assertTrue(result.isSuccess)
        assertEquals("primary-access", userPreference.getAuthToken().first())
        assertEquals("primary-refresh", userPreference.getRefreshToken().first())
        assertEquals("10", userPreference.getUserId().first())
    }

    @Test
    fun `login fails when access token is blank in login response`() = runBlocking {
        val userPreference = createUserPreference()
        val userDao = CapturingUserDao()
        val repository = createLoginRepository(
            userPreference = userPreference,
            userDao = userDao,
            userData = createUserData(refreshToken = "refresh-token").copy(token = "   ")
        )

        val result = repository.login(LoginCredentials(email = "user@example.com", password = "secret"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("access token", ignoreCase = true) == true)
        assertTrue(userPreference.getAuthToken().first().isBlank())
        assertTrue(userPreference.getRefreshToken().first().isBlank())
        assertTrue(userPreference.getUserId().first().isBlank())
        assertTrue(userDao.insertedUsers.isEmpty())
    }

    @Test
    fun `sync user profile returns success and preserves existing face embedding`() = runBlocking {
        val cachedUser = UserEntity(
            id = 10,
            fullName = "Cached User",
            email = "cached@example.com",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "12345",
            phone = "08123456789",
            photo = "cached.jpg",
            photoUpdatedAt = "2026-01-01T00:00:00Z",
            latitude = -0.9,
            longitude = 119.8,
            radius = 100,
            locationDescription = "Office",
            locationCategoryName = "WFO",
            faceEmbedding = byteArrayOf(1, 2, 3)
        )
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val userDao = CapturingUserDao(initialUser = cachedUser)
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    LoginResponse(
                        success = true,
                        message = "ok",
                        data = createUserData(refreshToken = "refresh-token")
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = userDao
        )

        val result = repository.syncUserProfile()

        assertTrue(result is ProfileSyncResult.Success)
        val syncedUser = (result as ProfileSyncResult.Success).user
        assertEquals("user@example.com", syncedUser.email)
        assertEquals(1, userDao.insertedUsers.size)
        assertTrue(userDao.insertedUsers.single().faceEmbedding.contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `bootstrap sync marks profile request with bootstrap header value`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val apiService = FakeApiService(
            getUserProfileBlock = { bootstrapMarker ->
                assertEquals(ApiService.BOOTSTRAP_AUTH_REQUEST_VALUE, bootstrapMarker)
                LoginResponse(
                    success = true,
                    message = "ok",
                    data = createUserData(refreshToken = "refresh-token")
                )
            }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = apiService,
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        val result = repository.syncUserProfileForBootstrap()

        assertTrue(result is ProfileSyncResult.Success)
    }

    @Test
    fun `sync user profile returns unauthorized when token is missing locally`() = runBlocking {
        val repository = AuthRepositoryImpl(
            userPreference = createUserPreference(),
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw AssertionError("Profile endpoint should not be called without a local access token")
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        val result = repository.syncUserProfile()

        assertTrue(result is ProfileSyncResult.Unauthorized)
    }

    @Test
    fun `sync user profile returns unauthorized on 401 response`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw httpException(
                        code = 401,
                        error = ErrorResponse(success = false, message = "unauthorized", code = "UNAUTHORIZED")
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        val result = repository.syncUserProfile()

        assertTrue(result is ProfileSyncResult.Unauthorized)
        assertEquals(null, (result as ProfileSyncResult.Unauthorized).reason)
    }

    @Test
    fun `bootstrap profile sync preserves inactive session unauthorized reason from 401 auth code`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw httpException(
                        code = 401,
                        error = ErrorResponse(
                            success = false,
                            message = "session inactive for more than 48 hours",
                            code = "AUTH_SESSION_INACTIVE"
                        )
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        val result = repository.syncUserProfileForBootstrap()

        assertTrue(result is ProfileSyncResult.Unauthorized)
        assertEquals(
            AuthRefreshFailureReason.INACTIVITY_EXPIRED,
            (result as ProfileSyncResult.Unauthorized).reason
        )
    }

    @Test
    fun `bootstrap profile sync preserves revoked refresh unauthorized reason from 401 auth code`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw httpException(
                        code = 401,
                        error = ErrorResponse(
                            success = false,
                            message = "refresh token revoked",
                            code = "AUTH_REFRESH_TOKEN_REVOKED"
                        )
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        val result = repository.syncUserProfileForBootstrap()

        assertTrue(result is ProfileSyncResult.Unauthorized)
        assertEquals(
            AuthRefreshFailureReason.REFRESH_REVOKED,
            (result as ProfileSyncResult.Unauthorized).reason
        )
    }

    @Test
    fun `sync user profile returns temporary failure on http exception even when cached user exists`() = runBlocking {
        val cachedUser = UserEntity(
            id = 10,
            fullName = "Cached User",
            email = "cached@example.com",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "12345",
            phone = "08123456789",
            photo = "cached.jpg",
            photoUpdatedAt = "2026-01-01T00:00:00Z",
            latitude = -0.9,
            longitude = 119.8,
            radius = 100,
            locationDescription = "Office",
            locationCategoryName = "WFO",
            faceEmbedding = null
        )

        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }

        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw httpException(
                        code = 500,
                        error = ErrorResponse(success = false, message = "server down", code = "SERVER_ERROR")
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = CachedUserDao(cachedUser)
        )

        val result = repository.syncUserProfile()

        assertTrue(result is ProfileSyncResult.TemporaryFailure)
    }

    @Test
    fun `sync user profile returns temporary failure on io exception even when cached user exists`() = runBlocking {
        val cachedUser = UserEntity(
            id = 10,
            fullName = "Cached User",
            email = "cached@example.com",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "12345",
            phone = "08123456789",
            photo = "cached.jpg",
            photoUpdatedAt = "2026-01-01T00:00:00Z",
            latitude = -0.9,
            longitude = 119.8,
            radius = 100,
            locationDescription = "Office",
            locationCategoryName = "WFO",
            faceEmbedding = null
        )

        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }

        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw IOException("timeout")
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = CachedUserDao(cachedUser)
        )

        val result = repository.syncUserProfile()

        assertTrue(result is ProfileSyncResult.TemporaryFailure)
    }

    @Test
    fun `sync user profile rethrows unexpected internal exception`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw IllegalStateException("boom")
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        try {
            repository.syncUserProfile()
            fail("Expected IllegalStateException to be rethrown")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }

    @Test
    fun `sync user profile rethrows unexpected error body read failure`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "access-token", userId = "10", refreshToken = "refresh-token")
        }
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                getUserProfileBlock = { _ ->
                    throw throwingRawHttpException(code = 500, error = IOException("body read failed"))
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = FakeUserDao()
        )

        try {
            repository.syncUserProfile()
            fail("Expected IOException to be rethrown")
        } catch (e: IOException) {
            assertEquals("body read failed", e.message)
        }
    }

    @Test
    fun `logout remote uses auth session api service without clearing local state`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "access-token", userId = "10", refreshToken = null)
        val userDao = CapturingUserDao()
        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = { unsupportedRefreshSession() },
            logoutBlock = {
                LogoutResponse(message = "ok")
            }
        )

        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(),
            authSessionApiService = fakeAuthSessionApi,
            userDao = userDao
        )

        val result = repository.logoutRemote()

        assertTrue(result.isSuccess)
        assertEquals(1, fakeAuthSessionApi.logoutCallCount)
        assertEquals("access-token", userPreference.getAuthToken().first())
        assertTrue(userPreference.getRefreshToken().first().isBlank())
        assertEquals("10", userPreference.getUserId().first())
    }

    @Test
    fun `refresh session returns reauth required without calling backend when refresh token is missing locally`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession(token = "existing-access", userId = "10", refreshToken = null)
        }
        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = { unsupportedRefreshSession() }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { request -> fakeAuthSessionApi.refreshSession(request) }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
        assertEquals(null, fakeAuthSessionApi.lastRefreshRequest)
        assertEquals("existing-access", userPreference.getAuthToken().first())
        assertTrue(userPreference.getRefreshToken().first().isBlank())
        assertEquals("10", userPreference.getUserId().first())
    }

    @Test
    fun `refresh session returns reauth required when refresh token revoked`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw httpException(
                        code = 401,
                        error = ErrorResponse(
                            success = false,
                            message = "refresh token revoked",
                            code = "AUTH_REFRESH_TOKEN_REVOKED"
                        )
                    )
                }
            )
        )

        val result = repository.refreshSession()

        assertRefreshFailure(
            result = result,
            kind = AuthRefreshFailureKind.NON_REFRESHABLE,
            reason = AuthRefreshFailureReason.REFRESH_REVOKED
        )
    }

    @Test
    fun `refresh session returns inactivity reauth required when inactivity exceeded 48h`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw httpException(
                        code = 401,
                        error = ErrorResponse(
                            success = false,
                            message = "session inactive for more than 48 hours",
                            code = "AUTH_SESSION_INACTIVE"
                        )
                    )
                }
            )
        )

        val result = repository.refreshSession()

        assertRefreshFailure(
            result = result,
            kind = AuthRefreshFailureKind.NON_REFRESHABLE,
            reason = AuthRefreshFailureReason.INACTIVITY_EXPIRED
        )
    }

    @Test
    fun `refresh session uses locale-stable error code normalization with Locale ROOT`() = runBlocking {
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale("tr", "TR"))
        try {
            val repository = createRepository(
                refreshApiService = FakeAuthSessionApiService(
                    refreshSessionBlock = {
                        throw httpException(
                            code = 401,
                            error = ErrorResponse(
                                success = false,
                                message = "session inactive for more than 48 hours",
                                code = "auth_session_inactive"
                            )
                        )
                    }
                )
            )

            val result = repository.refreshSession()

            assertRefreshFailure(
                result = result,
                kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                reason = AuthRefreshFailureReason.INACTIVITY_EXPIRED
            )
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun `refresh session rethrows cancellation exception`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw CancellationException("cancelled")
                }
            )
        )

        try {
            repository.refreshSession()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun `refresh session returns temporary failure on network exception`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw IOException("timeout")
                }
            )
        )

        val result = repository.refreshSession()

        assertRefreshFailure(
            result = result,
            kind = AuthRefreshFailureKind.TRANSPORT,
            reason = AuthRefreshFailureReason.TRANSPORT_ERROR
        )
    }

    @Test
    fun `refresh session returns transport failure on server side refresh error`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw httpException(
                        code = 500,
                        error = ErrorResponse(
                            success = false,
                            message = "temporary server error",
                            code = "TEMPORARY_REFRESH_FAILURE"
                        )
                    )
                }
            )
        )

        val result = repository.refreshSession()

        assertRefreshFailure(
            result = result,
            kind = AuthRefreshFailureKind.TRANSPORT,
            reason = AuthRefreshFailureReason.TRANSPORT_ERROR
        )
    }

    @Test
    fun `refresh session returns transport failure for unknown 429 response`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw httpException(
                        code = 429,
                        error = ErrorResponse(
                            success = false,
                            message = "rate limited",
                            code = "RATE_LIMITED"
                        )
                    )
                }
            )
        )

        val result = repository.refreshSession()

        assertRefreshFailure(
            result = result,
            kind = AuthRefreshFailureKind.TRANSPORT,
            reason = AuthRefreshFailureReason.TRANSPORT_ERROR
        )
    }

    @Test
    fun `refresh session uses auth session api and stores primary auth token payload`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")
        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = {
                RefreshSessionResponse(
                    success = true,
                    message = "ok",
                    data = RefreshSessionData(
                        id = 10,
                        token = "legacy-new-access",
                        refreshToken = "legacy-new-refresh",
                        auth = AuthPayload(
                            accessToken = "primary-new-access",
                            refreshToken = "primary-new-refresh"
                        )
                    )
                )
            }
        )

        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { _ ->
                    throw AssertionError("refresh must use AuthSessionApiService, not protected ApiService")
                }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("primary-new-access", userPreference.getAuthToken().first())
        assertEquals("primary-new-refresh", userPreference.getRefreshToken().first())
        assertEquals("old-refresh", fakeAuthSessionApi.lastRefreshRequest?.refreshToken)
    }

    @Test
    fun `refresh session success stores latest tokens`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")
        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = {
                RefreshSessionResponse(
                    success = true,
                    message = "ok",
                    data = RefreshSessionData(
                        id = 10,
                        token = "new-access",
                        refreshToken = "new-refresh"
                    )
                )
            }
        )

        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { request -> fakeAuthSessionApi.refreshSession(request) }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("new-access", userPreference.getAuthToken().first())
        assertEquals("new-refresh", userPreference.getRefreshToken().first())
        assertEquals("old-refresh", fakeAuthSessionApi.lastRefreshRequest?.refreshToken)
    }

    @Test
    fun `refresh session success with null refresh token keeps existing refresh token`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")

        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = {
                RefreshSessionResponse(
                    success = true,
                    message = "ok",
                    data = RefreshSessionData(
                        id = 10,
                        token = "new-access",
                        refreshToken = null
                    )
                )
            }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { request -> fakeAuthSessionApi.refreshSession(request) }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("new-access", userPreference.getAuthToken().first())
        assertEquals("old-refresh", userPreference.getRefreshToken().first())
    }

    @Test
    fun `refresh session returns temporary failure and does not persist when access token is blank`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")

        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = {
                RefreshSessionResponse(
                    success = true,
                    message = "ok",
                    data = RefreshSessionData(
                        id = 10,
                        token = "   ",
                        refreshToken = "new-refresh"
                    )
                )
            }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { request -> fakeAuthSessionApi.refreshSession(request) }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
        assertEquals("old-access", userPreference.getAuthToken().first())
        assertEquals("old-refresh", userPreference.getRefreshToken().first())
        assertEquals("10", userPreference.getUserId().first())
    }

    @Test
    fun `refresh session returns temporary failure and does not persist when user id is invalid`() = runBlocking {
        val userPreference = createUserPreference()
        userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")

        val fakeAuthSessionApi = FakeAuthSessionApiService(
            refreshSessionBlock = {
                RefreshSessionResponse(
                    success = true,
                    message = "ok",
                    data = RefreshSessionData(
                        id = 0,
                        token = "new-access",
                        refreshToken = "new-refresh"
                    )
                )
            }
        )
        val repository = AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                refreshBlock = { request -> fakeAuthSessionApi.refreshSession(request) }
            ),
            authSessionApiService = fakeAuthSessionApi,
            userDao = FakeUserDao()
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
        assertEquals("old-access", userPreference.getAuthToken().first())
        assertEquals("old-refresh", userPreference.getRefreshToken().first())
        assertEquals("10", userPreference.getUserId().first())
    }

    @Test
    fun `refresh session rethrows unexpected internal exception`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw IllegalStateException("boom")
                }
            )
        )

        try {
            repository.refreshSession()
            fail("Expected IllegalStateException to be rethrown")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }

    @Test
    fun `refresh session rethrows unexpected error body read failure`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw throwingRawHttpException(code = 500, error = IOException("body read failed"))
                }
            )
        )

        try {
            repository.refreshSession()
            fail("Expected IOException to be rethrown")
        } catch (e: IOException) {
            assertEquals("body read failed", e.message)
        }
    }

    @Test
    fun `refresh session returns temporary failure when error body is malformed json`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw rawHttpException(code = 500, rawBody = "not-json")
                }
            )
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
    }

    @Test
    fun `refresh session returns temporary failure when error body is empty`() = runBlocking {
        val repository = createRepository(
            refreshApiService = FakeAuthSessionApiService(
                refreshSessionBlock = {
                    throw rawHttpException(code = 500, rawBody = "")
                }
            )
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
    }

    private fun assertRefreshFailure(
        result: Result<AuthRefreshResult>,
        kind: AuthRefreshFailureKind,
        reason: AuthRefreshFailureReason
    ) {
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AuthRefreshException
        assertEquals(kind, exception.kind)
        assertEquals(reason, exception.reason)
    }

    private fun createRepository(
        refreshApiService: AuthSessionApiService,
        apiService: ApiService = FakeApiService(
            refreshBlock = { request -> refreshApiService.refreshSession(request) }
        )
    ): AuthRepositoryImpl {
        val userPreference = createUserPreference().also {
            runBlocking {
                it.saveSession(token = "existing-access", userId = "10", refreshToken = "existing-refresh")
            }
        }

        return AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = apiService,
            authSessionApiService = refreshApiService,
            userDao = FakeUserDao()
        )
    }

    private fun createLoginRepository(
        userPreference: UserPreference,
        userDao: UserDao,
        userData: UserData
    ): AuthRepositoryImpl {
        return AuthRepositoryImpl(
            userPreference = userPreference,
            apiService = FakeApiService(
                loginBlock = {
                    LoginResponse(
                        success = true,
                        message = "ok",
                        data = userData
                    )
                }
            ),
            authSessionApiService = FakeAuthSessionApiService(
                refreshSessionBlock = { unsupportedRefreshSession() }
            ),
            userDao = userDao
        )
    }

    private fun createUserPreference(): UserPreference {
        val testFile = File.createTempFile("user_prefs", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
        return UserPreference(dataStore)
    }

    private fun httpException(code: Int, error: ErrorResponse): HttpException {
        val body = "{\"success\":${error.success},\"message\":\"${error.message}\",\"code\":\"${error.code}\"}"
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    private fun rawHttpException(code: Int, rawBody: String): HttpException {
        val body = rawBody.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    private fun throwingRawHttpException(code: Int, error: IOException): HttpException {
        val body = object : ResponseBody() {
            override fun contentType() = "application/json".toMediaType()

            override fun contentLength(): Long = 0

            override fun source(): BufferedSource {
                throw error
            }
        }
        return HttpException(Response.error<Any>(code, body))
    }

    private fun createUserData(refreshToken: String?): UserData {
        return UserData(
            id = 10,
            fullName = "Test User",
            email = "user@example.com",
            roleName = "staff",
            positionName = "Engineer",
            programName = "Program",
            divisionName = "Division",
            nipNim = "12345",
            phone = "08123456789",
            photo = "photo.jpg",
            photoUpdatedAt = "2026-01-01T00:00:00Z",
            location = LocationData(
                latitude = -0.9,
                longitude = 119.8,
                radius = 100,
                description = "Office",
                categoryName = "WFO"
            ),
            token = "access-token",
            refreshToken = refreshToken,
            auth = null
        )
    }

    private suspend fun unsupportedRefreshSession(): RefreshSessionResponse {
        throw NotImplementedError("refreshSession not expected in this test")
    }
}

private class FakeUserDao : UserDao {
    override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
        // no-op
    }

    override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)

    override suspend fun getUserProfile(): UserEntity? = null

    override suspend fun clearUserProfile() {
        // no-op
    }
}

private class CapturingUserDao(
    private val initialUser: UserEntity? = null
) : UserDao {
    val insertedUsers = mutableListOf<UserEntity>()
    var clearCallCount: Int = 0

    override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
        insertedUsers += userEntity
    }

    override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(insertedUsers.lastOrNull() ?: initialUser)

    override suspend fun getUserProfile(): UserEntity? = insertedUsers.lastOrNull() ?: initialUser

    override suspend fun clearUserProfile() {
        clearCallCount += 1
    }
}

private class CachedUserDao(
    private val cachedUser: UserEntity
) : UserDao {
    override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) {
        // no-op
    }

    override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(cachedUser)

    override suspend fun getUserProfile(): UserEntity? = cachedUser

    override suspend fun clearUserProfile() {
        // no-op
    }
}

private class FakeApiService(
    private val loginBlock: suspend (LoginRequest) -> LoginResponse = { throw NotImplementedError("login not configured in test") },
    private val getUserProfileBlock: suspend (String?) -> LoginResponse = { throw NotImplementedError("getUserProfile not configured in test") },
    private val refreshBlock: suspend (RefreshSessionRequest) -> RefreshSessionResponse = { throw NotImplementedError("refresh not configured in test") }
) : ApiService {
    var lastRefreshRequest: RefreshSessionRequest? = null

    override suspend fun login(credentials: LoginCredentials): LoginResponse {
        return loginBlock(loginRequest)
    }

    override suspend fun getUserProfile(bootstrapAuthRequest: String?): LoginResponse {
        return getUserProfileBlock(bootstrapAuthRequest)
    }

    override suspend fun logout(): LogoutResponse {
        throw NotImplementedError()
    }

    override suspend fun logoutWithRefresh(request: LogoutRequest): LogoutResponse {
        throw NotImplementedError()
    }

    override suspend fun refresh(request: RefreshSessionRequest): RefreshSessionResponse {
        lastRefreshRequest = request
        return refreshBlock(request)
    }

    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse {
        throw NotImplementedError()
    }

    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse {
        throw NotImplementedError()
    }

    override suspend fun getTodayStatus(): TodayStatusResponse {
        throw NotImplementedError()
    }

    override suspend fun getAttendanceHistory(period: String, page: Int, limit: Int): AttendanceHistoryResponse {
        throw NotImplementedError()
    }

    override suspend fun updateUserProfile(userId: Int, request: ProfileUpdateRequest): ProfileUpdateResponse {
        throw NotImplementedError()
    }

    override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> {
        throw NotImplementedError()
    }

    override suspend fun getWfaRecommendations(latitude: Double, longitude: Double): WfaRecommendationResponse {
        throw NotImplementedError()
    }

    override suspend fun getBookingHistory(
        status: String?,
        page: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String
    ): BookingHistoryResponse {
        throw NotImplementedError()
    }

    override suspend fun submitWfaBooking(request: BookingRequest): BookingResponse {
        throw NotImplementedError()
    }
}

private class FakeAuthSessionApiService(
    private val refreshSessionBlock: suspend () -> RefreshSessionResponse,
    private val logoutBlock: suspend () -> LogoutResponse = { throw NotImplementedError("logout not configured in test") }
) : AuthSessionApiService {
    var lastRefreshRequest: RefreshSessionRequest? = null
    var logoutCallCount: Int = 0

    override suspend fun refreshSession(request: RefreshSessionRequest): RefreshSessionResponse {
        lastRefreshRequest = request
        return refreshSessionBlock()
    }

    override suspend fun logout(): LogoutResponse {
        logoutCallCount += 1
        return logoutBlock()
    }
}
