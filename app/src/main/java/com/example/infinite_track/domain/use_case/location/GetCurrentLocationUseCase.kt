package com.example.infinite_track.domain.use_case.location

import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val repository: CurrentLocationRepository
) {
    suspend operator fun invoke(): CurrentLocationResult = repository.getCurrentLocation()
}
