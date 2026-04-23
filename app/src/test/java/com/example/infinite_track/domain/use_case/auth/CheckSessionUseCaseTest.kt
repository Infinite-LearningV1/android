package com.example.infinite_track.domain.use_case.auth

import android.content.ContextWrapper
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.repository.UnauthorizedSyncFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CheckSessionUseCaseTest {

    @Test
    fun `bootstrap does not refresh after non-auth sync failure`() = runBlocking {
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(Result.failure(Exception("network down"))),
            refreshSessionResult = RefreshSessionResult.TemporaryFailure("timeout")
        )

        val useCase = createUseCase(repository)

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.TemporaryFailure)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `bootstrap refreshes after unauthorized sync failure`() = runBlocking {
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(Result.failure(UnauthorizedSyncFailure())),
            refreshSessionResult = RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
        )

        val useCase = createUseCase(repository)

        val result = useCase()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(
            RefreshSessionResult.ReAuthRequired.InvalidOrRevoked,
            (exception as SessionBootstrapFailure.ReAuthRequired).reason
        )
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `retries sync after successful refresh and returns success`() = runBlocking {
        val user = sampleUser()
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                Result.failure(UnauthorizedSyncFailure()),
                Result.success(user)
            ),
            refreshSessionResult = RefreshSessionResult.Success,
            loggedInUser = null
        )

        val useCase = createUseCase(repository)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        assertEquals(2, repository.syncCallCount)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `returns reauth required when second sync is still unauthorized after refresh success`() = runBlocking {
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                Result.failure(UnauthorizedSyncFailure()),
                Result.failure(UnauthorizedSyncFailure())
            ),
            refreshSessionResult = RefreshSessionResult.Success,
            loggedInUser = null
        )

        val useCase = createUseCase(repository)

        val result = useCase()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SessionBootstrapFailure.ReAuthRequired)
        assertEquals(
            RefreshSessionResult.ReAuthRequired.InvalidOrRevoked,
            (exception as SessionBootstrapFailure.ReAuthRequired).reason
        )
        assertEquals(2, repository.syncCallCount)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `returns temporary bootstrap failure when embedding generation fails after successful sync`() = runBlocking {
        val currentUser = sampleUser().copy(
            photoUpdatedAt = "2025-12-01T00:00:00Z",
            faceEmbedding = null
        )
        val syncedUser = sampleUser().copy(
            photoUpdatedAt = "2026-01-01T00:00:00Z",
            photoUrl = null
        )
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(Result.success(syncedUser)),
            refreshSessionResult = RefreshSessionResult.Success,
            loggedInUser = currentUser
        )

        val useCase = createUseCase(repository)

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionBootstrapFailure.TemporaryFailure)
    }

    @Test
    fun `rethrows cancellation exception from sync`() = runBlocking {
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(),
            refreshSessionResult = RefreshSessionResult.Success,
            syncThrowable = CancellationException("cancelled")
        )
        val useCase = createUseCase(repository)

        try {
            useCase()
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    private fun createUseCase(repository: FakeAuthRepository): CheckSessionUseCase {
        return CheckSessionUseCase(
            authRepository = repository,
            generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                authRepository = repository
            )
        )
    }

    private fun sampleUser(): UserModel = UserModel(
        id = 1,
        fullName = "User",
        email = "user@example.com",
        roleName = "staff",
        positionName = "Engineer",
        programName = "Program",
        divisionName = "Division",
        nipNim = "123",
        phone = "0812",
        photoUrl = "https://example.com/photo.jpg",
        photoUpdatedAt = "2026-01-01T00:00:00Z",
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = byteArrayOf(1, 2, 3)
    )

    private class FakeAuthRepository(
        private val syncResults: MutableList<Result<UserModel>>,
        private val refreshSessionResult: RefreshSessionResult,
        private val loggedInUser: UserModel? = null,
        private val syncThrowable: Throwable? = null
    ) : AuthRepository {
        var syncCallCount: Int = 0
        var refreshCallCount: Int = 0

        override suspend fun refreshSession(): RefreshSessionResult {
            refreshCallCount += 1
            return refreshSessionResult
        }

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            throw NotImplementedError()
        }

        override suspend fun syncUserProfile(): Result<UserModel> {
            syncCallCount += 1
            syncThrowable?.let { throw it }
            return syncResults.removeFirst()
        }

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(loggedInUser)

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.success(Unit)
    }
}
