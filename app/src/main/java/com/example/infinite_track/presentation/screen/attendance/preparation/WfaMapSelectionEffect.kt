package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.MapCameraEffect

internal object WfaMapSelectionEffect {
    fun focus(id: Long, coordinate: GeoCoordinate): MapCameraEffect.Focus =
        MapCameraEffect.Focus(
            id = id,
            coordinate = coordinate,
            zoom = 17f
        )

    fun explicitSelectionCoordinate(
        preparation: AttendancePreparationState
    ): GeoCoordinate? {
        if (preparation.selectedMode != WorkMode.WFA) return null
        val discovery = preparation.wfaDiscovery as? WfaDiscoveryState.Content ?: return null
        val selectedRecommendation = discovery.selectedKey?.let { selectedKey ->
            discovery.recommendations.firstOrNull { it.stableKey == selectedKey }
        }
        return selectedRecommendation?.coordinate
            ?: discovery.searchPreview?.let { preview ->
                runCatching {
                    GeoCoordinate(preview.latitude, preview.longitude)
                }.getOrNull()
            }
    }

    fun shouldAutoFitRecommendations(
        preparation: AttendancePreparationState
    ): Boolean = explicitSelectionCoordinate(preparation) == null
}
