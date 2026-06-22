package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class ResolveTodayApprovedWfaBookingIdUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(scheduleDateIso: String): Result<Int> {
        var page = 1

        while (true) {
            val bookingPageResult = bookingRepository.getBookingHistory(
                status = "approved",
                page = page,
                limit = 50
            )

            val bookingPage = bookingPageResult.getOrElse { error ->
                return Result.failure(error)
            }

            val approvedBooking = bookingPage.bookings.firstOrNull { booking ->
                booking.statusRaw.equals("approved", ignoreCase = true) &&
                    booking.scheduleDateRaw == scheduleDateIso
            }

            if (approvedBooking != null) {
                return Result.success(approvedBooking.bookingId)
            }

            if (!bookingPage.hasNextPage) {
                break
            }

            page += 1
        }

        return Result.failure(
            IllegalStateException("Approved WFA booking for $scheduleDateIso was not found.")
        )
    }
}
