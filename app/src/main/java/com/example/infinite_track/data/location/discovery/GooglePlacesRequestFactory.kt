package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import javax.inject.Inject

class GooglePlacesRequestFactory @Inject constructor() {
    fun autocomplete(
        query: String,
        proximity: GeoCoordinate?,
        token: AutocompleteSessionToken,
        cancellationToken: CancellationToken?
    ): FindAutocompletePredictionsRequest {
        val builder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("ID")
            .setSessionToken(token)

        proximity?.let {
            val origin = LatLng(it.latitude, it.longitude)
            builder
                .setOrigin(origin)
                .setLocationBias(CircularBounds.newInstance(origin, LOCATION_BIAS_METERS))
        }
        cancellationToken?.let(builder::setCancellationToken)
        return builder.build()
    }

    fun details(
        placeId: String,
        token: AutocompleteSessionToken,
        cancellationToken: CancellationToken?
    ): FetchPlaceRequest {
        val builder = FetchPlaceRequest.builder(
            placeId,
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
            .setSessionToken(token)
        cancellationToken?.let(builder::setCancellationToken)
        return builder.build()
    }

    private companion object {
        const val LOCATION_BIAS_METERS = 50_000.0
    }
}
