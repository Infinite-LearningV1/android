package com.example.infinite_track.domain.repository

import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.auth.UserModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 * This is in the domain layer and does not depend on any implementation details
 */
interface AuthRepository {
    /**
     * Login a user with credentials
     * @param loginRequest The login credentials
     * @return Result containing User domain model if successful
     */
    suspend fun login(loginRequest: LoginRequest): Result<UserModel>

    /**
     * Sync user profile from the server
     * @return Result containing User domain model if successful
     */
    suspend fun syncUserProfile(): Result<UserModel>

    /**
     * Logout the current user
     * @return Result indicating success or failure
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get the currently logged in user as a Flow
     * @return Flow of UserModel that emits when the user data changes
     */
    fun getLoggedInUser(): Flow<UserModel?>

    /**
     * Save face embedding for a user
     * @param userId The ID of the user
     * @param embedding The face embedding as ByteArray
     * @return Result indicating success or failure
     */
    suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit>
}
