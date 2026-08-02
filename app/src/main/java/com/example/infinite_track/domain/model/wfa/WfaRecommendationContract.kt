package com.example.infinite_track.domain.model.wfa

import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.time.LocalDate

data class WfaRecommendationQuery(
    val origin: GeoCoordinate,
    val scheduleDate: LocalDate
)

sealed interface WfaRecommendationResult {
    data class Success(
        val scheduleDate: LocalDate,
        val timezone: String,
        val recommendations: List<WfaRecommendation>,
        val meta: WfaRecommendationMeta?
    ) : WfaRecommendationResult

    data class Failure(val failure: WfaRecommendationFailure) : WfaRecommendationResult
}

sealed interface WfaRecommendationFailure {
    data object CurrentLocationUnavailable : WfaRecommendationFailure
    data object InvalidScheduleDate : WfaRecommendationFailure
    data object DuplicateBooking : WfaRecommendationFailure
    data object NetworkUnavailable : WfaRecommendationFailure
    data object ProviderUnavailable : WfaRecommendationFailure
    data object ServerUnavailable : WfaRecommendationFailure
    data object Unknown : WfaRecommendationFailure
}

data class WfaRecommendationMeta(
    val searchRadiusMeters: Double?,
    val candidatesFound: Int?,
    val candidatesReturned: Int?
)

sealed interface WfaRecommendationStatus {
    data object Ranked : WfaRecommendationStatus
    data object InsufficientFacilityData : WfaRecommendationStatus
    data object FacilityEnrichmentFailed : WfaRecommendationStatus
    data class Unsupported(val raw: String) : WfaRecommendationStatus
}

enum class WfaFacilityAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}

data class WfaFacilityEvidence(
    val internetAccess: WfaFacilityAvailability,
    val openingHours: WfaFacilityAvailability,
    val toilets: WfaFacilityAvailability,
    val airConditioning: WfaFacilityAvailability,
    val wheelchairAccessibility: WfaFacilityAvailability
)
