package com.example.infinite_track.domain.model.attendance

/**
 * Canonical Layer 5 submit command. Presentation builds this from already-resolved
 * preparation state; all business validation happens in SubmitAttendanceUseCase.
 */
sealed interface AttendanceSubmitCommand {
    val intent: AttendanceActionIntent

    data class CheckIn(
        val workMode: WorkMode,
        val authoritativeTarget: AuthoritativeTargetLocation
    ) : AttendanceSubmitCommand {
        override val intent: AttendanceActionIntent = AttendanceActionIntent.CHECK_IN
    }

    data class CheckOut(
        val activeAttendanceId: Int?
    ) : AttendanceSubmitCommand {
        override val intent: AttendanceActionIntent = AttendanceActionIntent.CHECK_OUT
    }
}
