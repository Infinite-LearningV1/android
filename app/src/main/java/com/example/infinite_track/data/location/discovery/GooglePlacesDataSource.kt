package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

class GooglePlacesDataSource @Inject constructor(
    private val clientProvider: GooglePlacesClientProvider
) {
    suspend fun findPredictions(
        query: String,
        proximity: GeoCoordinate?,
        sessionToken: AutocompleteSessionToken
    ): List<AutocompletePrediction> {
        val cancellation = CancellationTokenSource()
        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("ID")
            .setSessionToken(sessionToken)
            .setCancellationToken(cancellation.token)

        if (proximity != null) {
            requestBuilder.setLocationBias(proximity.toBias())
            requestBuilder.setOrigin(LatLng(proximity.latitude, proximity.longitude))
        }

        return clientProvider.get()
            .findAutocompletePredictions(requestBuilder.build())
            .await(cancellation)
            .autocompletePredictions
    }

    suspend fun fetchPlace(
        placeId: String,
        sessionToken: AutocompleteSessionToken
    ): Place {
        val cancellation = CancellationTokenSource()
        val request = FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
            .setSessionToken(sessionToken)
            .setCancellationToken(cancellation.token)
            .build()

        return clientProvider.get().fetchPlace(request).await(cancellation).place
    }

    private fun GeoCoordinate.toBias(): RectangularBounds {
        val latitudeDelta = 0.15
        val longitudeDelta = 0.15
        return RectangularBounds.newInstance(
            LatLng(
                max(GeoCoordinate.MIN_LATITUDE, latitude - latitudeDelta),
                max(GeoCoordinate.MIN_LONGITUDE, longitude - longitudeDelta)
            ),
            LatLng(
                min(GeoCoordinate.MAX_LATITUDE, latitude + latitudeDelta),
                min(GeoCoordinate.MAX_LONGITUDE, longitude + longitudeDelta)
            )
        )
    }

    private suspend fun <T> Task<T>.await(cancellation: CancellationTokenSource): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { value ->
                if (continuation.isActive) continuation.resume(value)
            }
            addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            addOnCanceledListener { continuation.cancel() }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }
}
