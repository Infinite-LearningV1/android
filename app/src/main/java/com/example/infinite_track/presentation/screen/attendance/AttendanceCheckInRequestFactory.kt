package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel

object AttendanceCheckInRequestFactory {
    fun create(
        selectedWorkMode: String,
        bookingId: Int?
    ): AttendanceRequestModel {
        val categoryId = when (selectedWorkMode) {
            "Work From Office", "WFO" -> 1
            "Work From Home", "WFH" -> 2
            "WFA", "Work From Anywhere" -> 3
            else -> 1
        }

        if (categoryId == 3 && bookingId == null) {
            throw IllegalArgumentException("WFA check-in requires bookingId.")
        }

        return AttendanceRequestModel(
            categoryId = categoryId,
            latitude = 0.0,
            longitude = 0.0,
            notes = "Check-in via mobile app",
            bookingId = bookingId,
            type = "checkin"
        )
    }
}
