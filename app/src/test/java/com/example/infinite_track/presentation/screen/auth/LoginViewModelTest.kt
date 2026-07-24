package com.example.infinite_track.presentation.screen.auth

import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.presentation.feedback.AppFeedbackEmitter
import com.example.infinite_track.presentation.feedback.AppFeedbackEvent
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login success emits navigation and semantic feedback once`() =
        runTest(mainDispatcherRule.dispatcher) {
            val feedback = FakeAppFeedbackController()
            val viewModel = createViewModel(
                loginResult = Result.success(user()),
                feedback = feedback
            )
            val navigation = async { viewModel.effects.filterNotNull().first() }

            viewModel.login("user@example.com", "password")
            advanceUntilIdle()

            assertEquals(listOf(AppFeedbackEvent.LOGIN_SUCCESS), feedback.events)
            assertEquals(LoginEffect.NavigateHome, navigation.await())
            assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `login success keeps navigation pending for a delayed subscriber`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = createViewModel(loginResult = Result.success(user()))

            viewModel.login("user@example.com", "password")
            advanceUntilIdle()

            assertEquals(LoginEffect.NavigateHome, viewModel.effects.value)
        }

    @Test
    fun `acknowledged login navigation is not replayed to a later subscriber`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = createViewModel(loginResult = Result.success(user()))

            viewModel.login("user@example.com", "password")
            advanceUntilIdle()
            viewModel.consumeEffect(LoginEffect.NavigateHome)

            assertEquals(null, viewModel.effects.value)
        }

    @Test
    fun `login failure stays inline and emits no root snackbar`() =
        runTest(mainDispatcherRule.dispatcher) {
            val feedback = FakeAppFeedbackController()
            val viewModel = createViewModel(
                loginResult = Result.failure(Exception("Invalid credentials")),
                feedback = feedback
            )

            viewModel.login("user@example.com", "bad")
            advanceUntilIdle()

            assertEquals(LoginUiState.Failure("Invalid credentials"), viewModel.uiState.value)
            assertEquals(emptyList<AppFeedbackEvent>(), feedback.events)
        }

    @Test
    fun `matching failure dismissal returns login to idle`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = createViewModel(
                loginResult = Result.failure(Exception("Invalid credentials"))
            )
            viewModel.login("user@example.com", "bad")
            advanceUntilIdle()

            viewModel.dismissFailure("Invalid credentials")

            assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `stale failure dismissal does not clear newer failure`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = createViewModel(
                loginResults = listOf(
                    Result.failure(Exception("First failure")),
                    Result.failure(Exception("New failure"))
                )
            )
            viewModel.login("user@example.com", "bad")
            advanceUntilIdle()
            viewModel.login("user@example.com", "bad-again")
            advanceUntilIdle()

            viewModel.dismissFailure("First failure")

            assertEquals(LoginUiState.Failure("New failure"), viewModel.uiState.value)
        }

    @Test
    fun `duplicate submit while loading starts only one login`() =
        runTest(mainDispatcherRule.dispatcher) {
            var loginCalls = 0
            val viewModel = LoginViewModel(
                loginExecutor = LoginExecutor { _, _ ->
                    loginCalls += 1
                    Result.failure(Exception("Invalid credentials"))
                },
                sessionManager = SessionManager(),
                appFeedbackEmitter = FakeAppFeedbackController()
            )

            viewModel.login("user@example.com", "bad")
            viewModel.login("user@example.com", "bad")
            advanceUntilIdle()

            assertEquals(1, loginCalls)
        }

    private fun createViewModel(
        loginResult: Result<UserModel>,
        feedback: AppFeedbackEmitter = FakeAppFeedbackController()
    ): LoginViewModel = createViewModel(listOf(loginResult), feedback)

    private fun createViewModel(
        loginResults: List<Result<UserModel>>,
        feedback: AppFeedbackEmitter = FakeAppFeedbackController()
    ): LoginViewModel {
        val results = ArrayDeque(loginResults)
        return LoginViewModel(
            loginExecutor = LoginExecutor { _, _ -> results.removeFirst() },
            sessionManager = SessionManager(),
            appFeedbackEmitter = feedback
        )
    }

    private fun user() = UserModel(
        id = 1,
        fullName = "Test User",
        email = "user@example.com",
        roleName = "employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "TEST-1",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = null,
        longitude = null,
        radius = null,
        locationDescription = null,
        locationCategoryName = null
    )

    private class FakeAppFeedbackController : AppFeedbackEmitter {
        val events = mutableListOf<AppFeedbackEvent>()

        override fun emit(event: AppFeedbackEvent) {
            events += event
        }
    }
}
