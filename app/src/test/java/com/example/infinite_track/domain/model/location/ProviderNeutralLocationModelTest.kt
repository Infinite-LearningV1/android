package com.example.infinite_track.domain.model.location

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderNeutralLocationModelTest {

    @Test
    fun `attendance location owns provider neutral coordinate`() {
        val location = Location(
            locationId = 1,
            description = "Office",
            coordinate = GeoCoordinate(-0.89, 119.87),
            radius = 100,
            category = "WFO"
        )

        assertEquals(GeoCoordinate(-0.89, 119.87), location.coordinate)
        assertEquals(-0.89, location.latitude, 0.0)
        assertEquals(119.87, location.longitude, 0.0)
    }

    @Test
    fun `wfa recommendation owns provider neutral coordinate`() {
        val recommendation = WfaRecommendation(
            stableKey = "cafe@-0.900000,119.880000",
            name = "Cafe",
            address = "Palu",
            coordinate = GeoCoordinate(-0.90, 119.88),
            placeId = null,
            placeType = "Cafe",
            distanceMeters = DistanceMeters(1_000.0),
            status = WfaRecommendationStatus.Ranked,
            finalRank = 1,
            finalScore = 90.0,
            finalLabel = "Good",
            facilityScore = null,
            facilityConfidence = 0,
            facilities = unknownFacilities()
        )

        assertEquals(GeoCoordinate(-0.90, 119.88), recommendation.coordinate)
        assertEquals(-0.90, recommendation.latitude, 0.0)
        assertEquals(119.88, recommendation.longitude, 0.0)
    }

    private fun unknownFacilities() = WfaFacilityEvidence(
        internetAccess = WfaFacilityAvailability.UNKNOWN,
        openingHours = WfaFacilityAvailability.UNKNOWN,
        toilets = WfaFacilityAvailability.UNKNOWN,
        airConditioning = WfaFacilityAvailability.UNKNOWN,
        wheelchairAccessibility = WfaFacilityAvailability.UNKNOWN
    )
}
