package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val generateAndSaveEmbeddingUseCase: GenerateAndSaveEmbeddingUseCase,
    private val userPreference: UserPreference,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<UserModel> {
        sessionManager.beginBootstrapSession()
        return try {
            val bootstrapRefreshResult = validateRefreshSessionIfAvailable()
            if (bootstrapRefreshResult != null) {
                return bootstrapRefreshResult
            }

            val syncResult = resolveSyncResult(authRepository.syncUserProfileForBootstrap())

            if (syncResult.isFailure) {
                return syncResult
            }

            val newUserData = syncResult.getOrThrow()
            val currentUser = authRepository.getLoggedInUser().first()

            if (currentUser != null &&
                (newUserData.photoUpdatedAt != currentUser.photoUpdatedAt ||
                    currentUser.faceEmbedding == null)
            ) {
                val embeddingResult = generateAndSaveEmbeddingUseCase(
                    userId = newUserData.id,
                    photoUrl = newUserData.photoUrl
                )

                if (embeddingResult.isFailure) {
                    return Result.failure(
                        embeddingResult.exceptionOrNull()
                            ?: Exception("Face embedding generation failed")
                    )
                }
            }

            syncResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionBootstrapFailure) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            sessionManager.endBootstrapSession()
        }
    }

    private suspend fun validateRefreshSessionIfAvailable(): Result<UserModel>? {
        val token = userPreference.getAuthToken().first()
        val refreshToken = userPreference.getRefreshToken().first()
        if (token.isBlank() || refreshToken.isBlank()) {
            return null
        }

        val refreshResult = authRepository.refreshSession()
        if (refreshResult.isSuccess) {
            return null
        }

        return handleFailedRefresh(refreshResult)
    }

    private suspend fun resolveSyncResult(
        syncResult: ProfileSyncResult
    ): Result<UserModel> {
        return when (syncResult) {
            is ProfileSyncResult.Success -> Result.success(syncResult.user)
            is ProfileSyncResult.Unauthorized -> handleUnauthorizedSync(syncResult.reason)
            is ProfileSyncResult.TemporaryFailure -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = syncResult.cause,
                    message = syncResult.message
                )
            )
        }
    }

    private suspend fun handleUnauthorizedSync(
        initialReason: AuthRefreshFailureReason?
    ): Result<UserModel> {
        val refreshResult = authRepository.refreshSession()
        if (refreshResult.isSuccess) {
            return when (val retrySyncResult = authRepository.syncUserProfileForBootstrap()) {
                is ProfileSyncResult.Success -> Result.success(retrySyncResult.user)
                is ProfileSyncResult.Unauthorized -> {
                    val reason = reauthReasonFor(
                        retrySyncResult.reason
                            ?: initialReason
                            ?: AuthRefreshFailureReason.REFRESH_INVALID
                    )
                    sessionManager.recordBootstrapReauth(reason)
                    Result.failure(SessionBootstrapFailure.ReAuthRequired(reason))
                }
                is ProfileSyncResult.TemporaryFailure -> Result.failure(
                    SessionBootstrapFailure.TemporaryFailure(
                        cause = retrySyncResult.cause,
                        message = retrySyncResult.message
                    )
                )
            }
        }

        return handleFailedRefresh(refreshResult, initialReason)
    }

    private fun handleFailedRefresh(
        refreshResult: Result<*>,
        initialReason: AuthRefreshFailureReason? = null
    ): Result<UserModel> {
        val refreshException = refreshResult.exceptionOrNull() as? AuthRefreshException
        return when (refreshException?.kind) {
            AuthRefreshFailureKind.NON_REFRESHABLE -> {
                val reason = reauthReasonFor(preferredUnauthorizedReason(refreshException.reason, initialReason))
                sessionManager.recordBootstrapReauth(reason)
                Result.failure(SessionBootstrapFailure.ReAuthRequired(reason))
            }

            AuthRefreshFailureKind.TRANSPORT -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = refreshException,
                    message = refreshException.message
                )
            )

            AuthRefreshFailureKind.TRANSIENT,
            null -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = refreshException,
                    message = refreshException?.message
                )
            )
        }
    }

    private fun preferredUnauthorizedReason(
        refreshFailureReason: AuthRefreshFailureReason,
        initialReason: AuthRefreshFailureReason?
    ): AuthRefreshFailureReason {
        return when {
            initialReason == null -> refreshFailureReason
            initialReason.isTerminalBootstrapReason() && refreshFailureReason.isGenericRefreshFailureReason() -> initialReason
            else -> refreshFailureReason
        }
    }

    private fun AuthRefreshFailureReason.isTerminalBootstrapReason(): Boolean {
        return this == AuthRefreshFailureReason.INACTIVITY_EXPIRED ||
            this == AuthRefreshFailureReason.REFRESH_REVOKED
    }

    private fun AuthRefreshFailureReason.isGenericRefreshFailureReason(): Boolean {
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
