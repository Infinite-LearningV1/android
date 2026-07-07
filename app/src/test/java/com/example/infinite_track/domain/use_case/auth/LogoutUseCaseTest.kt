package com.example.infinite_track.domain.use_case.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CancellationException

class LogoutUseCaseTest {

    @Test
    fun `logout use case attempts remote logout then clears runtime`() = runTest {
        val repository = FakeAuthRepository(logoutRemoteResult = Result.failure(IllegalStateException("offline")))
        var clearRuntimeCalls = 0
        val clearRuntime = createClearRuntimeUseCase { clearRuntimeCalls += 1 }
        val useCase = LogoutUseCase(repository, clearRuntime)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, repository.logoutRemoteCalls)
        assertEquals(1, clearRuntimeCalls)
    }

    @Test
    fun `logout use case returns failure when local cleanup fails`() = runTest {
        val repository = FakeAuthRepository(logoutRemoteResult = Result.success(Unit))
        val clearRuntime = createClearRuntimeUseCase { error("geofence cleanup failed") }
        val useCase = LogoutUseCase(repository, clearRuntime)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(1, repository.logoutRemoteCalls)
    }

    @Test
    fun `logout use case rethrows cancellation when local cleanup is cancelled`() = runTest {
        val repository = FakeAuthRepository(logoutRemoteResult = Result.success(Unit))
        val clearRuntime = createClearRuntimeUseCase { throw CancellationException("cancelled") }
        val useCase = LogoutUseCase(repository, clearRuntime)

        try {
            useCase()
            fail("Expected cancellation exception")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(1, repository.logoutRemoteCalls)
    }

    private fun createClearRuntimeUseCase(onRemoveAllGeofences: () -> Unit): ClearAuthenticatedRuntimeUseCase {
        return ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleaner {
                onRemoveAllGeofences()
            }
        )
    }

    private fun createDataStore(name: String) = PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile(name, ".preferences_pb").also { it.delete() } }
    )

    private class FakeUserDao : UserDao {
        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit
        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserProfile(): UserEntity? = null
        override suspend fun clearUserProfile() = Unit
    }

    private class FakeAuthRepository(
        private val logoutRemoteResult: Result<Unit>
    ) : AuthRepository {
        var logoutRemoteCalls: Int = 0

        override suspend fun refreshSession(): Result<AuthRefreshResult> = error("Not used in this test")
        override suspend fun login(credentials: LoginCredentials): Result<UserModel> = error("Not used in this test")
        override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used in this test")

        override suspend fun logoutRemote(): Result<Unit> {
            logoutRemoteCalls += 1
            return logoutRemoteResult
        }

        @Deprecated("Use LogoutUseCase for user-initiated logout orchestration")
        override suspend fun logout(): Result<Unit> = logoutRemote()

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used in this test")
    }
}
