package com.example.infinite_track.domain.use_case.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ValidateForegroundSessionUseCaseTest {
    private lateinit var tempFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        tempFile = File.createTempFile("foreground-session-", ".preferences_pb").also { it.delete() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { tempFile }
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempFile.delete()
    }

    @Test
    fun `skips when auth token or refresh token is missing`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository()

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Skipped, result)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns existing reauth before skipped when tokens are blank`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.REFRESH_INVALID)
        }
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository()

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_INVALID),
            result
        )
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `skips while bootstrap session is already in progress`() = runBlocking {
        val sessionManager = SessionManager().also { it.beginBootstrapSession() }
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser())))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Skipped, result)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns existing reauth before valid when profile sync would succeed`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.REFRESH_REVOKED)
        }
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser())))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_REVOKED),
            result
        )
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns existing reauth before temporary failure when profile sync would fail temporarily`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.INACTIVITY_EXPIRED)
        }
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.TemporaryFailure(message = "offline", cause = null))
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.INACTIVITY_EXPIRED),
            result
        )
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns valid when profile sync succeeds through interceptor path`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser())))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Valid, result)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns temporary failure when interceptor-backed profile sync remains access-expired unauthorized`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.ACCESS_EXPIRED)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertTrue(result is ForegroundSessionValidationResult.TemporaryFailure)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
        assertEquals(false, sessionManager.sessionExpired.value)
    }

    @Test
    fun `returns temporary failure when interceptor-backed profile sync remains generic unauthorized`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Unauthorized()))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertTrue(result is ForegroundSessionValidationResult.TemporaryFailure)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
        assertEquals(false, sessionManager.sessionExpired.value)
    }

    @Test
    fun `returns existing reauth when interceptor already handled access-expired unauthorized terminal refresh failure`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.REFRESH_INVALID)
        }
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.ACCESS_EXPIRED)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_INVALID),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns existing reauth when interceptor already handled generic unauthorized terminal refresh failure`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.REFRESH_REVOKED)
        }
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Unauthorized()))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_REVOKED),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns reauth required when profile sync reports refresh revoked`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_REVOKED)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_REVOKED),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns reauth required when profile sync reports refresh invalid`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_INVALID)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_INVALID),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns reauth required when profile sync reports inactive session`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.INACTIVITY_EXPIRED)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.INACTIVITY_EXPIRED),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `preserves existing forced reauth reason when interceptor already handled terminal auth`() = runBlocking {
        val sessionManager = SessionManager().also {
            it.triggerForcedReauth(SessionManager.ReauthReason.REFRESH_REVOKED)
        }
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_INVALID)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_REVOKED),
            result
        )
        assertEquals(true, sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns temporary failure when profile sync reports transport failure`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.TemporaryFailure(message = "offline", cause = null)
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertTrue(result is ForegroundSessionValidationResult.TemporaryFailure)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
        assertEquals(false, sessionManager.sessionExpired.value)
    }

    private fun createUserPreference(): UserPreference = UserPreference(dataStore)

    private fun sampleUser(): UserModel = UserModel(
        id = 1,
        fullName = "Redacted User",
        email = "redacted@example.test",
        roleName = "Employee",
        positionName = "Engineer",
        programName = null,
        divisionName = "Mobile",
        nipNim = "EMP-001",
        phone = "08123456789",
        photoUrl = "https://example.test/photo.png",
        photoUpdatedAt = "2026-06-21T00:00:00Z",
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = null
    )

    private class FakeAuthRepository(
        private val syncResults: MutableList<ProfileSyncResult> = mutableListOf()
    ) : AuthRepository {
        var syncCallCount: Int = 0
        var refreshCallCount: Int = 0

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            error("Not used in this test")
        }

        override suspend fun logout(): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            refreshCallCount += 1
            error("Foreground validation must rely on syncUserProfile interceptor handling")
        }

        override suspend fun syncUserProfile(): ProfileSyncResult {
            syncCallCount += 1
            return syncResults.removeAt(0)
        }

        override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
            error("Bootstrap sync is not used by foreground validation")
        }

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
            error("Not used in this test")
        }
    }
}
