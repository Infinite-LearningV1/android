package com.example.infinite_track.presentation.components.map

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRole
import com.example.infinite_track.presentation.map.model.MapPermissionRequirement
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadOnlyLocationMapModelTest {

    @Test
    fun `preview contains selected WFA marker server radius and read only policies`() {
        val state = buildReadOnlyLocationMapState(
            coordinate = GeoCoordinate(-0.89, 119.87),
            radiusMeters = 100,
            title = "Kafe Taman",
            address = "Jl. Merdeka 10, Palu",
            contentDescription = "Peta Kafe Taman"
        )

        assertEquals(MapPermissionRequirement.None, state.permissionRequirement)
        assertEquals(MapInteractionMode.ReadOnly, state.interactionMode)
        assertEquals(MapMarkerRole.AUTHORITATIVE_TARGET, state.markers.single().role)
        assertEquals(MapMarkerCategory.WFA, state.markers.single().category)
        assertEquals(DistanceMeters(100.0), state.circles.single().radius)
    }
}
