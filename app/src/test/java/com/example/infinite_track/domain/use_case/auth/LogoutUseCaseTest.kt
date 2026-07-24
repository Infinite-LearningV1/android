package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.ReauthReason
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.domain.repository.ProfileSyncResult
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LogoutUseCaseTest {

    @Test
    fun `remote success plus local success resets session and returns success`() = runTest {
        val sessionManager = staleSessionManager()
        val repository = FakeAuthRepository(Result.success(Unit))
        var clearRuntimeCalls = 0
        val useCase = createUseCase(repository, sessionManager) { clearRuntimeCalls += 1 }

        val outcome = useCase()

        assertEquals(LogoutOutcome.Success, outcome)
        assertEquals(1, repository.logoutRemoteCalls)
        assertEquals(1, clearRuntimeCalls)
        assertSessionReset(sessionManager)
    }

    @Test
    fun `remote failure plus local success clears reauth and returns warning`() = runTest {
        val sessionManager = staleSessionManager()
        val repository = FakeAuthRepository(Result.failure(IllegalStateException("offline")))
        val useCase = createUseCase(repository, sessionManager)

        val outcome = useCase()

        assertEquals(LogoutOutcome.SuccessWithRemoteWarning, outcome)
        assertEquals(1, repository.logoutRemoteCalls)
        assertSessionReset(sessionManager)
    }

    @Test
    fun `thrown remote failure still clears local runtime and returns warning`() = runTest {
        val sessionManager = staleSessionManager()
        var clearRuntimeCalls = 0
        val repository = FakeAuthRepository { throw IllegalStateException("offline") }
        val useCase = createUseCase(repository, sessionManager) { clearRuntimeCalls += 1 }

        val outcome = useCase()

        assertEquals(LogoutOutcome.SuccessWithRemoteWarning, outcome)
        assertEquals(1, clearRuntimeCalls)
        assertSessionReset(sessionManager)
    }

    @Test
    fun `local cleanup failure returns typed failure and preserves stale reauth state`() = runTest {
        val sessionManager = staleSessionManager()
        val failure = IllegalStateException("geofence cleanup failed")
        val useCase = createUseCase(
            repository = FakeAuthRepository(Result.success(Unit)),
            sessionManager = sessionManager,
            onClearRuntime = { throw failure }
        )

        val outcome = useCase()

        assertTrue(outcome is LogoutOutcome.LocalCleanupFailed)
        assertSame(failure, (outcome as LogoutOutcome.LocalCleanupFailed).cause)
        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(ReauthReason.UNKNOWN, sessionManager.reauthReason.value)
        assertFalse(sessionManager.beginSessionExpiryHandling())
    }

    @Test
    fun `remote cancellation is rethrown and skips local cleanup`() = runTest {
        var clearRuntimeCalls = 0
        val useCase = createUseCase(
            repository = FakeAuthRepository { throw CancellationException("cancelled remotely") },
            sessionManager = SessionManager(),
            onClearRuntime = { clearRuntimeCalls += 1 }
        )

        try {
            useCase()
            fail("Expected cancellation exception")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(0, clearRuntimeCalls)
    }

    @Test
    fun `remote cancellation result is rethrown and skips local cleanup`() = runTest {
        var clearRuntimeCalls = 0
        val useCase = createUseCase(
            repository = FakeAuthRepository(
                Result.failure(CancellationException("cancelled result"))
            ),
            sessionManager = SessionManager(),
            onClearRuntime = { clearRuntimeCalls += 1 }
        )

        try {
            useCase()
            fail("Expected cancellation exception")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(0, clearRuntimeCalls)
    }

    @Test
    fun `local cleanup cancellation is rethrown`() = runTest {
        val repository = FakeAuthRepository(Result.success(Unit))
        val useCase = createUseCase(repository, SessionManager()) {
            throw CancellationException("cancelled locally")
        }

        try {
            useCase()
            fail("Expected cancellation exception")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(1, repository.logoutRemoteCalls)
    }

    @Test
    fun `terminal forced reauth already cleaning cannot relatch after manual logout completes`() =
        runTest {
            val sessionManager = SessionManager()
            val forcedCleanupStarted = CompletableDeferred<Unit>()
            val finishForcedCleanup = CompletableDeferred<Unit>()
            val forcedReauth = ForceReauthUseCase(sessionManager) {
                forcedCleanupStarted.complete(Unit)
                finishForcedCleanup.await()
            }
            val forcedJob = launch {
                forcedReauth(ReauthReason.REFRESH_REVOKED)
            }
            forcedCleanupStarted.await()

            val outcome = createUseCase(
                repository = FakeAuthRepository(Result.success(Unit)),
                sessionManager = sessionManager
            )()
            finishForcedCleanup.complete(Unit)
            forcedJob.join()

            assertEquals(LogoutOutcome.Success, outcome)
            assertFalse(sessionManager.sessionExpired.value)
            assertNull(sessionManager.reauthReason.value)
        }

    @Test
    fun `successful login ends intentional logout suppression for later genuine forced reauth`() =
        runTest {
            val sessionManager = SessionManager()
            createUseCase(
                repository = FakeAuthRepository(Result.success(Unit)),
                sessionManager = sessionManager
            )()

            assertFalse(sessionManager.beginSessionExpiryHandling())

            sessionManager.onAuthenticatedSessionStarted()

            assertTrue(sessionManager.beginSessionExpiryHandling())
            sessionManager.triggerForcedReauth(ReauthReason.REFRESH_INVALID)
            assertTrue(sessionManager.sessionExpired.value)
            assertEquals(ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
        }

    private fun staleSessionManager() = SessionManager().also {
        assertTrue(it.beginSessionExpiryHandling())
        it.triggerForcedReauth(ReauthReason.UNKNOWN)
    }

    private fun assertSessionReset(sessionManager: SessionManager) {
        assertNull(sessionManager.reauthReason.value)
        assertFalse(sessionManager.sessionExpired.value)
        assertFalse(sessionManager.beginSessionExpiryHandling())
    }

    private fun createUseCase(
        repository: AuthRepository,
        sessionManager: SessionManager,
        onClearRuntime: () -> Unit = {}
    ) = LogoutUseCase(
        authRepository = repository,
        clearAuthenticatedRuntimeUseCase = ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleaner { onClearRuntime() }
        ),
        sessionManager = sessionManager
    )

    private class FakeAuthRepository(
        private val logoutRemoteBlock: suspend () -> Result<Unit>
    ) : AuthRepository {
        constructor(logoutRemoteResult: Result<Unit>) : this({ logoutRemoteResult })

        var logoutRemoteCalls: Int = 0

        override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
        override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
        override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used")

        override suspend fun logoutRemote(): Result<Unit> {
            logoutRemoteCalls += 1
            return logoutRemoteBlock()
        }

        @Deprecated("Use LogoutUseCase for user-initiated logout orchestration")
        override suspend fun logout(): Result<Unit> = logoutRemote()

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> =
            error("Not used")
    }
}
