package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.data.soucre.network.response.FacilityEvidenceDto
import com.example.infinite_track.data.soucre.network.response.RecommendationItem
import com.example.infinite_track.data.soucre.network.response.WfaRecommendationResponse
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationMeta
import com.example.infinite_track.domain.model.wfa.WfaRecommendationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import java.util.Locale
import java.time.LocalDate

fun RecommendationItem.toDomainOrNull(): WfaRecommendation? {
    if (facilityConfidence !in 0..100) return null

    val normalizedStatus = status.trim().lowercase(Locale.ROOT)
    val mappedStatus = when (normalizedStatus) {
        "ranked" -> if (finalScore != null && !finalLabel.isNullOrBlank()) {
            WfaRecommendationStatus.Ranked
        } else {
            WfaRecommendationStatus.Unsupported("ranked_missing_final_score")
        }
        "insufficient_facility_data" -> WfaRecommendationStatus.InsufficientFacilityData
        "facility_enrichment_failed" -> WfaRecommendationStatus.FacilityEnrichmentFailed
        else -> WfaRecommendationStatus.Unsupported(normalizedStatus)
    }
    val ranked = mappedStatus is WfaRecommendationStatus.Ranked
    val normalizedPlaceId = placeId?.trim()?.takeIf(String::isNotEmpty)

    return runCatching {
        WfaRecommendation(
            stableKey = normalizedPlaceId?.let { "place:$it" }
                ?: WfaRecommendation.stableKeyFor(name, latitude, longitude),
            placeId = normalizedPlaceId,
            name = name,
            address = address,
            coordinate = GeoCoordinate(latitude, longitude),
            placeType = placeType,
            distanceMeters = DistanceMeters(distanceMeters),
            status = mappedStatus,
            finalRank = finalRank.takeIf { ranked },
            finalScore = finalScore.takeIf { ranked },
            finalLabel = finalLabel?.trim()?.takeIf { ranked },
            facilityScore = facilityScore,
            facilityConfidence = facilityConfidence,
            facilities = facilities.toDomain()
        )
    }.getOrNull()
}

fun List<RecommendationItem>.toDomain(): List<WfaRecommendation> = mapNotNull {
    it.toDomainOrNull()
}

fun WfaRecommendationResponse.toDomainResultOrNull(): WfaRecommendationResult.Success? {
    if (!success || data.timezone.isBlank()) return null
    val scheduleDate = runCatching { LocalDate.parse(data.scheduleDate) }.getOrNull()
        ?: return null
    return WfaRecommendationResult.Success(
        scheduleDate = scheduleDate,
        timezone = data.timezone,
        recommendations = data.recommendations.toDomain(),
        meta = meta?.let {
            WfaRecommendationMeta(
                searchRadiusMeters = it.searchRadiusMeters,
                candidatesFound = it.candidatesFound,
                candidatesReturned = it.candidatesReturned
            )
        }
    )
}

private fun FacilityEvidenceDto.toDomain(): WfaFacilityEvidence = WfaFacilityEvidence(
    internetAccess = internetAccess.toAvailability(),
    openingHours = openingHours.toAvailability(),
    toilets = toilets.toAvailability(),
    airConditioning = airConditioning.toAvailability(),
    wheelchairAccessibility = wheelchairAccessibility.toAvailability()
)

private fun Boolean?.toAvailability(): WfaFacilityAvailability = when (this) {
    true -> WfaFacilityAvailability.AVAILABLE
    false -> WfaFacilityAvailability.UNAVAILABLE
    null -> WfaFacilityAvailability.UNKNOWN
}
