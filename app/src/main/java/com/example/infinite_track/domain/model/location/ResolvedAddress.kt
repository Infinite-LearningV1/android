package com.example.infinite_track.domain.model.location

data class ResolvedAddress(
    val coordinate: GeoCoordinate,
    val name: String?,
    val formattedAddress: String
)

sealed interface AddressResolutionResult {
    data class Resolved(val address: ResolvedAddress) : AddressResolutionResult

    data class CoordinateOnly(val coordinate: GeoCoordinate) : AddressResolutionResult

    sealed interface Failed : AddressResolutionResult {
        data object ServiceUnavailable : Failed
        data object InvalidCoordinate : Failed
        data object ProviderError : Failed
    }
}
