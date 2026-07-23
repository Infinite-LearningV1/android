package com.example.infinite_track.domain.model.location

/**
 * Provider-neutral geographic coordinate used across domain and presentation boundaries.
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude.isFinite()) { "Latitude must be finite" }
        require(longitude.isFinite()) { "Longitude must be finite" }
        require(latitude in MIN_LATITUDE..MAX_LATITUDE) {
            "Latitude must be between $MIN_LATITUDE and $MAX_LATITUDE"
        }
        require(longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
            "Longitude must be between $MIN_LONGITUDE and $MAX_LONGITUDE"
        }
    }

    companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}
