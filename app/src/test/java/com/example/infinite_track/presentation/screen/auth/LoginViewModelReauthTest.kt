package com.example.infinite_track.presentation.screen.auth

import android.content.ContextWrapper
import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.LoginUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginViewModelReauthTest {
    @Test
    fun `maps all reauth reasons to user facing banner messages`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val sessionManager = SessionManager()
            val viewModel = createViewModel(sessionManager)

            val expectations = mapOf(
                ReauthReason.INACTIVITY_EXPIRED to "Sesi tidak aktif lebih dari 48 jam. Silakan login lagi.",
                ReauthReason.REFRESH_INVALID to "Sesi tidak valid lagi. Silakan login lagi.",
                ReauthReason.REFRESH_REVOKED to "Sesi sudah berakhir. Silakan login lagi.",
                ReauthReason.NETWORK_OFFLINE_AT_REFRESH to "Tidak dapat memvalidasi sesi karena jaringan. Coba lagi setelah online."
            )

            expectations.forEach { (reason, message) ->
                sessionManager.triggerForcedReauth(reason)
                advanceUntilIdle()

                assertEquals(message, viewModel.reauthBannerMessage.value)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `dismiss reauth banner clears session manager state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val sessionManager = SessionManager()
            val viewModel = createViewModel(sessionManager)
            sessionManager.triggerForcedReauth(ReauthReason.REFRESH_INVALID)
            advanceUntilIdle()

            viewModel.dismissReauthBanner()
            advanceUntilIdle()

            assertEquals(null, viewModel.reauthBannerMessage.value)
            assertEquals(null, sessionManager.reauthReason.value)
            assertEquals(false, sessionManager.sessionExpired.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(sessionManager: SessionManager): LoginViewModel {
        val authRepository = object : AuthRepository {
            override suspend fun refreshSession(): Result<AuthRefreshResult> = Result.failure(NotImplementedError())
            override suspend fun login(credentials: LoginCredentials): Result<UserModel> = Result.failure(NotImplementedError())
            override suspend fun syncUserProfile(): ProfileSyncResult = ProfileSyncResult.TemporaryFailure(NotImplementedError())
            override suspend fun logout(): Result<Unit> = Result.success(Unit)
            override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
            override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.success(Unit)
        }
        return LoginViewModel(
            loginUseCase = LoginUseCase(
                authRepository = authRepository,
                faceProcessor = FaceProcessor(appContext = ContextWrapper(null))
            ),
            sessionManager = sessionManager
        )
    }
}
