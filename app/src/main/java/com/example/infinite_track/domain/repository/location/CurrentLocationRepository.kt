package com.example.infinite_track.domain.repository.location

import com.example.infinite_track.domain.model.location.CurrentLocationResult

interface CurrentLocationRepository {
    suspend fun getCurrentLocation(): CurrentLocationResult
}
