package com.example.infinite_track.presentation.map.mapper

import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.MapCircleUiModel
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapUiState
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState

object AttendanceMapUiMapper {
    fun map(
        preparation: AttendancePreparationState,
        hasPreciseLocationPermission: Boolean
    ): MapUiState {
        val resolvedTarget = preparation.targetResolution as? TargetLocationResolution.Resolved
        val markers = buildList {
            preparation.currentLocation.successfulCoordinateOrNull()?.let { coordinate ->
                add(
                    MapMarkerUiModel(
                        id = CURRENT_LOCATION_MARKER_ID,
                        role = MapMarkerRole.CURRENT_LOCATION,
                        category = MapMarkerCategory.CURRENT_LOCATION,
                        coordinate = coordinate,
                        title = "Lokasi saat ini",
                        snippet = null
                    )
                )
            }
            resolvedTarget?.target?.let { target ->
                add(
                    MapMarkerUiModel(
                        id = "authoritative-target:${target.targetId.value}",
                        role = MapMarkerRole.AUTHORITATIVE_TARGET,
                        category = target.mode.toMarkerCategory(),
                        coordinate = target.coordinate,
                        title = target.displayName,
                        snippet = "Radius ${target.radius.value.toInt()} m",
                        isSelected = true
                    )
                )
            }
        }
        val circles = resolvedTarget?.target?.let { target ->
            listOf(
                MapCircleUiModel(
                    id = "target-radius:${target.targetId.value}",
                    center = target.coordinate,
                    radius = target.radius
                )
            )
        }.orEmpty()
        return MapUiState(
            markers = markers,
            circles = circles,
            hasPreciseLocationPermission = hasPreciseLocationPermission
        )
    }

    private fun CurrentLocationResult?.successfulCoordinateOrNull(): GeoCoordinate? =
        (this as? CurrentLocationResult.Success)?.location?.coordinate

    private fun WorkMode.toMarkerCategory(): MapMarkerCategory = when (this) {
        WorkMode.WFO -> MapMarkerCategory.WFO
        WorkMode.WFH -> MapMarkerCategory.WFH
        WorkMode.WFA -> MapMarkerCategory.WFA
    }

    private const val CURRENT_LOCATION_MARKER_ID = "current-location"
}
