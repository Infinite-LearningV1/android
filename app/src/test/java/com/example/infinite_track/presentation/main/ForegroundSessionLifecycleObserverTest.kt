package com.example.infinite_track.presentation.main

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.ForceReauthUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.inject.Provider

class ForegroundSessionLifecycleObserverTest {

    @Test
    fun `onStart suppresses initial cold start validation`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val validationRepository = FakeValidationRepository(
            syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser()))
        )
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver.createForTest(
            validateForegroundSessionUseCase = ValidateForegroundSessionUseCase(
                authRepository = validationRepository,
                userPreference = userPreference,
                sessionManager = sessionManager
            ),
            sessionManager = sessionManager,
            forceReauthUseCaseProvider = forceReauthProvider(sessionManager),
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(0, observer.validationCount)
        assertEquals(0, validationRepository.syncCallCount)
        assertEquals(0, logoutRepository.logoutCallCount)
        assertFalse(sessionManager.sessionExpired.value)
    }

    @Test
    fun `onStart validates once and ignores re-entry while running`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver.createForTest(
            validateForegroundSessionUseCase = FixedForegroundValidationUseCase(ForegroundSessionValidationResult.Valid),
            sessionManager = sessionManager,
            forceReauthUseCaseProvider = forceReauthProvider(sessionManager),
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)
        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(1, observer.validationCount)
        assertEquals(0, logoutRepository.logoutCallCount)
        assertFalse(sessionManager.sessionExpired.value)
    }

    @Test
    fun `reauth required clears local runtime and triggers forced reauth`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        var localRuntimeClearCalls = 0
        val observer = ForegroundSessionLifecycleObserver.createForTest(
            validateForegroundSessionUseCase = FixedForegroundValidationUseCase(
                ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_INVALID)
            ),
            sessionManager = sessionManager,
            forceReauthUseCaseProvider = forceReauthProvider(sessionManager) { localRuntimeClearCalls += 1 },
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)
        scope.advanceUntilIdle()
        withTimeout(2_000) {
            while (localRuntimeClearCalls != 1) {
                delay(10)
                scope.advanceUntilIdle()
            }
        }

        assertEquals(1, localRuntimeClearCalls)
        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
    }

    @Test
    fun `temporary failure does not trigger logout or forced reauth`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver.createForTest(
            validateForegroundSessionUseCase = FixedForegroundValidationUseCase(
                ForegroundSessionValidationResult.TemporaryFailure("offline", null)
            ),
            sessionManager = sessionManager,
            forceReauthUseCaseProvider = forceReauthProvider(sessionManager),
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(0, logoutRepository.logoutCallCount)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(null, sessionManager.reauthReason.value)
    }

    @Test
    fun `unexpected validation exception is not swallowed as temporary failure`() = runTest {
        val capturedFailures = mutableListOf<Throwable>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(
            dispatcher + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
                capturedFailures += throwable
            }
        )
        val sessionManager = SessionManager()
        val logoutRepository = FakeLogoutRepository()
        val expectedFailure = IllegalStateException("unexpected validator failure")
        val observer = ForegroundSessionLifecycleObserver.createForTest(
            validateForegroundSessionUseCase = ThrowingForegroundValidationUseCase(expectedFailure),
            sessionManager = sessionManager,
            forceReauthUseCaseProvider = forceReauthProvider(sessionManager),
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L),
            unexpectedFailureLogger = { capturedFailures += it }
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)
        advanceUntilIdle()

        assertEquals(1, observer.validationCount)
        assertTrue(capturedFailures.isNotEmpty())
        assertTrue(capturedFailures.all { it === expectedFailure })
        assertEquals(0, logoutRepository.logoutCallCount)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(null, sessionManager.reauthReason.value)
    }

    private fun createUserPreference(): UserPreference {
        val tempDir = createTempDir(prefix = "foreground-observer-")
        val appContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(appContext.filesDir, "user.preferences_pb") }
        )
        return UserPreference(dataStore)
    }

    private fun forceReauthProvider(
        sessionManager: SessionManager,
        onClearRuntime: () -> Unit = {}
    ): Provider<ForceReauthUseCase> {
        return Provider {
            ForceReauthUseCase(sessionManager) {
                onClearRuntime()
            }
        }
    }


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
}

private class FixedForegroundValidationUseCase(
    private val result: ForegroundSessionValidationResult
) : ValidateForegroundSessionUseCase(
    authRepository = FakeValidationRepository(syncResults = mutableListOf()),
    userPreference = run {
        val tempDir = createTempDir(prefix = "foreground-observer-fixed-")
        val appContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(appContext.filesDir, "user.preferences_pb") }
        )
        UserPreference(dataStore)
    },
    sessionManager = SessionManager()
) {
    override suspend fun invoke(): ForegroundSessionValidationResult = result
}

private class ThrowingForegroundValidationUseCase(
    private val throwable: Throwable
) : ValidateForegroundSessionUseCase(
    authRepository = FakeValidationRepository(syncResults = mutableListOf()),
    userPreference = run {
        val tempDir = createTempDir(prefix = "foreground-observer-throwing-")
        val appContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(appContext.filesDir, "user.preferences_pb") }
        )
        UserPreference(dataStore)
    },
    sessionManager = SessionManager()
) {
    override suspend fun invoke(): ForegroundSessionValidationResult {
        throw throwable
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle = LifecycleRegistry(this)
}


private class FakeValidationRepository(
    private val syncResults: MutableList<ProfileSyncResult>,
    private val refreshFailureReason: AuthRefreshFailureReason? = null,
    val syncException: Throwable? = null
) : AuthRepository {
    var syncCallCount: Int = 0
        private set

    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        error("Not used")
    }

    override suspend fun logout(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun refreshSession(): Result<AuthRefreshResult> {
        return if (refreshFailureReason == null) {
            Result.success(AuthRefreshResult("new-access", "new-refresh", "1"))
        } else {
            Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = refreshFailureReason,
                    message = "reauth"
                )
            )
        }
    }

    override suspend fun syncUserProfile(): ProfileSyncResult {
        syncCallCount += 1
        syncException?.let { throw it }
        return syncResults.removeAt(0)
    }

    override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
        error("Not used")
    }

    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)

    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
        error("Not used")
    }
}

private class FakeLogoutRepository : AuthRepository {
    var logoutCallCount: Int = 0
        private set

    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        error("Not used")
    }

    override suspend fun logout(): Result<Unit> {
        logoutCallCount += 1
        return Result.success(Unit)
    }

    override suspend fun refreshSession(): Result<AuthRefreshResult> {
        error("Not used")
    }

    override suspend fun syncUserProfile(): ProfileSyncResult {
        error("Not used")
    }

    override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
        error("Not used")
    }

    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)

    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
        error("Not used")
    }
}
