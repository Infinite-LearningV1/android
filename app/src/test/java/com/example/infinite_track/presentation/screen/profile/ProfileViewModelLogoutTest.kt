package com.example.infinite_track.presentation.screen.profile

import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import com.example.infinite_track.domain.repository.LocalizationRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCase
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.language.GetSelectedLanguageUseCase
import com.example.infinite_track.domain.use_case.language.SetSelectedLanguageUseCase
import com.example.infinite_track.presentation.feedback.AppFeedbackEmitter
import com.example.infinite_track.presentation.feedback.AppFeedbackEvent
import com.example.infinite_track.testing.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelLogoutTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful logout emits success feedback and navigation once`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture(remoteResult = Result.success(Unit))
            val effects = fixture.collectEffects(this)

            fixture.viewModel.confirmLogout()
            advanceUntilIdle()

            assertEquals(ProfileLogoutUiState.Idle, fixture.viewModel.uiState.value)
            assertEquals(listOf(AppFeedbackEvent.LOGOUT_SUCCESS), fixture.feedback.events)
            assertEquals(listOf(ProfileEffect.NavigateToLogin), effects)
        }

    @Test
    fun `remote warning emits warning feedback and navigation once`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture(
                remoteResult = Result.failure(IllegalStateException("offline"))
            )
            val effects = fixture.collectEffects(this)

            fixture.viewModel.confirmLogout()
            advanceUntilIdle()

            assertEquals(ProfileLogoutUiState.Idle, fixture.viewModel.uiState.value)
            assertEquals(listOf(AppFeedbackEvent.LOGOUT_REMOTE_WARNING), fixture.feedback.events)
            assertEquals(listOf(ProfileEffect.NavigateToLogin), effects)
        }

    @Test
    fun `local cleanup failure does not navigate or claim logout`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture(localFailure = IllegalStateException("cleanup failed"))
            val effects = fixture.collectEffects(this)

            fixture.viewModel.confirmLogout()
            advanceUntilIdle()

            assertTrue(fixture.viewModel.uiState.value is ProfileLogoutUiState.LocalCleanupFailure)
            assertTrue(effects.isEmpty())
            assertTrue(fixture.feedback.events.isEmpty())
        }

    @Test
    fun `retry after local cleanup failure can succeed and navigate once`() =
        runTest(mainDispatcherRule.dispatcher) {
            var localAttempts = 0
            val fixture = createFixture(
                clearRuntime = {
                    localAttempts += 1
                    if (localAttempts == 1) error("cleanup failed")
                }
            )
            val effects = fixture.collectEffects(this)

            fixture.viewModel.confirmLogout()
            advanceUntilIdle()
            assertTrue(fixture.viewModel.uiState.value is ProfileLogoutUiState.LocalCleanupFailure)

            fixture.viewModel.retryLogout()
            advanceUntilIdle()

            assertEquals(2, localAttempts)
            assertEquals(ProfileLogoutUiState.Idle, fixture.viewModel.uiState.value)
            assertEquals(listOf(AppFeedbackEvent.LOGOUT_SUCCESS), fixture.feedback.events)
            assertEquals(listOf(ProfileEffect.NavigateToLogin), effects)
        }

    @Test
    fun `duplicate submit while logout is active starts one operation`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture(remoteResult = Result.success(Unit))
            val effects = fixture.collectEffects(this)

            fixture.viewModel.confirmLogout()
            fixture.viewModel.confirmLogout()
            advanceUntilIdle()

            assertEquals(1, fixture.repository.logoutRemoteCalls)
            assertEquals(listOf(AppFeedbackEvent.LOGOUT_SUCCESS), fixture.feedback.events)
            assertEquals(listOf(ProfileEffect.NavigateToLogin), effects)
        }

    @Test
    fun `navigation effect remains pending for a delayed subscriber`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture()

            fixture.viewModel.confirmLogout()
            advanceUntilIdle()

            assertEquals(ProfileEffect.NavigateToLogin, fixture.viewModel.effects.value)
            assertEquals(
                ProfileEffect.NavigateToLogin,
                fixture.viewModel.effects.filterNotNull().first()
            )
        }

    @Test
    fun `consumed navigation effect is cleared and not replayed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val fixture = createFixture()
            fixture.viewModel.confirmLogout()
            advanceUntilIdle()
            val pendingEffect = requireNotNull(fixture.viewModel.effects.value)

            fixture.viewModel.consumeEffect(pendingEffect)

            assertNull(fixture.viewModel.effects.value)
            assertNull(fixture.viewModel.effects.first())
        }

    @Test
    fun `local cleanup recovery renders independently of profile success content`() {
        val source = File(
            requireNotNull(System.getProperty("user.dir")),
            "src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt"
        ).readText()
        val scaffoldBody = source.substring(source.indexOf("Scaffold("))
        val recoveryIndex = scaffoldBody.indexOf("R.string.profile_logout_cleanup_failure_title")
        val profileStateBranchIndex = scaffoldBody.indexOf("when (profileState)")

        assertTrue("Profile logout recovery must be rendered", recoveryIndex >= 0)
        assertTrue(
            "Profile logout recovery must render outside and before profile state branches",
            recoveryIndex < profileStateBranchIndex
        )
    }

    private fun Fixture.collectEffects(scope: TestScope): MutableList<ProfileEffect> {
        val effects = mutableListOf<ProfileEffect>()
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
            viewModel.effects.filterNotNull().collect(effects::add)
        }
        return effects
    }

    private fun createFixture(
        remoteResult: Result<Unit> = Result.success(Unit),
        localFailure: Throwable? = null,
        remoteBlock: suspend () -> Result<Unit> = { remoteResult },
        clearRuntime: suspend () -> Unit = { localFailure?.let { throw it } }
    ): Fixture {
        val repository = FakeAuthRepository(remoteBlock)
        val feedback = FakeAppFeedbackEmitter()
        val localizationRepository = FakeLocalizationRepository()
        val logoutUseCase = LogoutUseCase(
            authRepository = repository,
            clearAuthenticatedRuntimeUseCase = ClearAuthenticatedRuntimeUseCase(
                AuthRuntimeCleaner { clearRuntime() }
            ),
            sessionManager = SessionManager()
        )
        val viewModel = ProfileViewModel(
            getLoggedInUserUseCase = GetLoggedInUserUseCase(repository),
            logoutUseCase = logoutUseCase,
            getSelectedLanguageUseCase = GetSelectedLanguageUseCase(localizationRepository),
            setSelectedLanguageUseCase = SetSelectedLanguageUseCase(localizationRepository),
            appFeedbackEmitter = feedback
        )
        return Fixture(viewModel, repository, feedback)
    }

    private data class Fixture(
        val viewModel: ProfileViewModel,
        val repository: FakeAuthRepository,
        val feedback: FakeAppFeedbackEmitter
    )

    private class FakeAppFeedbackEmitter : AppFeedbackEmitter {
        val events = mutableListOf<AppFeedbackEvent>()
        override fun emit(event: AppFeedbackEvent) {
            events += event
        }
    }

    private class FakeLocalizationRepository : LocalizationRepository {
        override fun getSelectedLanguage(): Flow<String> = flowOf("en")
        override suspend fun setSelectedLanguage(language: String) = Unit
    }

    private class FakeAuthRepository(
        private val logoutRemoteBlock: suspend () -> Result<Unit>
    ) : AuthRepository {
        var logoutRemoteCalls = 0

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
