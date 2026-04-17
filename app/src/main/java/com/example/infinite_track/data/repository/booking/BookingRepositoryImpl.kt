package com.example.infinite_track.data.repository.booking

import com.example.infinite_track.data.mapper.booking.toDomain
import com.example.infinite_track.data.soucre.network.request.BookingRequest
import com.example.infinite_track.data.soucre.network.response.ErrorResponse
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
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
				val bookingHistoryItems = response.data.bookings.map { it.toDomain() }
				val pageResult = BookingHistoryPage(
					bookings = bookingHistoryItems,
					hasNextPage = response.data.pagination.hasNextPage
				)
				Result.success(pageResult)
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
}
