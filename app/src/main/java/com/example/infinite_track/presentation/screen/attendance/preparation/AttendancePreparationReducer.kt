package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation

object AttendancePreparationReducer {
    fun selectRecommendation(
        state: AttendancePreparationState,
        recommendation: WfaRecommendation
    ): AttendancePreparationState {
        if (state.selectedMode != WorkMode.WFA) return state
        val discovery = state.wfaDiscovery as? WfaDiscoveryState.Content ?: return state
        return state.copy(
            wfaDiscovery = discovery.copy(
                selectedKey = recommendation.stableKey,
                searchPreview = null
            )
        )
    }

    fun selectSearchPreview(
        state: AttendancePreparationState,
        preview: LocationResult
    ): AttendancePreparationState {
        if (state.selectedMode != WorkMode.WFA) return state
        val discovery = state.wfaDiscovery as? WfaDiscoveryState.Content
            ?: WfaDiscoveryState.Content(recommendations = emptyList())
        return state.copy(
            wfaDiscovery = discovery.copy(
                selectedKey = null,
                searchPreview = preview
            )
        )
    }

}
