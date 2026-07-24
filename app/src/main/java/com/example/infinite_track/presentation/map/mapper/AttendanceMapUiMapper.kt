package com.example.infinite_track.presentation.map.mapper

import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.map.model.MapCircleUiModel
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapUiState
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState

object AttendanceMapUiMapper {
    fun map(
        preparation: AttendancePreparationState,
        hasPreciseLocationPermission: Boolean
    ): MapUiState {
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

            val resolvedTarget = preparation.targetResolution as? TargetLocationResolution.Resolved
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

            val discovery = preparation.wfaDiscovery as? WfaDiscoveryState.Content
            discovery?.recommendations?.forEach { recommendation ->
                add(
                    MapMarkerUiModel(
                        id = recommendationMarkerId(recommendation),
                        role = MapMarkerRole.WFA_RECOMMENDATION,
                        category = MapMarkerCategory.WFA,
                        coordinate = recommendation.coordinate,
                        title = recommendation.name,
                        snippet = recommendation.address,
                        isSelected = recommendation.stableKey == discovery.selectedKey
                    )
                )
            }
            discovery?.searchPreview?.let { preview ->
                runCatching { GeoCoordinate(preview.latitude, preview.longitude) }
                    .getOrNull()
                    ?.let { coordinate ->
                        add(
                            MapMarkerUiModel(
                                id = searchPreviewMarkerId(preview),
                                role = MapMarkerRole.SEARCH_PREVIEW,
                                category = MapMarkerCategory.WFA,
                                coordinate = coordinate,
                                title = preview.placeName,
                                snippet = preview.address,
                                isSelected = true
                            )
                        )
                    }
            }
        }

        val circles = (preparation.targetResolution as? TargetLocationResolution.Resolved)
            ?.target
            ?.let { target ->
                listOf(
                    MapCircleUiModel(
                        id = "target-radius:${target.targetId.value}",
                        center = target.coordinate,
                        radius = target.radius
                    )
                )
            }
            .orEmpty()

        return MapUiState(
            markers = markers,
            circles = circles,
            hasPreciseLocationPermission = hasPreciseLocationPermission
        )
    }

    fun recommendationMarkerId(recommendation: WfaRecommendation): String =
        "wfa:${recommendation.stableKey}"

    fun searchPreviewMarkerId(preview: LocationResult): String {
        val placeIdentity = preview.placeId?.trim()?.takeIf(String::isNotEmpty)
        if (placeIdentity != null) {
            return "$SEARCH_PREVIEW_MARKER_PREFIX:place:$placeIdentity"
        }

        return buildString {
            append(SEARCH_PREVIEW_MARKER_PREFIX)
            append(":coordinate:")
            append(preview.latitude.toBits().toString(16))
            append(':')
            append(preview.longitude.toBits().toString(16))
        }
    }

    private fun CurrentLocationResult?.successfulCoordinateOrNull(): GeoCoordinate? =
        (this as? CurrentLocationResult.Success)
            ?.location
            ?.coordinate

    private fun WorkMode.toMarkerCategory(): MapMarkerCategory = when (this) {
        WorkMode.WFO -> MapMarkerCategory.WFO
        WorkMode.WFH -> MapMarkerCategory.WFH
        WorkMode.WFA -> MapMarkerCategory.WFA
    }

    private const val CURRENT_LOCATION_MARKER_ID = "current-location"
    private const val SEARCH_PREVIEW_MARKER_PREFIX = "search-preview"
}
