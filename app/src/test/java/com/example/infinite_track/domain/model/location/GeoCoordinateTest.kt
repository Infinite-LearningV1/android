package com.example.infinite_track.domain.model.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoCoordinateTest {

    @Test
    fun `accepts inclusive geographic boundaries`() {
        assertEquals(-90.0, GeoCoordinate(-90.0, -180.0).latitude, 0.0)
        assertEquals(180.0, GeoCoordinate(90.0, 180.0).longitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects latitude outside geographic range`() {
        GeoCoordinate(90.0001, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects longitude outside geographic range`() {
        GeoCoordinate(0.0, -180.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non finite latitude`() {
        GeoCoordinate(Double.NaN, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non finite longitude`() {
        GeoCoordinate(0.0, Double.POSITIVE_INFINITY)
    }
}
