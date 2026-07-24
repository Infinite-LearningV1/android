package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
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

    @Test
    fun `explicit search preview prevents recommendation auto fit from superseding focus`() {
        val discovery = WfaDiscoveryState.Content(
            recommendations = emptyList(),
            searchPreview = LocationResult(
                placeName = "Pilihan terbaru",
                address = "Palu",
                latitude = -0.90,
                longitude = 119.88
            )
        )

        assertEquals(
            false,
            WfaMapSelectionEffect.shouldAutoFitRecommendations(discovery)
        )
    }

    @Test
    fun `recommendations may auto fit before any explicit WFA selection`() {
        val discovery = WfaDiscoveryState.Content(recommendations = emptyList())

        assertEquals(
            true,
            WfaMapSelectionEffect.shouldAutoFitRecommendations(discovery)
        )
    }
}
