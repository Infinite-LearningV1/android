package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.WorkMode

object AttendanceCheckInRequestFactory {
    fun create(
        workMode: WorkMode,
        bookingId: Int?
    ): AttendanceRequestModel {
        val categoryId = workMode.categoryId

        if (workMode == WorkMode.WFA && bookingId == null) {
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
