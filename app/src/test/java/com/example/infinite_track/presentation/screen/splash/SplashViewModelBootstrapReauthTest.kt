package com.example.infinite_track.presentation.screen.splash

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.local.room.UserEntity
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.CheckSessionUseCase
import com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCase
import com.example.infinite_track.domain.use_case.auth.GenerateAndSaveEmbeddingUseCase
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelBootstrapReauthTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `bootstrap reauth clears local runtime without opening global session expired state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val sessionManager = SessionManager()
            val userPreference = UserPreference(createDataStore("splash_bootstrap_user"))
            val attendancePreference = AttendancePreference(createDataStore("splash_bootstrap_attendance"))
            val todayStatusPreference = TodayStatusPreference(createDataStore("splash_bootstrap_today"), Gson())
            val userDao = FakeUserDao()
            var removeAllGeofencesCalls = 0
            val repository = TerminalRefreshRepository()

            userPreference.saveSession(
                token = "expired-access-redacted",
                userId = "147",
                refreshToken = "refresh-token-redacted",
                lastRefreshAt = 1L
            )
            attendancePreference.saveActiveAttendanceId(99)

            val viewModel = SplashViewModel(
                checkSessionUseCase = CheckSessionUseCase(
                    authRepository = repository,
                    generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                        faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                        authRepository = repository
                    ),
                    userPreference = userPreference,
                    sessionManager = sessionManager
                ),
                clearAuthenticatedRuntimeUseCase = ClearAuthenticatedRuntimeUseCase(
                    userPreference = userPreference,
                    userDao = userDao,
                    attendancePreference = attendancePreference,
                    todayStatusPreference = todayStatusPreference,
                    removeAllGeofences = { removeAllGeofencesCalls += 1 }
                )
            )

            val terminalState = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) {
                    while (viewModel.navigationState.value == SplashNavigationState.Loading) {
                        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                        delay(10)
                    }
                    viewModel.navigationState.value
                }
            }

            assertEquals(SplashNavigationState.NavigateToLogin, terminalState)
            assertEquals("", userPreference.getAuthToken().first())
            assertEquals("", userPreference.getRefreshToken().first())
            assertEquals(0L, userPreference.getLastRefreshAt().first())
            assertEquals(null, attendancePreference.getActiveAttendanceId().first())
            assertEquals(1, removeAllGeofencesCalls)
            assertEquals(1, repository.refreshCalls)
            assertFalse(repository.logoutRemoteCalled)
            assertFalse(sessionManager.sessionExpired.value)
            assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
        }

    private fun createDataStore(name: String) = PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile(name, ".preferences_pb").also { it.delete() } }
    )

    @Test
    fun `bootstrap reauth still navigates to login when local cleanup fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val sessionManager = SessionManager()
            val repository = TerminalRefreshRepository()
            val viewModel = SplashViewModel(
                checkSessionUseCase = CheckSessionUseCase(
                    authRepository = repository,
                    generateAndSaveEmbeddingUseCase = GenerateAndSaveEmbeddingUseCase(
                        faceProcessor = FaceProcessor(appContext = ContextWrapper(null)),
                        authRepository = repository
                    ),
                    userPreference = UserPreference(createDataStore("splash_bootstrap_user_fail")),
                    sessionManager = sessionManager
                ),
                clearAuthenticatedRuntimeUseCase = ClearAuthenticatedRuntimeUseCase(
                    userPreference = UserPreference(createDataStore("splash_bootstrap_user_fail_cleanup")),
                    userDao = FakeUserDao(),
                    attendancePreference = AttendancePreference(createDataStore("splash_bootstrap_attendance_fail")),
                    todayStatusPreference = TodayStatusPreference(createDataStore("splash_bootstrap_today_fail"), Gson()),
                    removeAllGeofences = { error("cleanup failed") }
                )
            )

            val terminalState = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) {
                    while (viewModel.navigationState.value == SplashNavigationState.Loading) {
                        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                        delay(10)
                    }
                    viewModel.navigationState.value
                }
            }

            assertEquals(SplashNavigationState.NavigateToLogin, terminalState)
            assertFalse(sessionManager.sessionExpired.value)
            assertEquals(SessionManager.ReauthReason.REFRESH_REVOKED, sessionManager.reauthReason.value)
            assertFalse(repository.logoutRemoteCalled)
        }

    private class FakeUserDao : UserDao {
        override suspend fun insertOrUpdateUserProfile(userEntity: UserEntity) = Unit
        override fun getUserProfileFlow(): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserProfile(): UserEntity? = null
        override suspend fun clearUserProfile() = Unit
    }

    private class TerminalRefreshRepository : AuthRepository {
        var refreshCalls: Int = 0
        var logoutRemoteCalled: Boolean = false

        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            refreshCalls += 1
            return Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = AuthRefreshFailureReason.REFRESH_REVOKED,
                    message = "refresh revoked"
                )
            )
        }

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> = error("Not used in this test")
        override suspend fun syncUserProfile(): ProfileSyncResult = error("Not used in this test")
        override suspend fun logoutRemote(): Result<Unit> {
            logoutRemoteCalled = true
            return Result.success(Unit)
        }

        @Deprecated("Use LogoutUseCase for user-initiated logout orchestration")
        override suspend fun logout(): Result<Unit> = logoutRemote()
        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = error("Not used in this test")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher: TestDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
