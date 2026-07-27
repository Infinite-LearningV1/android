package com.example.infinite_track.domain.model.attendance

/**
 * Typed Layer 5 submit failure classification. Transport/provider types
 * (HttpException, DTOs) must never cross this boundary.
 */
sealed interface AttendanceSubmitFailure {
    // Local precondition failures (before any backend call)
    data object CurrentLocationUnavailable : AttendanceSubmitFailure
    data object SessionUnavailable : AttendanceSubmitFailure
    data object ActiveAttendanceUnavailable : AttendanceSubmitFailure
    data object TargetModeMismatch : AttendanceSubmitFailure
    data object WfaBookingRequired : AttendanceSubmitFailure

    // Backend business rejections
    data object DuplicateAttendance : AttendanceSubmitFailure
    data object OutsideAllowedRadius : AttendanceSubmitFailure
    data object WfaBookingRejected : AttendanceSubmitFailure
    data object AlreadyCheckedOut : AttendanceSubmitFailure

    // Transport failures
    data object NetworkUnavailable : AttendanceSubmitFailure
    data object ServerUnavailable : AttendanceSubmitFailure

    /** Recognized backend rejection whose category is unknown; [safeReason] is the backend `message` string only. */
    data class BackendRejected(val safeReason: String) : AttendanceSubmitFailure

    data object Unknown : AttendanceSubmitFailure
}
