package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.booking.SubmittedWfaRequest
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors

sealed interface WfaRequestPhase {
    data object Loading : WfaRequestPhase
    data object Editing : WfaRequestPhase
    data object ReadyForReview : WfaRequestPhase
    data object Reviewing : WfaRequestPhase
    data object Submitting : WfaRequestPhase
    data object Success : WfaRequestPhase
    data object Failure : WfaRequestPhase
}

data class WfaEmployeeSummary(
    val fullName: String,
    val division: String
)

data class WfaRequestUiState(
    val phase: WfaRequestPhase = WfaRequestPhase.Loading,
    val employee: WfaEmployeeSummary? = null,
    val location: WfaCandidateLocation? = null,
    val config: WfaRequestConfig? = null,
    val draft: WfaRequestDraft = WfaRequestDraft.Empty,
    val fieldErrors: WfaRequestFieldErrors = WfaRequestFieldErrors(),
    val submitResult: SubmittedWfaRequest? = null,
    val failure: WfaRequestFailure? = null
)
