package com.example.infinite_track.domain.model.wfa

import com.example.infinite_track.domain.model.booking.BookingHistoryItem

sealed interface WfaBookingForDate {
    data object NotRequested : WfaBookingForDate
    data class Pending(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Rejected(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Approved(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Failed(val cause: Throwable) : WfaBookingForDate
}
