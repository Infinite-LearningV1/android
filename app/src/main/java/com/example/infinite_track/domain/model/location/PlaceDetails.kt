package com.example.infinite_track.domain.model.location

data class PlaceDetails(
    val placeId: String,
    val displayName: String,
    val formattedAddress: String?,
    val coordinate: GeoCoordinate
)

sealed interface PlaceDetailsResult {
    data class Success(val details: PlaceDetails) : PlaceDetailsResult
    data class Failure(val reason: PlaceDiscoveryFailure) : PlaceDetailsResult
}
