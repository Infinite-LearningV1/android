package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class ResolveTodayApprovedWfaBookingUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(scheduleDateIso: String): Result<BookingHistoryItem> {
        var page = 1

        while (true) {
            val bookingPage = bookingRepository.getBookingHistory(
                status = "approved",
                page = page,
                limit = 50
            ).getOrElse { error -> return Result.failure(error) }

            val approvedBooking = bookingPage.bookings.firstOrNull { booking ->
                booking.statusRaw.equals("approved", ignoreCase = true) &&
                    booking.scheduleDateRaw == scheduleDateIso
            }

            if (approvedBooking != null) {
                return Result.success(approvedBooking)
            }

            if (!bookingPage.hasNextPage) break
            page += 1
        }

        return Result.failure(
            IllegalStateException("Approved WFA booking for $scheduleDateIso was not found.")
        )
    }
}
