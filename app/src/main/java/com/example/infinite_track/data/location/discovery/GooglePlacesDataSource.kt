package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GooglePlacesDataSource @Inject constructor(
    private val clientProvider: GooglePlacesClientProvider,
    private val requestFactory: GooglePlacesRequestFactory
) {
    suspend fun findPredictions(
        query: String,
        proximity: GeoCoordinate?,
        sessionToken: AutocompleteSessionToken
    ): List<AutocompletePrediction> {
        val cancellation = CancellationTokenSource()
        val request = requestFactory.autocomplete(
            query = query,
            proximity = proximity,
            token = sessionToken,
            cancellationToken = cancellation.token
        )

        return clientProvider.get()
            .findAutocompletePredictions(request)
            .await(cancellation)
            .autocompletePredictions
    }

    suspend fun fetchPlace(
        placeId: String,
        sessionToken: AutocompleteSessionToken
    ): Place {
        val cancellation = CancellationTokenSource()
        val request = requestFactory.details(
            placeId = placeId,
            token = sessionToken,
            cancellationToken = cancellation.token
        )

        return clientProvider.get().fetchPlace(request).await(cancellation).place
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
