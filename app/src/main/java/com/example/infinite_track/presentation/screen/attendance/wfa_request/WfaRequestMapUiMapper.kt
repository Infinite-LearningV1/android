package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRecommendationInfo
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapMarkerUiModel
import com.example.infinite_track.presentation.map.model.MapPermissionRequirement
import com.example.infinite_track.presentation.map.model.MapUiState

object WfaRequestMapUiMapper {
    fun map(
        currentCoordinate: GeoCoordinate?,
        recommendationState: WfaRequestRecommendationState,
        hasPreciseLocationPermission: Boolean
    ): MapUiState {
        val recommendations = (recommendationState as? WfaRequestRecommendationState.Content)
            ?.recommendations
            .orEmpty()
        val selectedKey = (recommendationState as? WfaRequestRecommendationState.Content)
            ?.selectedKey
        val markers = buildList {
            currentCoordinate?.let {
                add(
                    MapMarkerUiModel(
                        id = CURRENT_LOCATION_ID,
                        role = MapMarkerRole.CURRENT_LOCATION,
                        category = MapMarkerCategory.CURRENT_LOCATION,
                        coordinate = it,
                        title = "Lokasi Anda",
                        snippet = null
                    )
                )
            }
            recommendations.forEach { recommendation ->
                add(recommendation.toMarker(selectedKey == recommendation.stableKey))
            }
        }
        return MapUiState(
            markers = markers,
            circles = emptyList(),
            hasPreciseLocationPermission = hasPreciseLocationPermission,
            contentDescription = "Pratinjau peta rekomendasi lokasi WFA, hanya baca",
            permissionRequirement = MapPermissionRequirement.PreciseLocation,
            interactionMode = MapInteractionMode.ReadOnly
        )
    }

    fun recommendationMarkerId(recommendation: WfaRecommendation): String =
        "wfa:${recommendation.stableKey}"

    private fun WfaRecommendation.toMarker(selected: Boolean) = MapMarkerUiModel(
        id = recommendationMarkerId(this),
        role = MapMarkerRole.WFA_RECOMMENDATION,
        category = MapMarkerCategory.WFA,
        coordinate = coordinate,
        title = name,
        snippet = address,
        isSelected = selected,
        recommendationInfo = rankedInfoOrNull()
    )

    private fun WfaRecommendation.rankedInfoOrNull(): MapMarkerRecommendationInfo? {
        if (status != WfaRecommendationStatus.Ranked) return null
        val score = finalScore ?: return null
        val label = finalLabel ?: return null
        return MapMarkerRecommendationInfo(
            category = placeType,
            distance = distanceMeters,
            fuzzyAhpScore = score / PERCENT_SCALE,
            suitabilityLabel = label
        )
    }

    private const val CURRENT_LOCATION_ID = "wfa:current-location"
    private const val PERCENT_SCALE = 100.0
}
