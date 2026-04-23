package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.repository.UnauthorizedSyncFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for checking if user has an active session and syncing face embedding if needed
 */
class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val generateAndSaveEmbeddingUseCase: GenerateAndSaveEmbeddingUseCase
) {
    /**
     * Invokes the session check process
     * Orchestrates the flow of data between repository and face processor
     * @return Result<UserModel> with the user data if session exists, or failure if embedding generation fails
     */
    suspend operator fun invoke(): Result<UserModel> {
        return try {
            val initialSyncResult = authRepository.syncUserProfile()
            val syncResult = if (initialSyncResult.isSuccess) {
                initialSyncResult
            } else {
                handleSyncFailure(initialSyncResult.exceptionOrNull())
            }

            if (syncResult.isFailure) {
                return syncResult
            }

            val newUserData = syncResult.getOrNull()!!

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
                        SessionBootstrapFailure.TemporaryFailure(
                            cause = embeddingResult.exceptionOrNull()
                                ?: Exception("Face embedding generation failed")
                        )
                    )
                }
            }

            syncResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            when (e) {
                is SessionBootstrapFailure.ReAuthRequired,
                is SessionBootstrapFailure.TemporaryFailure -> Result.failure(e)
                else -> Result.failure(SessionBootstrapFailure.TemporaryFailure(cause = e))
            }
        }
    }

    private suspend fun handleSyncFailure(cause: Throwable?): Result<UserModel> {
        if (!isUnauthorizedSyncFailure(cause)) {
            return Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = cause ?: Exception("Failed to sync profile data")
                )
            )
        }

        return when (val refreshResult = authRepository.refreshSession()) {
            RefreshSessionResult.Success -> {
                val retrySyncResult = authRepository.syncUserProfile()
                if (retrySyncResult.isFailure && isUnauthorizedSyncFailure(retrySyncResult.exceptionOrNull())) {
                    Result.failure(
                        SessionBootstrapFailure.ReAuthRequired(
                            RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
                        )
                    )
                } else {
                    retrySyncResult
                }
            }
            is RefreshSessionResult.ReAuthRequired -> Result.failure(
                SessionBootstrapFailure.ReAuthRequired(refreshResult)
            )
            is RefreshSessionResult.TemporaryFailure -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(message = refreshResult.reason)
            )
        }
    }

    private fun isUnauthorizedSyncFailure(cause: Throwable?): Boolean {
        return cause is UnauthorizedSyncFailure
    }
}
