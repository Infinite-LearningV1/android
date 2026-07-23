package com.example.infinite_track.domain.model.location

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
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
            name = "Cafe",
            address = "Palu",
            coordinate = GeoCoordinate(-0.90, 119.88),
            score = 0.9,
            label = "Good",
            category = "Cafe",
            distance = 1.0
        )

        assertEquals(GeoCoordinate(-0.90, 119.88), recommendation.coordinate)
        assertEquals(-0.90, recommendation.latitude, 0.0)
        assertEquals(119.88, recommendation.longitude, 0.0)
    }
}
