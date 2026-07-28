package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaRequestConfigResult
import com.example.infinite_track.domain.model.booking.WfaRequestResult

interface BookingRepository {
	suspend fun getBookingHistory(
		status: String? = null,
		page: Int = 1,
		limit: Int = 10,
		sortBy: String = "created_at",
		sortOrder: String = "DESC"
	): Result<BookingHistoryPage>

	suspend fun getWfaRequestConfig(): WfaRequestConfigResult

	suspend fun submitWfaRequest(command: SubmitWfaRequestCommand): WfaRequestResult
}
