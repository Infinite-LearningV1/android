package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaRequestMapUiMapperTest {

    @Test
    fun `map contains current and recommendation markers without circles`() {
        val result = WfaRequestMapUiMapper.map(
            currentCoordinate = GeoCoordinate(-0.9, 119.87),
            recommendationState = contentState(),
            hasPreciseLocationPermission = true
        )

        assertEquals(1, result.markers.count { it.role == MapMarkerRole.CURRENT_LOCATION })
        assertEquals(2, result.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
        assertTrue(result.circles.isEmpty())
        assertEquals(MapInteractionMode.ReadOnly, result.interactionMode)
    }

    @Test
    fun `selected marker and ids use recommendation stable key`() {
        val result = WfaRequestMapUiMapper.map(
            currentCoordinate = null,
            recommendationState = contentState(selectedKey = "place-2"),
            hasPreciseLocationPermission = false
        )

        assertEquals("wfa:place-1", result.markers[0].id)
        assertEquals("wfa:place-2", result.markers[1].id)
        assertEquals("wfa:place-2", result.markers.single { it.isSelected }.id)
    }

    private fun contentState(selectedKey: String? = null) = WfaRequestRecommendationState.Content(
        recommendations = listOf(recommendation("place-1"), recommendation("place-2")),
        selectedKey = selectedKey
    )

    private fun recommendation(key: String) = WfaRecommendation(
        stableKey = key,
        placeId = key,
        name = "Tempat $key",
        address = "Palu",
        coordinate = GeoCoordinate(-0.901, 119.878 + key.last().digitToInt() / 1000.0),
        placeType = "cafe",
        distanceMeters = DistanceMeters(100.0),
        status = WfaRecommendationStatus.Ranked,
        finalRank = 1,
        finalScore = 88.0,
        finalLabel = "Direkomendasikan",
        facilityScore = null,
        facilityConfidence = 0,
        facilities = WfaFacilityEvidence(
            internetAccess = WfaFacilityAvailability.UNKNOWN,
            openingHours = WfaFacilityAvailability.UNKNOWN,
            toilets = WfaFacilityAvailability.UNKNOWN,
            airConditioning = WfaFacilityAvailability.UNKNOWN,
            wheelchairAccessibility = WfaFacilityAvailability.UNKNOWN
        )
    )
}
