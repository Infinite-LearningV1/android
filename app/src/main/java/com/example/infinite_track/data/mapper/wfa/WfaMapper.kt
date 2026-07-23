package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.data.soucre.network.response.RecommendationItem
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.wfa.WfaRecommendation

/**
 * Mapper functions to convert network DTOs to domain models
 */

fun RecommendationItem.toDomain(): WfaRecommendation {
    return WfaRecommendation(
        stableKey = WfaRecommendation.stableKeyFor(this.name, this.latitude, this.longitude),
        name = this.name,
        address = this.address,
        category = this.category,
        coordinate = com.example.infinite_track.domain.model.location.GeoCoordinate(this.latitude, this.longitude),
        suitabilityScore = this.suitabilityScore,
        suitabilityLabel = this.suitabilityLabel,
        distanceMeters = DistanceMeters(this.distanceFromCenter)
    )
}

fun List<RecommendationItem>.toDomain(): List<WfaRecommendation> {
    return this.map { it.toDomain() }
}
