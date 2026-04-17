package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class GetBookingHistoryUseCase @Inject constructor(
	private val bookingRepository: BookingRepository
) {
	suspend operator fun invoke(
		status: String? = null,
		page: Int = 1,
		limit: Int = 10,
		sortBy: String = "created_at",
		sortOrder: String = "DESC"
	): Result<BookingHistoryPage> {
		return bookingRepository.getBookingHistory(status, page, limit, sortBy, sortOrder)
	}
}
