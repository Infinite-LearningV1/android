package com.example.infinite_track.domain.repository.location

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceSearchResult

interface PlaceDiscoveryRepository {
    suspend fun search(
        query: String,
        proximity: GeoCoordinate? = null
    ): PlaceSearchResult

    suspend fun resolve(placeId: String): PlaceDetailsResult

    fun abandonSession()
}
