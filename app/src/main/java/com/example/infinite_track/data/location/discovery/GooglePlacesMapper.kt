package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceDetails
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import javax.inject.Inject

class GooglePlacesMapper @Inject constructor() {
    fun toSuggestion(prediction: AutocompletePrediction): PlaceSuggestion {
        return PlaceSuggestion(
            placeId = prediction.placeId,
            primaryText = prediction.getPrimaryText(null).toString(),
            secondaryText = prediction.getSecondaryText(null).toString().ifBlank { null },
            distance = prediction.distanceMeters?.toDouble()?.let(::DistanceMeters)
        )
    }

    fun toDetails(place: Place, suggestion: PlaceSuggestion): PlaceDetails? {
        val id = place.id?.takeIf(String::isNotBlank) ?: return null
        val coordinate = place.latLng ?: return null
        return PlaceDetails(
            placeId = id,
            displayName = suggestion.primaryText,
            formattedAddress = place.address ?: suggestion.secondaryText,
            coordinate = GeoCoordinate(coordinate.latitude, coordinate.longitude)
        )
    }
}
