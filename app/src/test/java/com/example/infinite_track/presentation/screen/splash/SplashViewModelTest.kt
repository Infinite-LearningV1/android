package com.example.infinite_track.presentation.screen.splash

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCase
import com.example.infinite_track.domain.use_case.auth.GenerateAndSaveEmbeddingUseCase
import com.google.gson.Gson
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
import org.junit.Ignore
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@Ignore("Requires Android Main looper in JVM; bootstrap behavior is covered by CheckSessionUseCaseTest and SplashBootstrapGateTest.")
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

    private fun createViewModel(
        repository: FakeAuthRepository,
        userPreference: UserPreference = createUserPreference(),
        sessionManager: SessionManager = SessionManager()
    ): SplashViewModel {
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
            clearAuthenticatedRuntimeUseCase = createClearRuntimeUseCase()
        )
    }

    private fun createUserPreference(): UserPreference {
        val testFile = File.createTempFile("splash", ".preferences_pb").also { it.delete() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
        return UserPreference(dataStore)
    }

    private fun createClearRuntimeUseCase(): ClearAuthenticatedRuntimeUseCase {
        return ClearAuthenticatedRuntimeUseCase(
            AuthRuntimeCleaner {}
        )
    }

    private fun createDataStore(name: String) = PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile(name, ".preferences_pb").also { it.delete() } }
    )

    private fun sampleUser(): UserModel = UserModel(
        id = 900_002,
        fullName = "SENTINEL_NAME_BETA",
        email = "SENTINEL_EMAIL_BETA",
        roleName = "staff",
        positionName = "Engineer",
        programName = "Program",
        divisionName = "Division",
        nipNim = "SENTINEL_ID_BETA",
        phone = "SENTINEL_PHONE_BETA",
        photoUrl = "https://example.invalid/sentinel-photo.png",
        photoUpdatedAt = "2099-12-31T23:59:59Z",
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null,
        faceEmbedding = byteArrayOf(1, 2, 3)
    )

    private class FakeUserDao : UserDao {
        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit
        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserProfile(): UserEntity? = null
        override suspend fun clearUserProfile() = Unit
    }

    private class FakeAuthRepository(
        private val syncResult: ProfileSyncResult,
        private val loggedInUser: UserModel? = null
    ) : AuthRepository {
        var logoutCalled: Boolean = false

        override suspend fun refreshSession(): Result<AuthRefreshResult> = Result.success(
            AuthRefreshResult("new-access", "new-refresh", "1")
        )

        override suspend fun login(credentials: LoginCredentials): Result<UserModel> {
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
