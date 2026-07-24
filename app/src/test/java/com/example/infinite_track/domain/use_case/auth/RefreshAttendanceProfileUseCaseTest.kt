package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RefreshAttendanceProfileUseCaseTest {

    @Test
    fun `refresh delegates to server profile sync and preserves typed success`() = runTest {
        val user = sampleUser()
        val repository = FakeAuthRepository(ProfileSyncResult.Success(user))

        val result = RefreshAttendanceProfileUseCase(repository)()

        assertEquals(1, repository.syncCalls)
        assertEquals(ProfileSyncResult.Success(user), result)
    }

    @Test
    fun `refresh preserves typed temporary and unauthorized failures`() = runTest {
        val cause = IllegalStateException("offline")
        val failures = listOf(
            ProfileSyncResult.TemporaryFailure(cause = cause, message = "offline"),
            ProfileSyncResult.Unauthorized()
        )

        failures.forEach { failure ->
            val repository = FakeAuthRepository(failure)

            val result = RefreshAttendanceProfileUseCase(repository)()

            assertEquals(1, repository.syncCalls)
            assertSame(failure, result)
        }
    }

    private class FakeAuthRepository(
        private val syncResult: ProfileSyncResult
    ) : AuthRepository {
        var syncCalls = 0

        override suspend fun syncUserProfile(): ProfileSyncResult {
            syncCalls += 1
            return syncResult
        }

        override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used")
        override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used")
        override suspend fun logout(): Result<Unit> = error("Not used")
        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
        override suspend fun saveFaceEmbedding(
            userId: Int,
            embedding: ByteArray
        ): Result<Unit> = error("Not used")
    }

    private fun sampleUser() = UserModel(
        id = 7,
        fullName = "Ada",
        email = "ada@example.com",
        roleName = "Employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "007",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = -0.89,
        longitude = 119.87,
        radius = 100,
        locationDescription = "Rumah",
        locationCategoryName = "WFH"
    )
}
