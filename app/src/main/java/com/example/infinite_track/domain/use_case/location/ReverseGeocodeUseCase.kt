package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.location.AddressResolver
import javax.inject.Inject

/**
 * Use case for reverse geocoding - converting coordinates to address
 */
class ReverseGeocodeUseCase @Inject constructor(
    private val addressResolver: AddressResolver
) {
    suspend operator fun invoke(coordinate: GeoCoordinate): AddressResolutionResult =
        addressResolver.resolve(coordinate)
}
