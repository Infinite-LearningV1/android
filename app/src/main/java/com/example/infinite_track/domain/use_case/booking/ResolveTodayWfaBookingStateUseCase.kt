package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.repository.BookingRepository
import java.util.Locale
import javax.inject.Inject

class ResolveTodayWfaBookingStateUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(scheduleDateIso: String): WfaBookingForDate {
        var page = 1
        var latestBooking: BookingHistoryItem? = null

        while (true) {
            val bookingPage = bookingRepository.getBookingHistory(page = page, limit = PAGE_SIZE)
                .getOrElse { error -> return WfaBookingForDate.Failed(error) }

            bookingPage.bookings
                .filter { it.scheduleDateRaw == scheduleDateIso }
                .forEach { booking ->
                    if (latestBooking == null || booking.bookingId > latestBooking!!.bookingId) {
                        latestBooking = booking
                    }
                }

            if (!bookingPage.hasNextPage) break
            page += 1
        }

        val booking = latestBooking ?: return WfaBookingForDate.NotRequested
        return when (booking.normalizedStatus()) {
            "pending" -> WfaBookingForDate.Pending(booking)
            "rejected" -> WfaBookingForDate.Rejected(booking)
            "approved" -> WfaBookingForDate.Approved(booking)
            else -> WfaBookingForDate.NotRequested
        }
    }

    private fun BookingHistoryItem.normalizedStatus(): String =
        statusKey.ifBlank { statusRaw.ifBlank { status } }.lowercase(Locale.ROOT)

    private companion object {
        const val PAGE_SIZE = 50
    }
}
