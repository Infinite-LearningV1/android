package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.repository.location.AddressResolver
import javax.inject.Inject

/**
 * Use case for getting the current address based on device location
 */
class GetCurrentAddressUseCase @Inject constructor(
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val addressResolver: AddressResolver
) {
    suspend operator fun invoke(): AddressResolutionResult {
        return when (val current = getCurrentLocation()) {
            is CurrentLocationResult.Success -> addressResolver.resolve(current.location.coordinate)
            is CurrentLocationResult.Failure.InvalidCoordinate ->
                AddressResolutionResult.Failed.InvalidCoordinate
            is CurrentLocationResult.Failure.Unavailable ->
                AddressResolutionResult.Failed.ServiceUnavailable
            is CurrentLocationResult.Failure.PermissionDenied,
            is CurrentLocationResult.Failure.ProviderError ->
                AddressResolutionResult.Failed.ProviderError
        }
    }
}
