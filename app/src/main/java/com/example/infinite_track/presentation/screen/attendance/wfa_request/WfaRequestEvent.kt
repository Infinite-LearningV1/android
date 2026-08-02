package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import java.time.LocalDate

sealed interface WfaRequestEvent {
    data class ScheduleDateChanged(val date: LocalDate?) : WfaRequestEvent
    data class ReasonSelected(val reasonId: Long) : WfaRequestEvent
    data class OtherReasonChanged(val value: String) : WfaRequestEvent
    data class NotesChanged(val value: String) : WfaRequestEvent
    data class RecommendationSelected(val stableKey: String) : WfaRequestEvent
    data class ManualLocationSelected(val location: WfaCandidateLocation) : WfaRequestEvent
    data object RetryRecommendationsClicked : WfaRequestEvent
    data object ReviewClicked : WfaRequestEvent
    data object EditClicked : WfaRequestEvent
    data object SubmitConfirmed : WfaRequestEvent
    data object RetryConfigClicked : WfaRequestEvent
    data object RetrySubmitClicked : WfaRequestEvent
}
