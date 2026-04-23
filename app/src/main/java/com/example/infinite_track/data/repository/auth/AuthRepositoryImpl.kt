package com.example.infinite_track.data.repository.auth

import android.util.Log
import com.example.infinite_track.data.mapper.auth.toDomain
import com.example.infinite_track.data.mapper.auth.toEntity
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.repository.UnauthorizedSyncFailure
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImpl @Inject constructor(
    private val userPreference: UserPreference,
    private val apiService: ApiService,
    private val authSessionApiService: AuthSessionApiService,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun refreshSession(): RefreshSessionResult {
        return try {
            val existingRefreshToken = userPreference.getRefreshToken().first()
            if (existingRefreshToken.isBlank()) {
                return RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
            }

            val response = authSessionApiService.refreshSession(
                RefreshSessionRequest(refreshToken = existingRefreshToken)
            )

            if (response.data.token.isBlank() || response.data.id <= 0) {
                return RefreshSessionResult.TemporaryFailure("Invalid refresh session payload")
            }

            val refreshTokenToStore = response.data.refreshToken?.takeIf { it.isNotBlank() } ?: existingRefreshToken

            userPreference.saveSession(
                token = response.data.token,
                userId = response.data.id.toString(),
                refreshToken = refreshTokenToStore
            )
            RefreshSessionResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            classifyRefreshHttpError(e)
        } catch (e: IOException) {
            RefreshSessionResult.TemporaryFailure(e.message)
        }
    }


    /**
     * Login a user with provided credentials
     * @param loginRequest Login credentials
     * @return Result containing User domain model
     */
    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        return try {
            val response = apiService.login(loginRequest)
            val refreshToken = response.data.refreshToken?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Login response missing usable refresh token"))

            // Save token and user ID to DataStore
            userPreference.saveSession(
                token = response.data.token,
                userId = response.data.id.toString(),
                refreshToken = refreshToken
            )

            // Convert network response to domain model
            val user = response.data.toDomain()

            // Save user data to Room database
            val userEntity = user.toEntity(userDao.getUserProfile()?.faceEmbedding)
            userDao.insertOrUpdateUserProfile(userEntity)

            Result.success(user)
        } catch (e: HttpException) {
            val jsonInString = e.response()?.errorBody()?.string()
            val errorBody = Gson().fromJson(jsonInString, ErrorResponse::class.java)
            val errorMessage = errorBody?.message ?: "Unknown error from server"
            Log.e("AuthRepositoryImpl", "HTTP Error: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Log.e("AuthRepositoryImpl", "Network Error", e)
            Result.failure(Exception("Network error, please check your internet connection."))
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Unknown Error", e)
            Result.failure(e)
        }
    }

    /**
     * Sync user profile from server
     * @return Result containing User domain model
     */
    override suspend fun syncUserProfile(): Result<UserModel> {
        return try {
            // Check if user is logged in by getting token from DataStore
            val token = userPreference.getAuthToken().first()

            if (token.isEmpty()) {
                return Result.failure(Exception("No active session found"))
            }

            // User is logged in, fetch profile from API
            val response = apiService.getUserProfile()

            // Convert to domain model
            val user = response.data.toDomain()

            // Save to Room database
            val userEntity = user.toEntity(userDao.getUserProfile()?.faceEmbedding)
            userDao.insertOrUpdateUserProfile(userEntity)

            Result.success(user)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Result.failure(UnauthorizedSyncFailure())
            } else {
                Result.failure(Exception("Failed to sync profile data"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error, please check your internet connection."))
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Unknown Error during sync", e)
            Result.failure(e)
        }
    }

    /**
     * Logout the current user
     * @return Result indicating success or failure
     */
    override suspend fun logout(): Result<Unit> {
        return try {
            try {
                // Try to call logout API endpoint
                authSessionApiService.logout()
                safeLogDebug("Server logout successful")
            } catch (e: Exception) {
                // Log the error but continue with local logout
                safeLogError("Server logout failed, proceeding with local logout", e)
            } finally {
                // Always clear local data, regardless of API call result
                userPreference.clearAuthData()
                userDao.clearUserProfile()
                safeLogDebug("Local data cleared successfully")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // This would only happen if clearing local data fails
            safeLogError("Critical error during logout", e)
            Result.failure(e)
        }
    }

    /**
     * Get the currently logged in user as a Flow
     * @return Flow of UserModel that emits when the user data changes
     */
    override fun getLoggedInUser(): Flow<UserModel?> {
        return userPreference.getUserId()
            .flatMapLatest {
                userDao.getUserProfileFlow()
                    .map { userEntity ->
                        userEntity?.toDomain()
                    }
            }
    }

    /**
     * Save face embedding for a user
     * @param userId The ID of the user
     * @param embedding The face embedding as ByteArray
     * @return Result indicating success or failure
     */
    override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> {
        return try {
            // Get current user data from Room database
            val currentUser = userDao.getUserProfile()

            if (currentUser != null && currentUser.id == userId) {
                // Create updated entity with the same data but new embedding
                val updatedEntity = currentUser.copy(faceEmbedding = embedding)

                // Save updated entity back to database
                userDao.insertOrUpdateUserProfile(updatedEntity)

                Log.d("AuthRepositoryImpl", "Face embedding saved successfully for user $userId")
                Result.success(Unit)
            } else {
                Log.e(
                    "AuthRepositoryImpl",
                    "Cannot save face embedding: User $userId not found in local database"
                )
                Result.failure(Exception("User not found in local database"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error saving face embedding", e)
            Result.failure(e)
        }
    }

    private fun safeLogDebug(message: String) {
        runCatching { Log.d("AuthRepositoryImpl", message) }
    }

    private fun safeLogError(message: String, throwable: Throwable) {
        runCatching { Log.e("AuthRepositoryImpl", message, throwable) }
    }

    private fun classifyRefreshHttpError(httpException: HttpException): RefreshSessionResult {
        val code = httpException.code()
        val errorBody = parseHttpErrorBodySafely(httpException)
        val normalizedCode = errorBody?.code?.uppercase(Locale.ROOT)

        return when {
            normalizedCode == "INACTIVITY_TIMEOUT_48H" -> {
                RefreshSessionResult.ReAuthRequired.InactivityExceeded
            }

            code == 401 || code == 403 || normalizedCode == "INVALID_REFRESH_TOKEN" || normalizedCode == "REFRESH_TOKEN_REVOKED" -> {
                RefreshSessionResult.ReAuthRequired.InvalidOrRevoked
            }

            else -> {
                RefreshSessionResult.TemporaryFailure(errorBody?.message ?: "HTTP $code")
            }
        }
    }

    private fun parseHttpErrorBodySafely(httpException: HttpException): ErrorResponse? {
        return try {
            val rawBody = httpException.response()?.errorBody()?.string().orEmpty()
            if (rawBody.isBlank()) {
                null
            } else {
                Gson().fromJson(rawBody, ErrorResponse::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }
}