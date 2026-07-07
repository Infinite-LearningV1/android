package com.example.infinite_track.domain.use_case.auth

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class CheckSessionUseCaseTest {

    @Test
    fun `bootstrap validates refresh once then uses bootstrap scoped profile sync`() = runBlocking {
        val user = sampleUser()
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.Success(user)),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = user
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(1, repository.bootstrapSyncCallCount)
        assertEquals(null, sessionManager.reauthReason.value)
    }

    @Test
    fun `bootstrap returns fresh local user without profile sync when freshness is still active`() = runBlocking {
        val user = sampleUser()
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
            it.saveLastProfileSyncAt(System.currentTimeMillis())
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.Success(user)),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = user
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.bootstrapSyncCallCount)
        assertEquals(null, sessionManager.reauthReason.value)
    }

    @Test
    fun `bootstrap does not persist freshness when face embedding recovery fails`() = runBlocking {
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser(photoUrl = null))),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser(faceEmbedding = null)
        )

        val result = createUseCase(repository, userPreference, SessionManager())()

        assertTrue(result.isFailure)
        assertEquals(0L, userPreference.getLastProfileSyncAt().first())
        assertEquals(1, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap forced reauth on non refreshable refresh failure`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = AuthRefreshFailureReason.INACTIVITY_EXPIRED,
                    message = "inactive"
                )
            ),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized preserves refresh revoked reason without global session expired dialog flag`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(),
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_REVOKED)
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized preserves inactivity reason without global session expired dialog flag`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(),
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.INACTIVITY_EXPIRED)
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized falls back to initial inactivity reason when retry omits reason`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.INACTIVITY_EXPIRED),
                ProfileSyncResult.Unauthorized()
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized falls back to initial refresh revoked reason when retry omits reason`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_REVOKED),
                ProfileSyncResult.Unauthorized()
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized keeps initial inactivity reason when retry reason is generic invalid`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.INACTIVITY_EXPIRED),
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_INVALID)
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap retry sync unauthorized keeps initial refresh revoked reason when retry reason is generic unknown`() = runBlocking {
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_REVOKED),
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.UNKNOWN)
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(2, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(2, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap unauthorized uses initial inactivity reason when refresh failure is generic invalid`() = runBlocking {
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.INACTIVITY_EXPIRED)
            ),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = AuthRefreshFailureReason.REFRESH_INVALID,
                    message = "invalid"
                )
            ),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.INACTIVITY_EXPIRED, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(1, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap unauthorized uses refresh missing token reason over generic initial access expired`() = runBlocking {
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.ACCESS_EXPIRED)
            ),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = AuthRefreshFailureReason.MISSING_REFRESH_TOKEN,
                    message = "missing refresh token"
                )
            ),
            loggedInUser = sampleUser()
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
        assertEquals(1, repository.bootstrapSyncCallCount)
    }

    @Test
    fun `bootstrap reports temporary failure on refresh transport failure while preserving cached session`() = runBlocking {
        val cachedUser = sampleUser()
        val userPreference = createUserPreference().also {
            it.saveSession("old-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.TRANSPORT,
                    reason = AuthRefreshFailureReason.TRANSPORT_ERROR,
                    message = "offline"
                )
            ),
            loggedInUser = cachedUser
        )
        val sessionManager = SessionManager()

        val result = createUseCase(repository, userPreference, sessionManager)()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.TemporaryFailure)
        assertEquals(null, sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals("old-access", userPreference.getAuthToken().first())
        assertEquals("refresh", userPreference.getRefreshToken().first())
        assertEquals(1, repository.refreshCallCount)
        assertEquals(0, repository.syncCallCount)
    }

    @Test
    fun `rethrows cancellation exception from sync`() = runBlocking {
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1")),
            syncThrowable = CancellationException("cancelled")
        )
        val useCase = createUseCase(repository, userPreference, SessionManager())

        try {
            useCase()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    private fun createUseCase(
        repository: FakeAuthRepository,
        userPreference: UserPreference,
        sessionManager: SessionManager
    ): CheckSessionUseCase {
        return CheckSessionUseCase(
            authRepository = repository,
            generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                authRepository = repository
            ),
            userPreference = userPreference,
            sessionManager = sessionManager
        )
    }

    private fun createUserPreference(): UserPreference {
        val testFile = File.createTempFile("check_session", ".preferences_pb").also { it.delete() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
        return UserPreference(dataStore)
    }

    private fun sampleUser(
        photoUrl: String? = "https://example.com/photo.jpg",
        faceEmbedding: ByteArray? = byteArrayOf(1, 2, 3)
    ): UserModel = UserModel(
        id = 1,
        fullName = "User",
        email = "user@example.com",
        roleName = "staff",
        positionName = "Engineer",
        programName = "Program",
        divisionName = "Division",
        nipNim = "123",
        phone = "0812",
        photoUrl = photoUrl,
        photoUpdatedAt = "2026-01-01T00:00:00Z",
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = faceEmbedding
    )

    private class FakeAuthRepository(
        private val syncResults: MutableList<ProfileSyncResult>,
        private val refreshSessionResult: Result<AuthRefreshResult>,
        private val loggedInUser: UserModel? = null,
        private val syncThrowable: Throwable? = null,
        private val refreshThrowable: Throwable? = null
    ) : AuthRepository {
        var syncCallCount: Int = 0
        var bootstrapSyncCallCount: Int = 0
        var refreshCallCount: Int = 0

        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            refreshCallCount += 1
            refreshThrowable?.let { throw it }
            return refreshSessionResult
        }

        override suspend fun login(credentials: LoginCredentials): Result<UserModel> {
            throw NotImplementedError()
        }

        override suspend fun syncUserProfile(): ProfileSyncResult {
            syncCallCount += 1
            syncThrowable?.let { throw it }
            return syncResults.removeFirst()
        }

        override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
            bootstrapSyncCallCount += 1
            syncThrowable?.let { throw it }
            return syncResults.removeFirst()
        }

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(loggedInUser)

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.success(Unit)
    }
}
