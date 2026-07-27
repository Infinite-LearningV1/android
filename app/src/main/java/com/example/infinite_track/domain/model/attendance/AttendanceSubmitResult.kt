package com.example.infinite_track.domain.model.attendance

/** Typed outcome of one Layer 5 backend mutation attempt. */
sealed interface AttendanceSubmitResult {
    data class Success(
        val intent: AttendanceActionIntent,
        val session: ActiveAttendanceSession
    ) : AttendanceSubmitResult

    data class Failure(
        val intent: AttendanceActionIntent,
        val failure: AttendanceSubmitFailure
    ) : AttendanceSubmitResult
}
