package com.example.infinite_track.presentation.map.model

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate

enum class MapPermissionRequirement { PreciseLocation, None }

enum class MapInteractionMode { Interactive, ReadOnly }

data class MapUiState(
    val markers: List<MapMarkerUiModel> = emptyList(),
    val circles: List<MapCircleUiModel> = emptyList(),
    val hasPreciseLocationPermission: Boolean = false,
    val contentDescription: String = "Peta lokasi attendance",
    val permissionRequirement: MapPermissionRequirement = MapPermissionRequirement.PreciseLocation,
    val interactionMode: MapInteractionMode = MapInteractionMode.Interactive
)

data class MapCircleUiModel(
    val id: String,
    val center: GeoCoordinate,
    val radius: DistanceMeters
)
