package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import javax.inject.Inject

class ResolvePlaceDetailsUseCase @Inject constructor(
    private val repository: PlaceDiscoveryRepository
) {
    suspend operator fun invoke(placeId: String): PlaceDetailsResult {
        return if (placeId.isBlank()) {
            PlaceDetailsResult.Failure(
                com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure.INVALID_REQUEST
            )
        } else {
            repository.resolve(placeId)
        }
    }
}
