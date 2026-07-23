package com.example.infinite_track.presentation.map.mapper

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.map.model.MapCircleUiModel
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapUiState
import com.example.infinite_track.presentation.screen.attendance.AttendanceScreenState

object AttendanceMapUiMapper {
    fun map(
        state: AttendanceScreenState,
        hasPreciseLocationPermission: Boolean
    ): MapUiState {
        val markers = linkedMapOf<String, MapMarkerUiModel>()

        if (state.isWfaModeActive) {
            state.wfaRecommendations.forEach { recommendation ->
                val id = recommendationMarkerId(recommendation)
                markers[id] = MapMarkerUiModel(
                    id = id,
                    role = MapMarkerRole.WFA_RECOMMENDATION,
                    coordinate = recommendation.coordinate,
                    title = recommendation.name,
                    snippet = recommendation.address,
                    isSelected = state.selectedWfaLocation?.coordinate == recommendation.coordinate
                )
            }
        } else {
            state.wfoLocation?.let { location -> markers[locationMarkerId(MapMarkerRole.WFO, location)] = location.toMarker(MapMarkerRole.WFO) }
            state.wfhLocation?.let { location -> markers[locationMarkerId(MapMarkerRole.WFH, location)] = location.toMarker(MapMarkerRole.WFH) }
        }

        state.currentCoordinateOrNull()?.let { coordinate ->
            markers[CURRENT_USER_MARKER_ID] = MapMarkerUiModel(
                id = CURRENT_USER_MARKER_ID,
                role = MapMarkerRole.CURRENT_USER,
                coordinate = coordinate,
                title = "Lokasi saat ini",
                snippet = state.currentUserAddress.takeIf(String::isNotBlank)
            )
        }

        val selectedTarget = state.selectedTargetLocation?.location
        val selectedCoordinate = selectedTarget?.coordinate
        val selectedMarkers = markers.values.map { marker ->
            if (selectedCoordinate != null && marker.coordinate == selectedCoordinate) {
                marker.copy(isSelected = true)
            } else {
                marker
            }
        }

        val circles = selectedTarget?.let { target ->
            listOf(
                MapCircleUiModel(
                    id = "target-radius:${target.locationId}",
                    center = target.coordinate,
                    radius = DistanceMeters(target.radius.toDouble())
                )
            )
        }.orEmpty()

        return MapUiState(
            markers = selectedMarkers,
            circles = circles,
            hasPreciseLocationPermission = hasPreciseLocationPermission
        )
    }

    fun recommendationMarkerId(recommendation: WfaRecommendation): String {
        return "wfa:${recommendation.stableKey}"
    }

    fun locationMarkerId(role: MapMarkerRole, location: Location): String {
        return "${role.name.lowercase()}:${location.locationId}"
    }

    private fun Location.toMarker(role: MapMarkerRole): MapMarkerUiModel {
        return MapMarkerUiModel(
            id = locationMarkerId(role, this),
            role = role,
            coordinate = coordinate,
            title = description,
            snippet = category
        )
    }

    private fun AttendanceScreenState.currentCoordinateOrNull(): GeoCoordinate? {
        val latitude = currentUserLatitude ?: return null
        val longitude = currentUserLongitude ?: return null
        return runCatching { GeoCoordinate(latitude, longitude) }.getOrNull()
    }

    private const val CURRENT_USER_MARKER_ID = "current-user"
}
