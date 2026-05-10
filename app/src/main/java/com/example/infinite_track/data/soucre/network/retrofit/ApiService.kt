package com.example.infinite_track.data.soucre.network.retrofit

import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Singleton

@Singleton
interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @GET("api/auth/me")
    suspend fun getUserProfile(): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("api/attendance/check-in")
    suspend fun checkIn(
        @Body request: AttendanceRequest
    ): AttendanceResponse

    @POST("api/attendance/checkout/{id_attendance}")
    suspend fun checkOut(
        @Path("id_attendance") attendanceId: Int,
        @Body request: CheckOutRequestDto
    ): AttendanceResponse

    @GET("api/attendance/status-today")
    suspend fun getTodayStatus(): TodayStatusResponse

    @GET("api/attendance/history")
    suspend fun getAttendanceHistory(
        @Query("period") period: String = "daily",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 5
    ): AttendanceHistoryResponse

    @PATCH("api/users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: Int,
        @Body request: ProfileUpdateRequest
    ): ProfileUpdateResponse

    @POST("api/attendance/location-event")
    suspend fun sendLocationEvent(
        @Body request: LocationEventRequest
    ): retrofit2.Response<Unit>

    @GET("api/wfa/recommendations")
    suspend fun getWfaRecommendations(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): WfaRecommendationResponse

    @GET("api/bookings/history")
    suspend fun getBookingHistory(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("sort_by") sortBy: String = "created_at",
        @Query("sort_order") sortOrder: String = "DESC"
    ): BookingHistoryResponse

    @POST("api/bookings")
    suspend fun submitWfaBooking(
        @Body request: BookingRequest
    ): BookingResponse
}