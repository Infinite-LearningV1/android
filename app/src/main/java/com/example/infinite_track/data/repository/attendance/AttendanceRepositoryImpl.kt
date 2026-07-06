package com.example.infinite_track.data.repository.attendance

import android.util.Log
import com.example.infinite_track.data.mapper.attendance.toActiveSession
import com.example.infinite_track.data.mapper.attendance.toDomain
import com.example.infinite_track.data.mapper.attendance.toDto
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.local.preferences.CachedTodayStatusPayload
import com.example.infinite_track.data.soucre.local.preferences.TodayStatusPreference
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import com.example.infinite_track.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val attendancePreference: AttendancePreference,
    private val todayStatusPreference: TodayStatusPreference,
    private val userPreference: UserPreference
) : AttendanceRepository {

    companion object {
        private const val TAG = "AttendanceRepository"
        private val CLEAR_EMPTY_ATTENDANCE_STATES = setOf("not_started", "completed")
    }

    /**
     * Extract error message from HTTP response body
     */
    private fun extractErrorMessage(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val jsonObject = JSONObject(errorBody)
                val message = jsonObject.optString("message", "")
                if (message.isNotEmpty()) {
                    Log.d(TAG, "Extracted error message: $message")
                    return message
                }
            }
            // Fallback to HTTP status message
            "HTTP ${exception.code()} ${exception.message()}"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract error message", e)
            "HTTP ${exception.code()} ${exception.message()}"
        }
    }

    /**
     * Gets the current day's attendance status
     */
    override suspend fun getTodayStatus(forceRefresh: Boolean): Result<TodayStatus> {
        return try {
            val userId = userPreference.getUserId().first()
            val now = System.currentTimeMillis()
            val cached = todayStatusPreference.getTodayStatusCache().first()

            if (!forceRefresh && cached != null && isCacheValid(cached, userId, now)) {
                return Result.success(cached.status)
            }

            val response = apiService.getTodayStatus()
            if (response.success) {
                val status = response.toDomain()
                syncActiveAttendanceId(status)
                todayStatusPreference.saveTodayStatusCache(
                    CachedTodayStatusPayload(
                        userId = userId,
                        todayDate = status.todayDate,
                        attendanceSessionStateId = status.attendanceSessionState?.id,
                        attendanceSessionStateKey = status.attendanceSessionState?.key,
                        activeAttendanceId = status.activeAttendanceId,
                        fetchedAtMillis = now,
                        ttlSeconds = status.cacheTtlSeconds,
                        status = status
                    )
                )
                Result.success(status)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Error getting today's status", e)
            val errorMessage = extractErrorMessage(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's status", e)
            Result.failure(e)
        }
    }

    override suspend fun clearTodayStatusCache() {
        todayStatusPreference.clearTodayStatusCache()
    }

    /**
     * Performs check-in operation - simplified without client-side location validation
     * Backend will handle all location validation
     */
    override suspend fun checkIn(request: AttendanceRequestModel): Result<ActiveAttendanceSession> {
        return try {
            // Convert domain model to DTO using mapper
            val requestDto = request.toDto()

            // Call API for check-in - backend handles location validation
            val response = apiService.checkIn(requestDto)

            if (response.success) {
                // Save the attendance ID for later checkout
                attendancePreference.saveActiveAttendanceId(response.data.idAttendance)
                todayStatusPreference.clearTodayStatusCache()
                // Convert DTO to ActiveAttendanceSession domain model using mapper
                Result.success(response.data.toActiveSession())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Error during check-in", e)
            val errorMessage = extractErrorMessage(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Error during check-in", e)
            Result.failure(e)
        }
    }

    /**
     * Performs check-out operation with attendanceId and coordinates
     * Backend will handle location validation for checkout
     */
    override suspend fun checkOut(
        attendanceId: Int,
        latitude: Double,
        longitude: Double
    ): Result<ActiveAttendanceSession> {
        return try {
            // Create checkout request DTO with coordinates
            val checkOutRequestDto = CheckOutRequestDto(
                latitude = latitude,
                longitude = longitude
            )

            // Call API for check-out with attendanceId in URL and coordinates in body
            val response = apiService.checkOut(attendanceId, checkOutRequestDto)

            if (response.success) {
                // Clear the active attendance ID
                attendancePreference.clearActiveAttendanceId()
                todayStatusPreference.clearTodayStatusCache()
                // Convert DTO to ActiveAttendanceSession domain model using mapper
                Result.success(response.data.toActiveSession())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during check-out", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves the active attendance ID from preferences
     */
    override suspend fun getActiveAttendanceId(): Int? {
        return try {
            attendancePreference.getActiveAttendanceId().first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active attendance ID", e)
            null
        }
    }

    /**
     * Sends location event (ENTER/EXIT) to backend
     */
    override suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit> {
        return try {
            val response = apiService.sendLocationEvent(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Location event sent successfully: ${request.eventType}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send location event: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending location event", e)
            Result.failure(e)
        }
    }

    private fun isCacheValid(cache: CachedTodayStatusPayload, userId: String, now: Long): Boolean {
        if (cache.userId != userId) return false
        if (cache.todayDate != cache.status.todayDate) return false

        val ttlMillis = (cache.ttlSeconds.takeIf { it > 0 } ?: AuthRuntimePolicy.SHARED_TTL_SECONDS) * 1000L
        return now - cache.fetchedAtMillis < ttlMillis
    }

    private suspend fun syncActiveAttendanceId(status: TodayStatus) {
        val activeAttendanceId = status.activeAttendanceId
        when {
            activeAttendanceId != null && activeAttendanceId > 0 -> {
                attendancePreference.saveActiveAttendanceId(activeAttendanceId)
            }
            activeAttendanceId == null && status.attendanceSessionState?.key in CLEAR_EMPTY_ATTENDANCE_STATES -> {
                attendancePreference.clearActiveAttendanceId()
            }
        }
    }
}
