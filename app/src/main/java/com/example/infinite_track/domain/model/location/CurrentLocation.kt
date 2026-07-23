package com.example.infinite_track.domain.model.location

data class CurrentLocation(
    val coordinate: GeoCoordinate,
    val accuracy: DistanceMeters?,
    val capturedAtEpochMillis: Long,
    val provider: String?,
    val isMock: Boolean
)

sealed interface CurrentLocationResult {
    data class Success(val location: CurrentLocation) : CurrentLocationResult

    sealed interface Failure : CurrentLocationResult {
        object PermissionDenied : Failure
        object Unavailable : Failure
        object InvalidCoordinate : Failure
        object ProviderError : Failure
    }
}
