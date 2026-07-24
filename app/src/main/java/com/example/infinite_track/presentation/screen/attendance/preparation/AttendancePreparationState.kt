package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation

data class AttendancePreparationState(
    val selectedMode: WorkMode = WorkMode.WFO,
    val targetResolution: TargetLocationResolution = TargetLocationResolution.Resolving(WorkMode.WFO),
    val currentLocation: CurrentLocationResult? = null,
    val rangeStatus: TargetRangeStatus? = null,
    val wfaDiscovery: WfaDiscoveryState = WfaDiscoveryState.Hidden,
    val mapPickInteraction: WfaMapPickInteractionState = WfaMapPickInteractionState.Inactive,
    val eligibility: AttendancePreparationEligibility = AttendancePreparationEligibility.Resolving
)

sealed interface WfaMapPickInteractionState {
    data object Inactive : WfaMapPickInteractionState
    data class Active(val sessionId: Long) : WfaMapPickInteractionState
}

sealed interface WfaDiscoveryState {
    data object Hidden : WfaDiscoveryState
    data object Loading : WfaDiscoveryState
    data object Empty : WfaDiscoveryState
    data class Failure(val retryable: Boolean = true) : WfaDiscoveryState

    data class Content(
        val recommendations: List<WfaRecommendation>,
        val selectedKey: String? = null,
        val searchPreview: LocationResult? = null
    ) : WfaDiscoveryState
}
