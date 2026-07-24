package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.MapCameraEffect

internal object WfaMapSelectionEffect {
    fun focus(id: Long, coordinate: GeoCoordinate): MapCameraEffect.Focus =
        MapCameraEffect.Focus(
            id = id,
            coordinate = coordinate,
            zoom = 17f
        )
}
