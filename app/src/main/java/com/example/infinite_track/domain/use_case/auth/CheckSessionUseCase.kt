package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.repository.RefreshSessionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val generateAndSaveEmbeddingUseCase: GenerateAndSaveEmbeddingUseCase
) {
    suspend operator fun invoke(): Result<UserModel> {
        return try {
            val syncResult = resolveSyncResult(authRepository.syncUserProfile())

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

    private suspend fun resolveSyncResult(syncResult: ProfileSyncResult): Result<UserModel> {
        return when (syncResult) {
            is ProfileSyncResult.Success -> Result.success(syncResult.user)
            ProfileSyncResult.Unauthorized -> handleUnauthorizedSync()
            is ProfileSyncResult.TemporaryFailure -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = syncResult.cause,
                    message = syncResult.message
                )
            )
        }
    }

    private suspend fun handleUnauthorizedSync(): Result<UserModel> {
        return when (val refreshResult = authRepository.refreshSession()) {
            RefreshSessionResult.Success -> {
                when (val retrySyncResult = authRepository.syncUserProfile()) {
                    is ProfileSyncResult.Success -> Result.success(retrySyncResult.user)
                    ProfileSyncResult.Unauthorized -> Result.failure(
                        SessionBootstrapFailure.ReAuthRequired(
                            RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
                        )
                    )
                    is ProfileSyncResult.TemporaryFailure -> Result.failure(
                        SessionBootstrapFailure.TemporaryFailure(
                            cause = retrySyncResult.cause,
                            message = retrySyncResult.message
                        )
                    )
                }
            }
            is RefreshSessionResult.ReAuthRequired -> Result.failure(
                SessionBootstrapFailure.ReAuthRequired(refreshResult)
            )
            is RefreshSessionResult.TemporaryFailure -> Result.failure(
                SessionBootstrapFailure.TemporaryFailure(
                    cause = refreshResult.cause,
                    message = refreshResult.reason
                )
            )
        }
    }
}
