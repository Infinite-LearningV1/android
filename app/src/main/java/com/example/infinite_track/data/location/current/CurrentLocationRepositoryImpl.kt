package com.example.infinite_track.data.location.current

import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CurrentLocationRepositoryImpl @Inject constructor(
    private val dataSource: CurrentLocationDataSource,
    private val mapper: AndroidLocationMapper
) : CurrentLocationRepository {

    override suspend fun getCurrentLocation(): CurrentLocationResult {
        return try {
            val snapshot = dataSource.getCurrentLocation()
                ?: return CurrentLocationResult.Failure.Unavailable

            try {
                CurrentLocationResult.Success(mapper.toDomain(snapshot))
            } catch (_: IllegalArgumentException) {
                CurrentLocationResult.Failure.InvalidCoordinate
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SecurityException) {
            CurrentLocationResult.Failure.PermissionDenied
        } catch (_: Throwable) {
            CurrentLocationResult.Failure.ProviderError
        }
    }
}
