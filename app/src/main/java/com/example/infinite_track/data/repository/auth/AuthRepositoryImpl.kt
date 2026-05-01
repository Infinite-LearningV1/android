package com.example.infinite_track.data.repository.auth

import android.util.Log
import com.example.infinite_track.data.mapper.auth.toDomain
import com.example.infinite_track.data.mapper.auth.toEntity
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImpl @Inject constructor(
    private val userPreference: UserPreference,
    private val apiService: ApiService,
    private val userDao: UserDao
) : AuthRepository {

    /**
     * Login a user with provided credentials
     * @param loginRequest Login credentials
     * @return Result containing User domain model
     */
    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        return try {
            val response = apiService.login(loginRequest)

            // Save token and user ID to DataStore
            userPreference.saveSession(
                token = response.data.token,
                userId = response.data.id.toString()
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
            // Try to return cached user if available
            val cachedUser = userDao.getUserProfile()
            if (cachedUser != null) {
                return Result.success(cachedUser.toDomain())
            }

            Log.e("AuthRepositoryImpl", "HTTP Error during sync", e)
            Result.failure(Exception("Failed to sync profile data"))
        } catch (e: IOException) {
            // Try to return cached user if available
            val cachedUser = userDao.getUserProfile()
            if (cachedUser != null) {
                return Result.success(cachedUser.toDomain())
            }

            Log.e("AuthRepositoryImpl", "Network Error during sync", e)
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
                apiService.logout()
                Log.d("AuthRepositoryImpl", "Server logout successful")
            } catch (e: Exception) {
                // Log the error but continue with local logout
                Log.e("AuthRepositoryImpl", "Server logout failed, proceeding with local logout", e)
            } finally {
                // Always clear local data, regardless of API call result
                userPreference.clearAuthData()
                userDao.clearUserProfile()
                Log.d("AuthRepositoryImpl", "Local data cleared successfully")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // This would only happen if clearing local data fails
            Log.e("AuthRepositoryImpl", "Critical error during logout", e)
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
}