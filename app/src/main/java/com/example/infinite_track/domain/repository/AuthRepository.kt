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
     * Attempt to refresh the current auth session.
     *
     * This does not perform interceptor retry orchestration. It only classifies
     * refresh outcomes so higher layers can decide between re-auth and retry.
     */
    suspend fun refreshSession(): Result<AuthRefreshResult>

    /**
     * Login a user with credentials
     * @param loginRequest The login credentials
     * @return Result containing User domain model if successful
     */
    suspend fun login(loginRequest: LoginRequest): Result<UserModel>

    /**
     * Sync user profile from the server
     * @return Explicit sync outcome for success, unauthorized, or temporary failure
     */
    suspend fun syncUserProfile(): ProfileSyncResult

    /**
     * Sync user profile as part of startup bootstrap.
     * Implementations may mark the underlying request so interceptor-level forced reauth
     * handling stays scoped to the bootstrap owner instead of the global dialog path.
     */
    suspend fun syncUserProfileForBootstrap(): ProfileSyncResult = syncUserProfile()

    /**
     * Attempt to invalidate the current server-side auth session.
     *
     * This does not clear local authenticated runtime state. User-initiated logout
     * orchestration belongs in LogoutUseCase, and forced re-auth cleanup belongs in
     * ForceReauthUseCase.
     */
    @Suppress("DEPRECATION")
    suspend fun logoutRemote(): Result<Unit> = logout()

    /**
     * Logout the current user
     * @return Result indicating success or failure
     */
    @Deprecated("Use LogoutUseCase for user-initiated logout orchestration")
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
