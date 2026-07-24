package com.example.infinite_track.presentation.map.model

import com.example.infinite_track.domain.model.location.GeoCoordinate

data class MapMarkerUiModel(
    val id: String,
    val role: MapMarkerRole,
    val category: MapMarkerCategory,
    val coordinate: GeoCoordinate,
    val title: String,
    val snippet: String?,
    val isSelected: Boolean = false
)

enum class MapMarkerRole {
    CURRENT_LOCATION,
    AUTHORITATIVE_TARGET,
    WFA_RECOMMENDATION,
    SEARCH_PREVIEW
}

enum class MapMarkerCategory {
    CURRENT_LOCATION,
    WFO,
    WFH,
    WFA
}
