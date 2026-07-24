package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
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
}
