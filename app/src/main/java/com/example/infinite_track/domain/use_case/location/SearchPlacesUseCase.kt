package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(
    private val repository: PlaceDiscoveryRepository
) {
    suspend operator fun invoke(
        query: String,
        proximity: GeoCoordinate? = null
    ): PlaceSearchResult {
        val normalizedQuery = query.trim()
        return if (normalizedQuery.isEmpty()) {
            PlaceSearchResult.Success(emptyList())
        } else {
            repository.search(normalizedQuery, proximity)
        }
    }

    fun abandonSession() = repository.abandonSession()
}
