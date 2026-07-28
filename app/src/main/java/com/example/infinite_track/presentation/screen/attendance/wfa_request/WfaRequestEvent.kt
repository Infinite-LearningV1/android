package com.example.infinite_track.presentation.screen.attendance.wfa_request

import java.time.LocalDate

sealed interface WfaRequestEvent {
    data class ScheduleDateChanged(val date: LocalDate?) : WfaRequestEvent
    data class ReasonSelected(val reasonId: Long) : WfaRequestEvent
    data class OtherReasonChanged(val value: String) : WfaRequestEvent
    data class NotesChanged(val value: String) : WfaRequestEvent
    data object ReviewClicked : WfaRequestEvent
    data object EditClicked : WfaRequestEvent
    data object SubmitConfirmed : WfaRequestEvent
    data object RetryConfigClicked : WfaRequestEvent
    data object RetrySubmitClicked : WfaRequestEvent
}
