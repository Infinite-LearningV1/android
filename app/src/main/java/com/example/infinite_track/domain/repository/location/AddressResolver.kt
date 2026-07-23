package com.example.infinite_track.domain.repository.location

import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate

interface AddressResolver {
    suspend fun resolve(coordinate: GeoCoordinate): AddressResolutionResult
}
