package com.example.infinite_track.domain.model.booking

data class WfaCandidateLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val formattedAddress: String
) {
    val hasValidCoordinates: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            latitude != 0.0 && longitude != 0.0
}
