package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WfaMapSelectionEffectTest {
    @Test
    fun `selected WFA coordinate produces detail zoom focus`() {
        val coordinate = GeoCoordinate(-0.90, 119.88)

        val effect = WfaMapSelectionEffect.focus(
            id = 7L,
            coordinate = coordinate
        )

        assertEquals(7L, effect.id)
        assertEquals(17f, effect.zoom)
        assertEquals(coordinate, effect.coordinate)
    }

    @Test
    fun `explicit search preview prevents recommendation auto fit from superseding focus`() {
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = emptyList(),
                searchPreview = LocationResult(
                    placeName = "Pilihan terbaru",
                    address = "Palu",
                    latitude = -0.90,
                    longitude = 119.88
                )
            )
        )

        assertEquals(
            false,
            WfaMapSelectionEffect.shouldAutoFitRecommendations(preparation)
        )
        assertEquals(
            GeoCoordinate(-0.90, 119.88),
            WfaMapSelectionEffect.explicitSelectionCoordinate(preparation)
        )
    }

    @Test
    fun `recommendations may auto fit before any explicit WFA selection`() {
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            wfaDiscovery = WfaDiscoveryState.Content(recommendations = emptyList())
        )

        assertEquals(
            true,
            WfaMapSelectionEffect.shouldAutoFitRecommendations(preparation)
        )
    }

    @Test
    fun `selected recommendation is the persistent explicit WFA focus source`() {
        val recommendation = WfaRecommendation(
            stableKey = "selected",
            name = "Selected",
            address = "Palu",
            coordinate = GeoCoordinate(-0.91, 119.89),
            placeId = null,
            placeType = "cafe",
            distanceMeters = DistanceMeters(250.0),
            status = WfaRecommendationStatus.Ranked,
            finalRank = 1,
            finalScore = 90.0,
            finalLabel = "Sesuai",
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
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = listOf(recommendation),
                selectedKey = recommendation.stableKey
            )
        )

        assertEquals(
            recommendation.coordinate,
            WfaMapSelectionEffect.explicitSelectionCoordinate(preparation)
        )
    }

    @Test
    fun `WFA discovery selection is ignored outside WFA mode`() {
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFO,
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = emptyList(),
                searchPreview = LocationResult("Preview", "Palu", -0.90, 119.88)
            )
        )

        assertNull(WfaMapSelectionEffect.explicitSelectionCoordinate(preparation))
    }
}
