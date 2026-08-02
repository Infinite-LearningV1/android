package com.example.infinite_track.domain.use_case.wfa

import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.repository.WfaRepository
import javax.inject.Inject

/**
 * Use case for getting WFA (Work From Anywhere) recommendations
 */
class GetWfaRecommendationsUseCase @Inject constructor(
    private val wfaRepository: WfaRepository
) {
    suspend operator fun invoke(query: WfaRecommendationQuery): WfaRecommendationResult =
        wfaRepository.getRecommendations(query)
}
