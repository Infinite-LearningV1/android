package com.example.infinite_track.presentation.map.model

import com.example.infinite_track.domain.model.location.GeoCoordinate

sealed interface AttendanceMapEvent {
    object Ready : AttendanceMapEvent
    data class MarkerClicked(val marker: MapMarkerUiModel) : AttendanceMapEvent
    data class CameraIdle(
        val center: GeoCoordinate,
        val origin: AttendanceMapCameraMoveOrigin
    ) : AttendanceMapEvent
}

enum class AttendanceMapCameraMoveOrigin {
    USER_GESTURE,
    PROGRAMMATIC,
    UNKNOWN
}
