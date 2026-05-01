package com.example.infinite_track.data.repository.attendance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.response.AttendanceData
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class AttendanceRepositoryNotificationStateTest {

    private lateinit var attendancePreference: AttendancePreference
    private lateinit var repository: AttendanceRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        attendancePreference = AttendancePreference(context)
        runBlocking {
            attendancePreference.clearActiveAttendanceId()
            attendancePreference.clearNotificationSessionState()
        }
        repository = AttendanceRepositoryImpl(FakeApiService(), attendancePreference)
    }

    @Test
    fun checkIn_resetsNotificationFlags_and_savesAttendanceId() = runBlocking {
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.setHasEnteredActiveSessionArea(true)

        val request = AttendanceRequestModel(
            categoryId = 1,
            latitude = -6.2,
            longitude = 106.8,
            notes = "Check in",
            bookingId = null,
            type = "WFO"
        )

        val result = repository.checkIn(request)

        assertNotNull(result.getOrNull())
        assertEquals(101, attendancePreference.getActiveAttendanceId().first())
        assertFalse(attendancePreference.isUserInsideGeofence().first())
        assertFalse(attendancePreference.hasEnteredActiveSessionArea().first())
    }

    @Test
    fun checkOut_clearsAttendanceId_and_resetsNotificationFlags() = runBlocking {
        attendancePreference.saveActiveAttendanceId(101)
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.setHasEnteredActiveSessionArea(true)

        val result = repository.checkOut(
            attendanceId = 101,
            latitude = -6.2,
            longitude = 106.8
        )

        assertNotNull(result.getOrNull())
        assertNull(attendancePreference.getActiveAttendanceId().first())
        assertFalse(attendancePreference.isUserInsideGeofence().first())
        assertFalse(attendancePreference.hasEnteredActiveSessionArea().first())
    }

    private class FakeApiService : ApiService {
        override suspend fun login(loginRequest: LoginRequest): LoginResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun getUserProfile(): LoginResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse {
            return AttendanceResponse(
                success = true,
                message = "ok",
                data = sampleAttendanceData(idAttendance = 101, timeOut = null)
            )
        }

        override suspend fun checkOut(
            attendanceId: Int,
            request: CheckOutRequestDto
        ): AttendanceResponse {
            return AttendanceResponse(
                success = true,
                message = "ok",
                data = sampleAttendanceData(idAttendance = attendanceId, timeOut = "17:00:00")
            )
        }

        override suspend fun getTodayStatus(): TodayStatusResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun getAttendanceHistory(
            period: String,
            page: Int,
            limit: Int
        ): AttendanceHistoryResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun updateUserProfile(
            userId: Int,
            request: ProfileUpdateRequest
        ): ProfileUpdateResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun getWfaRecommendations(
            latitude: Double,
            longitude: Double
        ): WfaRecommendationResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): BookingHistoryResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun submitWfaBooking(request: BookingRequest): BookingResponse {
            throw UnsupportedOperationException("Not needed for this test")
        }

        private fun sampleAttendanceData(idAttendance: Int, timeOut: String?): AttendanceData {
            return AttendanceData(
                idAttendance = idAttendance,
                userId = 1,
                categoryId = 1,
                statusId = 1,
                locationId = 1,
                bookingId = null,
                timeIn = "08:00:00",
                timeOut = timeOut,
                workHour = "08:00",
                attendanceDate = "2026-04-23",
                notes = "test",
                createdAt = null,
                updatedAt = null
            )
        }
    }
}
