package com.example.infinite_track.data.repository.booking

import com.example.infinite_track.data.mapper.booking.WfaRequestFailureMapper
import com.example.infinite_track.data.mapper.booking.toDomain
import com.example.infinite_track.data.mapper.booking.toDomainOrNull
import com.example.infinite_track.data.mapper.booking.toDto
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestErrorResponseDto
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestResult
import com.example.infinite_track.domain.repository.BookingRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val gson: Gson
) : BookingRepository {

    override suspend fun getWfaRequestConfig(): WfaRequestConfigResult =
        withContext(Dispatchers.IO) {
            try {
                val config = apiService.getWfaRequestConfig().toDomainOrNull()
                    ?: return@withContext WfaRequestConfigResult.Failure(
                        WfaRequestFailure.ConfigUnavailable
                    )
                WfaRequestConfigResult.Success(config)
            } catch (exception: HttpException) {
                WfaRequestConfigResult.Failure(parseWfaHttpFailure(exception))
            } catch (throwable: Throwable) {
                WfaRequestConfigResult.Failure(WfaRequestFailureMapper.mapThrowable(throwable))
            }
        }

    override suspend fun submitWfaRequest(
        command: SubmitWfaRequestCommand
    ): WfaRequestResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.submitWfaRequest(command.toDto())
            val request = response.toDomainOrNull(command.location)
            when {
                request != null -> WfaRequestResult.Success(request)
                !response.success && !response.message.isNullOrBlank() -> WfaRequestResult.Failure(
                    WfaRequestFailure.BackendRejected(response.message)
                )
                else -> WfaRequestResult.Failure(WfaRequestFailure.Unknown)
            }
        } catch (exception: HttpException) {
            WfaRequestResult.Failure(parseWfaHttpFailure(exception))
        } catch (throwable: Throwable) {
            WfaRequestResult.Failure(WfaRequestFailureMapper.mapThrowable(throwable))
        }
    }

    override suspend fun getBookingHistory(
        status: String?,
        page: Int,
        limit: Int,
        sortBy: String,
        sortOrder: String
    ): Result<BookingHistoryPage> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBookingHistory(
                    status = status,
                    page = page,
                    limit = limit,
                    sortBy = sortBy,
                    sortOrder = sortOrder
                )
                if (response.success) {
                    Result.success(response.toDomain())
                } else {
                    Result.failure(Exception(response.message ?: "Riwayat booking gagal diambil."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun submitBooking(
        scheduleDate: String,
        latitude: Double,
        longitude: Double,
        radius: Int,
        description: String,
        notes: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = BookingRequest(
                    scheduleDate = scheduleDate,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    description = description,
                    notes = notes
                )

                val response = apiService.submitWfaBooking(request)

                if (response.success) {
                    Result.success(Unit)
                } else {
                    // If success=false, use message from server
                    Result.failure(Exception(response.message))
                }
            } catch (e: HttpException) {
                // Handle HTTP errors (like 400 or 500)
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = try {
                    gson.fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorResponse?.message ?: "Terjadi kesalahan jaringan."
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                // For other errors (like no connection)
                Result.failure(Exception("Tidak dapat terhubung ke server."))
            }
        }
    }

    private fun parseWfaHttpFailure(exception: HttpException): WfaRequestFailure {
        val parsed = runCatching {
            exception.response()?.errorBody()?.string()?.let { body ->
                gson.fromJson(body, WfaRequestErrorResponseDto::class.java)
            }
        }.getOrNull()
        val firstCode = parsed?.code ?: parsed?.errors?.firstNotNullOfOrNull { it.code }
        val fieldErrors = parsed?.errors.orEmpty().mapNotNull { error ->
            val field = error.field ?: return@mapNotNull null
            val message = error.message ?: return@mapNotNull null
            field to message
        }.toMap()

        return WfaRequestFailureMapper.mapHttp(
            statusCode = exception.code(),
            code = firstCode,
            safeMessage = parsed?.message,
            fieldErrors = fieldErrors
        )
    }
}
