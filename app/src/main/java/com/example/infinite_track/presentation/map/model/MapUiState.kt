package com.example.infinite_track.presentation.map.model

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate

data class MapUiState(
    val markers: List<MapMarkerUiModel> = emptyList(),
    val circles: List<MapCircleUiModel> = emptyList(),
    val hasPreciseLocationPermission: Boolean = false,
    val contentDescription: String = "Peta lokasi attendance"
)

data class MapCircleUiModel(
    val id: String,
    val center: GeoCoordinate,
    val radius: DistanceMeters
)
