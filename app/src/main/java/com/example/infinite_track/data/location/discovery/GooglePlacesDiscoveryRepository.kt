package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceDiscoveryFailure
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GooglePlacesDiscoveryRepository @Inject constructor(
    private val dataSource: GooglePlacesDataSource,
    private val mapper: GooglePlacesMapper,
    private val sessions: GooglePlacesSessionManager
) : PlaceDiscoveryRepository {

    override suspend fun search(
        query: String,
        proximity: GeoCoordinate?
    ): PlaceSearchResult {
        return try {
            val suggestions = dataSource.findPredictions(query, proximity, sessions.current())
                .map(mapper::toSuggestion)
            PlaceSearchResult.Success(suggestions)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            PlaceSearchResult.Failure(error.toDiscoveryFailure())
        }
    }

    override suspend fun resolve(placeId: String): PlaceDetailsResult {
        return try {
            val details = mapper.toDetails(dataSource.fetchPlace(placeId, sessions.current()))
                ?: return PlaceDetailsResult.Failure(PlaceDiscoveryFailure.UNAVAILABLE)
            PlaceDetailsResult.Success(details)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            PlaceDetailsResult.Failure(error.toDiscoveryFailure())
        } finally {
            sessions.renew()
        }
    }

    override fun abandonSession() {
        sessions.renew()
    }

    private fun Throwable.toDiscoveryFailure(): PlaceDiscoveryFailure {
        if (this is PlacesConfigurationException) return PlaceDiscoveryFailure.CONFIGURATION
        val statusCode = (this as? ApiException)?.statusCode
            ?: return PlaceDiscoveryFailure.UNAVAILABLE

        return when (statusCode) {
            PlacesStatusCodes.REQUEST_DENIED,
            CommonStatusCodes.DEVELOPER_ERROR -> PlaceDiscoveryFailure.AUTHENTICATION

            PlacesStatusCodes.OVER_QUERY_LIMIT -> PlaceDiscoveryFailure.QUOTA
            CommonStatusCodes.NETWORK_ERROR,
            CommonStatusCodes.API_NOT_CONNECTED,
            CommonStatusCodes.TIMEOUT -> PlaceDiscoveryFailure.NETWORK

            PlacesStatusCodes.INVALID_REQUEST -> PlaceDiscoveryFailure.INVALID_REQUEST
            else -> PlaceDiscoveryFailure.UNAVAILABLE
        }
    }
}
