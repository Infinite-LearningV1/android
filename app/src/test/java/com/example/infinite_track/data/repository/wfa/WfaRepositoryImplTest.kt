package com.example.infinite_track.data.repository.wfa

import com.example.infinite_track.data.soucre.network.response.FacilityEvidenceDto
import com.example.infinite_track.data.soucre.network.response.RecommendationItem
import com.example.infinite_track.data.soucre.network.response.WfaData
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.LocalDate

class WfaRepositoryImplTest {

    @Test
    fun `repository forwards ISO date and maps successful response`() = runTest {
        var capturedArguments: List<Any?> = emptyList()
        val repository = WfaRepositoryImpl(
            apiService = apiService { arguments ->
                capturedArguments = arguments.take(3)
                successfulResponse()
            },
            gson = Gson()
        )

        val result = repository.getRecommendations(query()) as WfaRecommendationResult.Success

        assertEquals(listOf(-0.9, 119.88, "2026-08-03"), capturedArguments)
        assertEquals(LocalDate.of(2026, 8, 3), result.scheduleDate)
        assertEquals("Asia/Jakarta", result.timezone)
        assertEquals(1, result.recommendations.size)
    }

    @Test
    fun `http rejection maps safe error code`() = runTest {
        val repository = WfaRepositoryImpl(
            apiService = apiService {
                throw httpException(409, "DUPLICATE_BOOKING")
            },
            gson = Gson()
        )

        val result = repository.getRecommendations(query()) as WfaRecommendationResult.Failure

        assertEquals(WfaRecommendationFailure.DuplicateBooking, result.failure)
    }

    @Test
    fun `io exception maps to network unavailable`() = runTest {
        val repository = WfaRepositoryImpl(
            apiService = apiService { throw IOException("offline") },
            gson = Gson()
        )

        val result = repository.getRecommendations(query()) as WfaRecommendationResult.Failure

        assertEquals(WfaRecommendationFailure.NetworkUnavailable, result.failure)
    }

    @Test
    fun `malformed successful response fails closed`() = runTest {
        val repository = WfaRepositoryImpl(
            apiService = apiService {
                successfulResponse().copy(
                    data = successfulResponse().data.copy(scheduleDate = "not-a-date")
                )
            },
            gson = Gson()
        )

        val result = repository.getRecommendations(query())

        assertTrue(result is WfaRecommendationResult.Failure)
        assertEquals(
            WfaRecommendationFailure.Unknown,
            (result as WfaRecommendationResult.Failure).failure
        )
    }

    private fun query() = WfaRecommendationQuery(
        origin = GeoCoordinate(-0.9, 119.88),
        scheduleDate = LocalDate.of(2026, 8, 3)
    )

    private fun successfulResponse() = WfaRecommendationResponse(
        success = true,
        message = "OK",
        data = WfaData(
            scheduleDate = "2026-08-03",
            timezone = "Asia/Jakarta",
            workWindow = null,
            recommendations = listOf(
                RecommendationItem(
                    placeId = "place-1",
                    name = "Cafe",
                    address = "Palu",
                    latitude = -0.9,
                    longitude = 119.88,
                    distanceMeters = 100.0,
                    placeType = "cafe",
                    status = "ranked",
                    finalRank = 1,
                    finalScore = 90.0,
                    finalLabel = "Tinggi",
                    facilityScore = 80.0,
                    facilityConfidence = 90,
                    facilities = FacilityEvidenceDto(true, true, true, null, null)
                )
            )
        )
    )

    private fun apiService(handler: (List<Any?>) -> Any): ApiService =
        Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, arguments ->
            when (method.name) {
                "getWfaRecommendations" -> handler(arguments.orEmpty().toList())
                "toString" -> "WfaRepositoryImplTestApiService"
                else -> error("Unexpected API call: ${method.name}")
            }
        } as ApiService

    private fun httpException(status: Int, code: String): HttpException {
        val body = """{"success":false,"message":"Rejected","code":"$code"}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, body))
    }
}
