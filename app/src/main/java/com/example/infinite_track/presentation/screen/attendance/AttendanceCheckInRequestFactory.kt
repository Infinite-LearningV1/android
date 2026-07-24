package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode

object AttendanceCheckInRequestFactory {
    fun create(
        workMode: WorkMode,
        authoritativeTarget: AuthoritativeTargetLocation
    ): AttendanceRequestModel {
        require(authoritativeTarget.mode == workMode) {
            "Authoritative target mode must match the selected work mode."
        }
        val categoryId = workMode.categoryId
        val bookingId = authoritativeTarget.approvedWfaContext?.bookingId

        if (workMode == WorkMode.WFA && bookingId == null) {
            throw IllegalArgumentException("WFA check-in requires bookingId.")
        }

        return AttendanceRequestModel(
            categoryId = categoryId,
            latitude = 0.0,
            longitude = 0.0,
            notes = "Check-in via mobile app",
            bookingId = bookingId.takeIf { workMode == WorkMode.WFA },
            type = "checkin"
        )
    }
}
