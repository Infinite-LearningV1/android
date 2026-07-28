package com.example.infinite_track.presentation.screen.attendance.wfa_request

sealed interface WfaRequestEffect {
    data object OpenReview : WfaRequestEffect
    data object OpenResult : WfaRequestEffect
    data object ReturnToForm : WfaRequestEffect
    data object ReturnToAttendance : WfaRequestEffect
    data object ReturnHome : WfaRequestEffect
    data class Announce(val message: String) : WfaRequestEffect
}
