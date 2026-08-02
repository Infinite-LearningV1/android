package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure

sealed interface WfaRequestRecommendationState {
    data object Initializing : WfaRequestRecommendationState
    data object Loading : WfaRequestRecommendationState
    data object Empty : WfaRequestRecommendationState
    data class Content(
        val recommendations: List<WfaRecommendation>,
        val selectedKey: String? = null
    ) : WfaRequestRecommendationState

    data class Failure(
        val failure: WfaRecommendationFailure,
        val retryable: Boolean
    ) : WfaRequestRecommendationState
}
