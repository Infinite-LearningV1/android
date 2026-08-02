package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.wfa.WfaRecommendationQuery
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult

/**
 * Repository interface for WFA (Work From Anywhere) recommendations
 */
interface WfaRepository {
    suspend fun getRecommendations(
        query: WfaRecommendationQuery
    ): WfaRecommendationResult
}
