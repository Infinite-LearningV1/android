package com.example.infinite_track.data.repository.auth

import android.util.Log
import com.example.infinite_track.data.mapper.auth.toDomain
import com.example.infinite_track.data.mapper.auth.toEntity
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.local.room.UserDao
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.LogoutRequest
import com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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

    override suspend fun refreshSession(): Result<AuthRefreshResult> {
        return try {
            val existingRefreshToken = userPreference.getRefreshToken().first()
            if (existingRefreshToken.isBlank()) {
                safeLogDebug("Refresh session skipped because refresh token is missing")
                return Result.failure(
                    AuthRefreshException(
                        kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                        reason = AuthRefreshFailureReason.MISSING_REFRESH_TOKEN,
                        message = "Refresh token is missing"
                    )
                )
            }

            val refreshData = authSessionApiService.refreshSession(
                RefreshSessionRequest(refreshToken = existingRefreshToken)
            ).data
            val accessToken = refreshData.resolvedAccessToken()

            if (accessToken.isBlank() || refreshData.id <= 0) {
                val error = IllegalStateException("Invalid refresh session payload")
                safeLogError("Refresh session returned invalid payload", error)
                return Result.failure(
                    AuthRefreshException(
                        kind = AuthRefreshFailureKind.TRANSIENT,
                        reason = AuthRefreshFailureReason.INVALID_PAYLOAD,
                        message = "Invalid refresh session payload",
                        cause = error
                    )
                )
            }

            val refreshedUserId = refreshData.id.toString()
            val refreshTokenToStore = refreshData.resolvedRefreshToken() ?: existingRefreshToken

            userPreference.saveSession(
                token = accessToken,
                userId = refreshedUserId,
                refreshToken = refreshTokenToStore,
                lastRefreshAt = System.currentTimeMillis()
            )
            Result.success(
                AuthRefreshResult(
                    token = accessToken,
                    refreshToken = refreshTokenToStore,
                    userId = refreshedUserId
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            classifyRefreshHttpError(e)
        } catch (e: IOException) {
            safeLogError("Refresh session failed due to transport error", e)
            Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.TRANSPORT,
                    reason = AuthRefreshFailureReason.TRANSPORT_ERROR,
                    message = e.message ?: "Refresh transport error",
                    cause = e
                )
            )
        }
    }


    /**
     * Login a user with provided credentials
     * @param loginRequest Login credentials
     * @return Result containing User domain model
     */
    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        return try {
            val loginResponse = apiService.login(loginRequest)
            val loginData = loginResponse.data
            val accessToken = loginData.resolvedAccessToken().takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Login response missing usable access token"))
            val refreshToken = loginData.resolvedRefreshToken()

            userPreference.saveSession(
                token = accessToken,
                userId = loginData.id.toString(),
                refreshToken = refreshToken,
                lastRefreshAt = System.currentTimeMillis()
            )

            val user = loginData.toDomain()
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
     * @return Explicit sync outcome for success, unauthorized, or temporary failure
     */
    override suspend fun syncUserProfile(): ProfileSyncResult {
        return syncUserProfile(bootstrapAuthRequest = null)
    }

    override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
        return syncUserProfile(bootstrapAuthRequest = ApiService.BOOTSTRAP_AUTH_REQUEST_VALUE)
    }

    private suspend fun syncUserProfile(bootstrapAuthRequest: String?): ProfileSyncResult {
        return try {
            val token = userPreference.getAuthToken().first()

            if (token.isBlank()) {
                safeLogDebug("Profile sync skipped because auth token is missing")
                return ProfileSyncResult.Unauthorized()
            }

            val response = apiService.getUserProfile(bootstrapAuthRequest)
            val user = response.data.toDomain()
            val userEntity = user.toEntity(userDao.getUserProfile()?.faceEmbedding)
            userDao.insertOrUpdateUserProfile(userEntity)

            ProfileSyncResult.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                val reason = authFailureReasonFor(parseHttpErrorBodySafely(e)?.code)
                safeLogDebug("Profile sync unauthorized: HTTP ${e.code()}")
                ProfileSyncResult.Unauthorized(reason)
            } else {
                safeLogError("Profile sync failed with HTTP ${e.code()}", e)
                ProfileSyncResult.TemporaryFailure(
                    cause = e,
                    message = parseHttpErrorBodySafely(e)?.message ?: "Failed to sync profile data"
                )
            }
        } catch (e: IOException) {
            safeLogError("Profile sync failed due to network error", e)
            ProfileSyncResult.TemporaryFailure(
                cause = e,
                message = "Network error, please check your internet connection."
            )
        }
    }

    /**
     * Attempts server-side logout without clearing local runtime state.
     * Local cleanup is orchestrated by LogoutUseCase or ForceReauthUseCase.
     */
    override suspend fun logoutRemote(): Result<Unit> {
        return try {
            val refreshToken = userPreference.getRefreshToken().first()
            if (refreshToken.isNotBlank()) {
                apiService.logoutWithRefresh(LogoutRequest(refreshToken = refreshToken))
            } else {
                authSessionApiService.logout()
            }
            safeLogDebug("Server logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            safeLogError("Server logout failed", e)
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun logout(): Result<Unit> = logoutRemote()

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

    private fun classifyRefreshHttpError(httpException: HttpException): Result<AuthRefreshResult> {
        val statusCode = httpException.code()
        val errorBody = parseHttpErrorBodySafely(httpException)
        val normalizedCode = errorBody?.code?.uppercase(Locale.ROOT)
        val message = errorBody?.message ?: "HTTP $statusCode"

        val reason = authFailureReasonFor(normalizedCode)
        val exception = when (reason) {
            AuthRefreshFailureReason.ACCESS_EXPIRED -> AuthRefreshException(
                kind = AuthRefreshFailureKind.TRANSIENT,
                reason = reason,
                message = message,
                cause = httpException
            )

            AuthRefreshFailureReason.REFRESH_INVALID,
            AuthRefreshFailureReason.REFRESH_REVOKED,
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> AuthRefreshException(
                kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                reason = reason,
                message = message,
                cause = httpException
            )

            else -> AuthRefreshException(
                kind = AuthRefreshFailureKind.TRANSPORT,
                reason = AuthRefreshFailureReason.TRANSPORT_ERROR,
                message = message,
                cause = httpException
            )
        }

        safeLogDebug("Refresh session rejected: HTTP $statusCode, code=${normalizedCode ?: "UNKNOWN"}")
        return Result.failure(exception)
    }

    private fun authFailureReasonFor(code: String?): AuthRefreshFailureReason? {
        return when (code?.uppercase(Locale.ROOT)) {
            "AUTH_ACCESS_TOKEN_EXPIRED" -> AuthRefreshFailureReason.ACCESS_EXPIRED
            "AUTH_REFRESH_TOKEN_INVALID" -> AuthRefreshFailureReason.REFRESH_INVALID
            "AUTH_REFRESH_TOKEN_REVOKED" -> AuthRefreshFailureReason.REFRESH_REVOKED
            "AUTH_SESSION_INACTIVE", "INACTIVITY_TIMEOUT_48H" -> AuthRefreshFailureReason.INACTIVITY_EXPIRED
            else -> null
        }
    }

    private fun parseHttpErrorBodySafely(httpException: HttpException): ErrorResponse? {
        val rawBody = httpException.response()?.errorBody()?.string().orEmpty()
        if (rawBody.isBlank()) {
            return null
        }

        return try {
            Gson().fromJson(rawBody, ErrorResponse::class.java)
        } catch (e: JsonSyntaxException) {
            safeLogError("Failed to parse HTTP error body JSON", e)
            null
        }
    }
}