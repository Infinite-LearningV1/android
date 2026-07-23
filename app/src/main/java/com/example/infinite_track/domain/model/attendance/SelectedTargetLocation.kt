package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.location.GeoCoordinate

data class SelectedTargetLocation(
    val mode: WorkMode,
    val location: Location?,
    val displayName: String,
    val description: String?,
    val isAvailable: Boolean,
    val unavailableReason: String? = null
) {
    val coordinate: GeoCoordinate?
        get() = location?.coordinate
}
