package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.AuthRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

sealed interface LogoutOutcome {
    data object Success : LogoutOutcome
    data object SuccessWithRemoteWarning : LogoutOutcome
    data class LocalCleanupFailed(val cause: Throwable) : LogoutOutcome
}

/**
 * User-initiated logout orchestration.
 *
 * Remote logout is best-effort and local authenticated runtime cleanup always runs.
 */
class LogoutUseCase private constructor(
    private val authRepository: AuthRepository,
    private val clearAuthenticatedRuntime: suspend () -> Unit,
    private val sessionManager: SessionManager
) {
    @Inject
    constructor(
        authRepository: AuthRepository,
        clearAuthenticatedRuntimeUseCase: ClearAuthenticatedRuntimeUseCase,
        sessionManager: SessionManager
    ) : this(authRepository, clearAuthenticatedRuntimeUseCase::invoke, sessionManager)

    suspend operator fun invoke(): LogoutOutcome {
        val logoutAttempt = sessionManager.beginIntentionalLogout()
        val remoteResult = try {
            authRepository.logoutRemote()
        } catch (e: CancellationException) {
            sessionManager.cancelIntentionalLogout(logoutAttempt)
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        (remoteResult.exceptionOrNull() as? CancellationException)?.let {
            sessionManager.cancelIntentionalLogout(logoutAttempt)
            throw it
        }

        try {
            clearAuthenticatedRuntime()
        } catch (e: CancellationException) {
            sessionManager.cancelIntentionalLogout(logoutAttempt)
            throw e
        } catch (e: Exception) {
            sessionManager.cancelIntentionalLogout(logoutAttempt)
            return LogoutOutcome.LocalCleanupFailed(e)
        }

        sessionManager.completeIntentionalLogout(logoutAttempt)

        return if (remoteResult.isSuccess) {
            LogoutOutcome.Success
        } else {
            LogoutOutcome.SuccessWithRemoteWarning
        }
    }
}
