package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
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
        try {
            // First, sync user profile from the server
            val syncResult = authRepository.syncUserProfile()

            // If sync failed, return the failure immediately
            if (syncResult.isFailure) {
                return syncResult
            }

            val newUserData = syncResult.getOrNull()!!

            // Get current user data from local storage to compare photoUpdatedAt
            val currentUser = authRepository.getLoggedInUser().first()

            // If photoUpdatedAt has changed or face embedding is missing, generate new embedding
            if (currentUser != null &&
                (newUserData.photoUpdatedAt != currentUser.photoUpdatedAt ||
                        currentUser.faceEmbedding == null)
            ) {

                // Use GenerateAndSaveEmbeddingUseCase to handle face embedding generation
                val embeddingResult = generateAndSaveEmbeddingUseCase(
                    userId = newUserData.id,
                    photoUrl = newUserData.photoUrl
                )

                // If embedding generation failed, stop the process and return failure
                if (embeddingResult.isFailure) {
                    return Result.failure(
                        embeddingResult.exceptionOrNull()
                            ?: Exception("Face embedding generation failed")
                    )
                }
            }

            // If everything succeeded, return the sync result
            return syncResult

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
