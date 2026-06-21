package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.flow.first

sealed class ForegroundSessionValidationResult {
    data object Skipped : ForegroundSessionValidationResult()
    data object Valid : ForegroundSessionValidationResult()
    data object Refreshed : ForegroundSessionValidationResult()
    data class ReauthRequired(val reason: SessionManager.ReauthReason) : ForegroundSessionValidationResult()
    data class TemporaryFailure(val message: String?, val cause: Throwable?) : ForegroundSessionValidationResult()
}

class ValidateForegroundSessionUseCase(
    private val authRepository: AuthRepository,
    private val userPreference: UserPreference,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): ForegroundSessionValidationResult {
        val accessToken = userPreference.getAuthToken().first()
        val refreshToken = userPreference.getRefreshToken().first()

        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return ForegroundSessionValidationResult.Skipped
        }

        if (sessionManager.isBootstrapSessionInProgress) {
            return ForegroundSessionValidationResult.Skipped
        }

        return when (val syncResult = authRepository.syncUserProfile()) {
            is ProfileSyncResult.Success -> ForegroundSessionValidationResult.Valid
            is ProfileSyncResult.TemporaryFailure -> ForegroundSessionValidationResult.TemporaryFailure(
                message = syncResult.message,
                cause = syncResult.cause
            )
            is ProfileSyncResult.Unauthorized -> handleUnauthorized(syncResult.reason)
        }
    }

    private suspend fun handleUnauthorized(
        initialReason: AuthRefreshFailureReason?
    ): ForegroundSessionValidationResult {
        if (initialReason?.isTerminalReason() == true) {
            return reauthRequired(reauthReasonFor(initialReason))
        }

        val refreshResult = authRepository.refreshSession()
        if (refreshResult.isSuccess) {
            return when (val retrySync = authRepository.syncUserProfile()) {
                is ProfileSyncResult.Success -> ForegroundSessionValidationResult.Refreshed
                is ProfileSyncResult.TemporaryFailure -> ForegroundSessionValidationResult.TemporaryFailure(
                    message = retrySync.message,
                    cause = retrySync.cause
                )
                is ProfileSyncResult.Unauthorized -> reauthRequired(
                    reason = reauthReasonFor(preferredUnauthorizedReason(retrySync.reason, initialReason))
                )
            }
        }

        val refreshException = refreshResult.exceptionOrNull() as? AuthRefreshException
        return when (refreshException?.kind) {
            AuthRefreshFailureKind.NON_REFRESHABLE -> reauthRequired(
                reason = reauthReasonFor(preferredUnauthorizedReason(refreshException.reason, initialReason))
            )
            AuthRefreshFailureKind.TRANSPORT,
            AuthRefreshFailureKind.TRANSIENT,
            null -> ForegroundSessionValidationResult.TemporaryFailure(
                message = refreshException?.message,
                cause = refreshException
            )
        }
    }

    private fun reauthRequired(
        reason: SessionManager.ReauthReason
    ): ForegroundSessionValidationResult.ReauthRequired {
        sessionManager.triggerForcedReauth(reason)
        return ForegroundSessionValidationResult.ReauthRequired(reason)
    }

    private fun preferredUnauthorizedReason(
        laterReason: AuthRefreshFailureReason?,
        initialReason: AuthRefreshFailureReason?
    ): AuthRefreshFailureReason {
        val fallbackReason = laterReason ?: AuthRefreshFailureReason.REFRESH_INVALID
        return when {
            initialReason == null -> fallbackReason
            initialReason.isTerminalReason() && fallbackReason.isGenericReason() -> initialReason
            else -> fallbackReason
        }
    }

    private fun AuthRefreshFailureReason.isTerminalReason(): Boolean {
        return this == AuthRefreshFailureReason.INACTIVITY_EXPIRED ||
            this == AuthRefreshFailureReason.REFRESH_INVALID ||
            this == AuthRefreshFailureReason.REFRESH_REVOKED
    }

    private fun AuthRefreshFailureReason.isGenericReason(): Boolean {
        return this == AuthRefreshFailureReason.REFRESH_INVALID ||
            this == AuthRefreshFailureReason.UNKNOWN
    }

    private fun reauthReasonFor(reason: AuthRefreshFailureReason): SessionManager.ReauthReason {
        return when (reason) {
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> SessionManager.ReauthReason.INACTIVITY_EXPIRED
            AuthRefreshFailureReason.REFRESH_INVALID,
            AuthRefreshFailureReason.MISSING_REFRESH_TOKEN -> SessionManager.ReauthReason.REFRESH_INVALID
            AuthRefreshFailureReason.REFRESH_REVOKED -> SessionManager.ReauthReason.REFRESH_REVOKED
            else -> SessionManager.ReauthReason.UNKNOWN
        }
    }
}
