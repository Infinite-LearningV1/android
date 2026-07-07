package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.face.FaceProcessor
import com.example.infinite_track.domain.model.auth.LoginCredentials
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for handling user login and initial face embedding generation
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val faceProcessor: FaceProcessor
) {
    /**
     * Invokes the login process with email and password
     * Orchestrates the flow between repository and face processor
     * @return Result<User> with the user data if successful
     */
    suspend operator fun invoke(email: String, password: String): Result<UserModel> {
        // First, perform the login via repository
        val credentials = LoginCredentials(email, password)
        val loginResult = authRepository.login(credentials)

        // If login is successful, process the user's photo to generate face embedding
        if (loginResult.isSuccess) {
            val userData = loginResult.getOrNull()!!

            android.util.Log.d("LoginUseCase", "Login successful for user: ${userData.email}")
            android.util.Log.d("LoginUseCase", "Photo URL: ${userData.photoUrl}")

            // Only process if photo URL is available
            if (userData.photoUrl?.isNotBlank() == true) {
                // Generate embedding from user's photo
                userData.photoUrl?.let { photoUrl ->
                    android.util.Log.d("LoginUseCase", "Starting face embedding generation for URL: $photoUrl")

                    try {
                        // Generate embedding from user's photo with timeout
                        val embeddingResult = kotlinx.coroutines.withTimeout(45000L) { // 45 seconds timeout
                            faceProcessor.generateEmbedding(photoUrl)
                        }

                        // If embedding generation is successful, save it to the repository
                        if (embeddingResult.isSuccess) {
                            val embedding = embeddingResult.getOrNull()!!
                            android.util.Log.d("LoginUseCase", "Face embedding generated successfully, size: ${embedding.size}")

                            try {
                                authRepository.saveFaceEmbedding(userData.id, embedding)
                                android.util.Log.d("LoginUseCase", "Face embedding saved to database successfully")
                            } catch (e: Exception) {
                                android.util.Log.e("LoginUseCase", "Failed to save face embedding to database", e)
                                // Don't fail login if saving embedding fails
                            }
                        } else {
                            val error = embeddingResult.exceptionOrNull()
                            android.util.Log.w("LoginUseCase", "Face embedding generation failed: ${error?.message}")
                            // Don't fail login if embedding generation fails
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        android.util.Log.w("LoginUseCase", "Face embedding generation timed out after 45 seconds. Login will continue without embedding.")
                    } catch (e: Exception) {
                        android.util.Log.w("LoginUseCase", "Unexpected error during face embedding generation: ${e.message}")
                        // Don't fail login if embedding generation fails
                    }
                }
            } else {
                android.util.Log.w("LoginUseCase", "Photo URL is null or blank, skipping face embedding generation")
            }
        } else {
            android.util.Log.e("LoginUseCase", "Login failed: ${loginResult.exceptionOrNull()?.message}")
        }

        // Return the original login result (success even if embedding fails)
        return loginResult
    }
}
