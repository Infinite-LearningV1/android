package com.example.infinite_track.data.repository.booking

import com.example.infinite_track.data.soucre.network.request.AttendanceRequest
import com.example.infinite_track.data.soucre.network.request.CheckOutRequestDto
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.data.soucre.network.request.LogoutRequest
import com.example.infinite_track.data.soucre.network.request.ProfileUpdateRequest
import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.request.WfaRequestDto
import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryResponse
import com.example.infinite_track.data.soucre.network.response.AttendanceResponse
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.LogoutResponse
import com.example.infinite_track.data.soucre.network.response.ProfileUpdateResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.response.booking.BookingHistoryResponse
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigDataDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestLocationDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestReasonDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestReasonResultDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDataDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDto
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class BookingRepositoryImplWfaRequestTest {

    private val location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")
    private val command = SubmitWfaRequestCommand(
        scheduleDate = LocalDate.of(2026, 8, 10),
        reasonId = 1L,
        otherReasonText = null,
        notes = null,
        location = location
    )

    @Test
    fun `config success returns typed domain config`() = runTest {
        val api = WfaRequestFakeApiService().apply {
            configBehavior = {
                WfaRequestConfigResponseDto(
                    success = true,
                    message = null,
                    data = WfaRequestConfigDataDto(
                        radiusMeters = 100,
                        reasons = listOf(WfaRequestReasonDto(1L, "Client meeting", false))
                    )
                )
            }
        }

        val result = BookingRepositoryImpl(api, Gson()).getWfaRequestConfig()

        assertEquals(100, (result as WfaRequestConfigResult.Success).config.radiusMeters)
    }

    @Test
    fun `malformed config becomes ConfigUnavailable`() = runTest {
        val api = WfaRequestFakeApiService().apply {
            configBehavior = {
                WfaRequestConfigResponseDto(
                    success = true,
                    message = null,
                    data = WfaRequestConfigDataDto(radiusMeters = 0, reasons = emptyList())
                )
            }
        }

        val result = BookingRepositoryImpl(api, Gson()).getWfaRequestConfig()

        assertEquals(
            WfaRequestFailure.ConfigUnavailable,
            (result as WfaRequestConfigResult.Failure).failure
        )
    }

    @Test
    fun `submit success returns backend confirmed request`() = runTest {
        val api = WfaRequestFakeApiService().apply {
            submitBehavior = {
                WfaRequestResponseDto(
                    success = true,
                    message = "Diajukan",
                    data = WfaRequestResponseDataDto(
                        bookingId = 42L,
                        scheduleDate = "2026-08-10",
                        status = "pending",
                        location = WfaRequestLocationDto(-0.9001, 119.877, "Hub", "Palu"),
                        reason = WfaRequestReasonResultDto(1L, "Client meeting"),
                        radiusMeters = 100,
                        submittedAt = null
                    )
                )
            }
        }

        val result = BookingRepositoryImpl(api, Gson()).submitWfaRequest(command)

        assertEquals(42L, (result as WfaRequestResult.Success).request.bookingId)
        assertEquals(1, api.submitCalls)
        assertEquals("2026-08-10", api.lastSubmitRequest?.scheduleDate)
    }

    @Test
    fun `duplicate HTTP failure is classified`() = runTest {
        val api = WfaRequestFakeApiService().apply {
            submitBehavior = {
                throw httpException(
                    409,
                    """{"success":false,"message":"Validasi booking gagal.","errors":[{"field":"schedule_date","code":"DUPLICATE_BOOKING","message":"Sudah ada"}]}"""
                )
            }
        }

        val result = BookingRepositoryImpl(api, Gson()).submitWfaRequest(command)

        assertEquals(
            WfaRequestFailure.DuplicateRequest,
            (result as WfaRequestResult.Failure).failure
        )
    }

    @Test
    fun `IO failure becomes NetworkUnavailable`() = runTest {
        val api = WfaRequestFakeApiService().apply {
            submitBehavior = { throw IOException("offline") }
        }

        val result = BookingRepositoryImpl(api, Gson()).submitWfaRequest(command)

        assertEquals(
            WfaRequestFailure.NetworkUnavailable,
            (result as WfaRequestResult.Failure).failure
        )
        assertTrue(api.submitCalls == 1)
    }

    private fun httpException(status: Int, body: String): HttpException =
        HttpException(Response.error<Any>(status, body.toResponseBody()))
}

private class WfaRequestFakeApiService : ApiService {
    var configBehavior: suspend () -> WfaRequestConfigResponseDto = { error("config not configured") }
    var submitBehavior: suspend (WfaRequestDto) -> WfaRequestResponseDto = { error("submit not configured") }
    var submitCalls: Int = 0
    var lastSubmitRequest: WfaRequestDto? = null

    override suspend fun getWfaRequestConfig(): WfaRequestConfigResponseDto = configBehavior()

    override suspend fun submitWfaRequest(request: WfaRequestDto): WfaRequestResponseDto {
        submitCalls += 1
        lastSubmitRequest = request
        return submitBehavior(request)
    }

    override suspend fun login(loginRequest: LoginRequest): LoginResponse = unsupported()
    override suspend fun getUserProfile(bootstrapAuthRequest: String?): LoginResponse = unsupported()
    override suspend fun logout(): LogoutResponse = unsupported()
    override suspend fun logoutWithRefresh(request: LogoutRequest): LogoutResponse = unsupported()
    override suspend fun refresh(request: RefreshRequest): RefreshResponse = unsupported()
    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse = unsupported()
    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse = unsupported()
    override suspend fun getTodayStatus(): TodayStatusResponse = unsupported()
    override suspend fun getAttendanceHistory(period: String, page: Int, limit: Int): AttendanceHistoryResponse = unsupported()
    override suspend fun previewAttendanceReportPdf(period: String, startDate: String?, endDate: String?, timezone: String?): Response<ResponseBody> = unsupported()
    override suspend fun exportAttendanceReportPdf(period: String, startDate: String?, endDate: String?, timezone: String?): Response<ResponseBody> = unsupported()
    override suspend fun updateUserProfile(userId: Int, request: ProfileUpdateRequest): ProfileUpdateResponse = unsupported()
    override suspend fun sendLocationEvent(request: LocationEventRequest): Response<Unit> = unsupported()
    override suspend fun getWfaRecommendations(latitude: Double, longitude: Double): WfaRecommendationResponse = unsupported()
    override suspend fun getBookingHistory(status: String?, page: Int, limit: Int, sortBy: String, sortOrder: String): BookingHistoryResponse = unsupported()

    private fun unsupported(): Nothing = error("Not supported in this test")
}
