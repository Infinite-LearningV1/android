package com.example.infinite_track.domain.model.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoBoundsTest {

    @Test
    fun `calculates center for valid bounds`() {
        val bounds = GeoBounds(
            southWest = GeoCoordinate(-1.0, 119.0),
            northEast = GeoCoordinate(1.0, 121.0)
        )

        assertEquals(GeoCoordinate(0.0, 120.0), bounds.center)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects inverted latitude bounds`() {
        GeoBounds(
            southWest = GeoCoordinate(1.0, 119.0),
            northEast = GeoCoordinate(-1.0, 121.0)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects inverted longitude bounds`() {
        GeoBounds(
            southWest = GeoCoordinate(-1.0, 121.0),
            northEast = GeoCoordinate(1.0, 119.0)
        )
    }
}
