package com.example.infinite_track.presentation.map.model

import com.example.infinite_track.domain.model.location.GeoCoordinate

sealed interface MapCameraEffect {
    val id: Long

    data class Focus(
        override val id: Long,
        val coordinate: GeoCoordinate,
        val zoom: Float = 15f
    ) : MapCameraEffect

    data class Fit(
        override val id: Long,
        val coordinates: List<GeoCoordinate>
    ) : MapCameraEffect
}
