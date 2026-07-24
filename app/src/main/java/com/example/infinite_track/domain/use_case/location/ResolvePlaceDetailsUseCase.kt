package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import javax.inject.Inject

class ResolvePlaceDetailsUseCase @Inject constructor(
    private val repository: PlaceDiscoveryRepository
) {
    suspend operator fun invoke(suggestion: PlaceSuggestion): PlaceDetailsResult {
        return if (suggestion.placeId.isBlank()) {
            PlaceDetailsResult.Failure(PlaceDiscoveryFailure.INVALID_REQUEST)
        } else {
            repository.resolve(suggestion)
        }
    }
}
