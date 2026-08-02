package com.example.infinite_track.domain.model.wfa

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.util.Locale

data class WfaRecommendation(
    val stableKey: String,
    val placeId: String?,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val placeType: String,
    val distanceMeters: DistanceMeters,
    val status: WfaRecommendationStatus,
    val finalRank: Int?,
    val finalScore: Double?,
    val finalLabel: String?,
    val facilityScore: Double?,
    val facilityConfidence: Int,
    val facilities: WfaFacilityEvidence
) {
    val latitude: Double
        get() = coordinate.latitude

    val longitude: Double
        get() = coordinate.longitude

    companion object {
        fun stableKeyFor(name: String, latitude: Double, longitude: Double): String =
            "${name.trim().lowercase(Locale.ROOT)}@${String.format(Locale.US, "%.6f,%.6f", latitude, longitude)}"
    }
}
