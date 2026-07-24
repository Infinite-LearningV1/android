package com.example.infinite_track.domain.model.wfa

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.util.Locale

/**
 * Domain models for WFA (Work From Anywhere) recommendations
 * These are clean models used by the UI layer
 */
data class WfaRecommendation(
    val stableKey: String,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: String,
    val suitabilityScore: Double,
    val suitabilityLabel: String,
    val distanceMeters: DistanceMeters
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

/**
 * Detailed WFA recommendation with score breakdown
 */
data class WfaRecommendationDetail(
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: String,
    val suitabilityScore: Double,
    val suitabilityLabel: String,
    val distanceKm: Double,
    val wifiQuality: ScoreItem,
    val noiseLevel: ScoreItem,
    val crowdDensity: ScoreItem,
    val operationalHours: ScoreItem,
    val amenities: AmenityItem
) {
    constructor(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        category: String,
        suitabilityScore: Double,
        suitabilityLabel: String,
        distanceKm: Double,
        wifiQuality: ScoreItem,
        noiseLevel: ScoreItem,
        crowdDensity: ScoreItem,
        operationalHours: ScoreItem,
        amenities: AmenityItem
    ) : this(
        name = name,
        address = address,
        coordinate = GeoCoordinate(latitude, longitude),
        category = category,
        suitabilityScore = suitabilityScore,
        suitabilityLabel = suitabilityLabel,
        distanceKm = distanceKm,
        wifiQuality = wifiQuality,
        noiseLevel = noiseLevel,
        crowdDensity = crowdDensity,
        operationalHours = operationalHours,
        amenities = amenities
    )

    val latitude: Double
        get() = coordinate.latitude

    val longitude: Double
        get() = coordinate.longitude
}

data class ScoreItem(
    val score: Double,
    val label: String,
    val description: String
)

data class AmenityItem(
    val score: Double,
    val label: String,
    val facilities: List<String>
)
