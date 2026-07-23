package com.example.infinite_track.data.mapper.wfa

import com.example.infinite_track.data.soucre.network.response.Amenities
import com.example.infinite_track.data.soucre.network.response.CrowdDensity
import com.example.infinite_track.data.soucre.network.response.NoiseLevel
import com.example.infinite_track.data.soucre.network.response.OperationalHours
import com.example.infinite_track.data.soucre.network.response.RecommendationItem
import com.example.infinite_track.data.soucre.network.response.ScoreDetails
import com.example.infinite_track.data.soucre.network.response.WifiQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class WfaMapperTest {

    @Test
    fun `mapper preserves backend suitability and distance in meters`() {
        val result = recommendationItem(distanceFromCenter = 1250.0).toDomain()

        assertEquals(0.91, result.suitabilityScore, 0.0)
        assertEquals("Sangat sesuai", result.suitabilityLabel)
        assertEquals(1250.0, result.distanceMeters.value, 0.0)
        assertEquals("cafe@-0.900000,119.880000", result.stableKey)
    }

    private fun recommendationItem(distanceFromCenter: Double) = RecommendationItem(
        name = " Cafe ",
        address = "Palu",
        latitude = -0.9,
        longitude = 119.88,
        category = "Cafe",
        suitabilityScore = 0.91,
        suitabilityLabel = "Sangat sesuai",
        distanceFromCenter = distanceFromCenter,
        scoreDetails = ScoreDetails(
            wifiQuality = WifiQuality(0.0, "", ""),
            noiseLevel = NoiseLevel(0.0, "", ""),
            crowdDensity = CrowdDensity(0.0, "", ""),
            operationalHours = OperationalHours(0.0, "", ""),
            amenities = Amenities(0.0, "", emptyList())
        )
    )
}
