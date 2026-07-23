package com.example.infinite_track.domain.model.location

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?,
    val distance: DistanceMeters?
)

sealed interface PlaceSearchResult {
    data class Success(val suggestions: List<PlaceSuggestion>) : PlaceSearchResult
    data class Failure(val reason: PlaceDiscoveryFailure) : PlaceSearchResult
}

enum class PlaceDiscoveryFailure {
    CONFIGURATION,
    AUTHENTICATION,
    QUOTA,
    NETWORK,
    INVALID_REQUEST,
    UNAVAILABLE
}
