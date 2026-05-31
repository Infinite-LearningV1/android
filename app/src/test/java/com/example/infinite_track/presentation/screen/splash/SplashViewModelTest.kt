package com.example.infinite_track.presentation.screen.splash

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.GenerateAndSaveEmbeddingUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore("Requires Android Main looper in JVM; bootstrap behavior is covered by CheckSessionUseCaseTest.")
class SplashViewModelTest {
    @Test
    fun `navigates to home when bootstrap succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository(syncResult = ProfileSyncResult.Success(sampleUser()))
            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.NavigateToHome, viewModel.navigationState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shows temporary failure and preserves local session when bootstrap is unavailable`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResult = ProfileSyncResult.TemporaryFailure(Exception("offline"))
            )
            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.TemporaryFailure, viewModel.navigationState.value)
            assertEquals(false, repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(repository: FakeAuthRepository): SplashViewModel {
        val userPreference = createUserPreference()
        val sessionManager = SessionManager()
        return SplashViewModel(
            checkSessionUseCase = CheckSessionUseCase(
                authRepository = repository,
                generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                    faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                    authRepository = repository
                ),
                userPreference = userPreference,
                sessionManager = sessionManager
            ),
            logoutUseCase = LogoutUseCase(repository),
            context = ContextWrapper(null)
        )
    }

    private fun createUserPreference(): UserPreference {
        val testFile = File.createTempFile("splash", ".preferences_pb").also { it.delete() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
        return UserPreference(dataStore)
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
        private val syncResult: ProfileSyncResult,
        private val loggedInUser: UserModel? = null
    ) : AuthRepository {
        var logoutCalled: Boolean = false

        override suspend fun refreshSession(): Result<AuthRefreshResult> = Result.success(
            AuthRefreshResult("new-access", "new-refresh", "1")
        )

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            throw NotImplementedError()
        }

        override suspend fun syncUserProfile(): ProfileSyncResult = syncResult

        override suspend fun logout(): Result<Unit> {
            logoutCalled = true
            return Result.success(Unit)
        }

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(loggedInUser)

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.success(Unit)
    }
}
