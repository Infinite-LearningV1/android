package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.flow.first

sealed class ForegroundSessionValidationResult {
    data object Skipped : ForegroundSessionValidationResult()
    data object Valid : ForegroundSessionValidationResult()
    data class ReauthRequired(val reason: SessionManager.ReauthReason) : ForegroundSessionValidationResult()
    data class TemporaryFailure(val message: String?, val cause: Throwable?) : ForegroundSessionValidationResult()
}

open class ValidateForegroundSessionUseCase(
    private val authRepository: AuthRepository,
    private val userPreference: UserPreference,
    private val sessionManager: SessionManager
) {
    open suspend operator fun invoke(): ForegroundSessionValidationResult {
        existingForcedReauthResult()?.let { return it }

        val accessToken = userPreference.getAuthToken().first()
        val refreshToken = userPreference.getRefreshToken().first()

        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return ForegroundSessionValidationResult.Skipped
        }

        if (sessionManager.isBootstrapSessionInProgress) {
            return ForegroundSessionValidationResult.Skipped
        }

        if (isProfileFresh()) {
            return ForegroundSessionValidationResult.Valid
        }

        return when (val syncResult = authRepository.syncUserProfile()) {
            is ProfileSyncResult.Success -> {
                userPreference.saveLastProfileSyncAt(System.currentTimeMillis())
                ForegroundSessionValidationResult.Valid
            }
            is ProfileSyncResult.TemporaryFailure -> ForegroundSessionValidationResult.TemporaryFailure(
                message = syncResult.message,
                cause = syncResult.cause
            )
            is ProfileSyncResult.Unauthorized -> handleUnauthorized(syncResult.reason)
        }
    }

    private suspend fun isProfileFresh(): Boolean {
        val lastProfileSyncAt = userPreference.getLastProfileSyncAt().first()
        return lastProfileSyncAt > 0L &&
            (System.currentTimeMillis() - lastProfileSyncAt) < AuthRuntimePolicy.SHARED_TTL_MILLIS
    }

    private fun handleUnauthorized(
        reason: AuthRefreshFailureReason?
    ): ForegroundSessionValidationResult {
        existingForcedReauthResult()?.let { return it }

        val reauthReason = reason?.toTerminalReauthReason()
        if (reauthReason != null) {
            return reauthRequired(reauthReason)
        }

        return ForegroundSessionValidationResult.TemporaryFailure(
            message = "Foreground profile sync remained unauthorized after interceptor handling",
            cause = null
        )
    }

    private fun reauthRequired(
        reason: SessionManager.ReauthReason
    ): ForegroundSessionValidationResult.ReauthRequired {
        existingForcedReauthResult()?.let { return it }

        return ForegroundSessionValidationResult.ReauthRequired(reason)
    }

    private fun existingForcedReauthResult(): ForegroundSessionValidationResult.ReauthRequired? {
        val existingReason = sessionManager.reauthReason.value
        return if (sessionManager.sessionExpired.value && existingReason != null) {
            ForegroundSessionValidationResult.ReauthRequired(existingReason)
        } else {
            null
        }
    }

    private fun AuthRefreshFailureReason.toTerminalReauthReason(): SessionManager.ReauthReason? {
        return when (this) {
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> SessionManager.ReauthReason.INACTIVITY_EXPIRED
            AuthRefreshFailureReason.REFRESH_INVALID,
            AuthRefreshFailureReason.MISSING_REFRESH_TOKEN -> SessionManager.ReauthReason.REFRESH_INVALID
            AuthRefreshFailureReason.REFRESH_REVOKED -> SessionManager.ReauthReason.REFRESH_REVOKED
            AuthRefreshFailureReason.ACCESS_EXPIRED,
            AuthRefreshFailureReason.TRANSPORT_ERROR,
            AuthRefreshFailureReason.INVALID_PAYLOAD,
            AuthRefreshFailureReason.UNKNOWN -> null
        }
    }
}
