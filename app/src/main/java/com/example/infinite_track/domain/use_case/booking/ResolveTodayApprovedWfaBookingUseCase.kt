package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.repository.BookingRepository
import javax.inject.Inject

class ResolveTodayApprovedWfaBookingUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(scheduleDate: String): Result<Int?> {
        var page = 1

        while (true) {
            val bookingPageResult = bookingRepository.getBookingHistory(
                status = "approved",
                page = page,
                limit = 20,
                sortBy = "schedule_date",
                sortOrder = "DESC"
            )

            if (bookingPageResult.isFailure) {
                return Result.failure(
                    bookingPageResult.exceptionOrNull()
                        ?: Exception("Gagal memuat booking WFA yang telah di-approve.")
                )
            }

            val bookingPage = bookingPageResult.getOrThrow()
            val matchedBookingId = bookingPage.bookings.firstOrNull { booking ->
                booking.scheduleDateRaw == scheduleDate && booking.statusRaw.equals("approved", ignoreCase = true)
            }?.bookingId

            if (matchedBookingId != null) {
                return Result.success(matchedBookingId)
            }

            if (!bookingPage.hasNextPage) {
                return Result.success(null)
            }

            page++
        }
    }
}
