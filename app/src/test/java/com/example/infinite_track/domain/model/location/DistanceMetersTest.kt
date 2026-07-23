package com.example.infinite_track.domain.model.location

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceMetersTest {

    @Test
    fun `accepts zero and positive distances`() {
        assertEquals(0.0, DistanceMeters.Zero.value, 0.0)
        assertEquals(125.5, DistanceMeters(125.5).value, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative distance`() {
        DistanceMeters(-0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non finite distance`() {
        DistanceMeters(Double.POSITIVE_INFINITY)
    }
}
