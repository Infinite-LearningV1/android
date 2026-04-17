package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage

interface BookingRepository {
	suspend fun getBookingHistory(
		status: String? = null,
		page: Int = 1,
		limit: Int = 10,
		sortBy: String = "created_at",
		sortOrder: String = "DESC"
	): Result<BookingHistoryPage>

	suspend fun submitBooking(
		scheduleDate: String,
		latitude: Double,
		longitude: Double,
		radius: Int,
		description: String,
		notes: String
	): Result<Unit>
}
