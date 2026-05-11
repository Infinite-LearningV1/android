package com.example.infinite_track.presentation.screen.splash

import android.content.ContextWrapper
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.GenerateAndSaveEmbeddingUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @Test
    fun `navigates to login and clears local session on non refreshable bootstrap failure`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResults = mutableListOf(ProfileSyncResult.Unauthorized),
                refreshSessionResult = RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
            )

            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.NavigateToLogin, viewModel.navigationState.value)
            assertTrue(repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shows temporary failure state and preserves local session on temporary bootstrap failure`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResults = mutableListOf(
                    ProfileSyncResult.TemporaryFailure(Exception("network"))
                ),
                refreshSessionResult = RefreshSessionResult.TemporaryFailure("timeout")
            )

            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.TemporaryFailure, viewModel.navigationState.value)
            assertFalse(repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `post refresh sync temporary failure does not navigate to login and stays temporary failure`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResults = mutableListOf(
                    ProfileSyncResult.Unauthorized,
                    ProfileSyncResult.TemporaryFailure(Exception("post-refresh sync failed"))
                ),
                refreshSessionResult = RefreshSessionResult.Success
            )

            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.TemporaryFailure, viewModel.navigationState.value)
            assertFalse(repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `navigates to login and clears session when second sync stays unauthorized after refresh success`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResults = mutableListOf(
                    ProfileSyncResult.Unauthorized,
                    ProfileSyncResult.Unauthorized
                ),
                refreshSessionResult = RefreshSessionResult.Success
            )

            val viewModel = createViewModel(repository)

            advanceUntilIdle()

            assertEquals(SplashNavigationState.NavigateToLogin, viewModel.navigationState.value)
            assertTrue(repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `retry from temporary failure rechecks session and can navigate to home`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val repository = FakeAuthRepository(
                syncResults = mutableListOf(
                    ProfileSyncResult.TemporaryFailure(Exception("network")),
                    ProfileSyncResult.Success(sampleUser())
                ),
                refreshSessionResult = RefreshSessionResult.TemporaryFailure("timeout")
            )

            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            assertEquals(SplashNavigationState.TemporaryFailure, viewModel.navigationState.value)

            viewModel.retrySessionCheck()
            advanceUntilIdle()

            assertEquals(SplashNavigationState.NavigateToHome, viewModel.navigationState.value)
            assertFalse(repository.logoutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(repository: FakeAuthRepository): SplashViewModel {
        return SplashViewModel(
            checkSessionUseCase = CheckSessionUseCase(
                authRepository = repository,
                generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                    faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                    authRepository = repository
                )
            ),
            logoutUseCase = LogoutUseCase(repository),
            context = ContextWrapper(null)
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
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = null
    )

    private class FakeAuthRepository(
        private val syncResults: MutableList<ProfileSyncResult>,
        private val refreshSessionResult: RefreshSessionResult
    ) : AuthRepository {

        var logoutCalled: Boolean = false

        override suspend fun refreshSession(): RefreshSessionResult = refreshSessionResult

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            throw NotImplementedError()
        }

        override suspend fun syncUserProfile(): ProfileSyncResult = syncResults.removeFirst()

        override suspend fun logout(): Result<Unit> {
            logoutCalled = true
            return Result.success(Unit)
        }

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)

        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
