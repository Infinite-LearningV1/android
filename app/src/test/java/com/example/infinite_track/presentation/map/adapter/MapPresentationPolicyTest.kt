package com.example.infinite_track.presentation.map.adapter

import com.example.infinite_track.presentation.map.model.MapInteractionMode
import com.example.infinite_track.presentation.map.model.MapPermissionRequirement
import com.example.infinite_track.presentation.map.model.MapUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPresentationPolicyTest {

    @Test
    fun `existing map defaults remain permission gated and interactive`() {
        val state = MapUiState()

        assertEquals(MapPermissionRequirement.PreciseLocation, state.permissionRequirement)
        assertEquals(MapInteractionMode.Interactive, state.interactionMode)
        assertTrue(state.interactionMode.toMapUiSettings().scrollGesturesEnabled)
    }

    @Test
    fun `read only mode disables all map gestures and toolbar`() {
        val settings = MapInteractionMode.ReadOnly.toMapUiSettings()

        assertFalse(settings.scrollGesturesEnabled)
        assertFalse(settings.zoomGesturesEnabled)
        assertFalse(settings.rotationGesturesEnabled)
        assertFalse(settings.tiltGesturesEnabled)
        assertFalse(settings.mapToolbarEnabled)
        assertFalse(settings.myLocationButtonEnabled)
        assertFalse(settings.zoomControlsEnabled)
    }

    @Test
    fun `read only marker click is consumed without dispatch`() {
        assertFalse(MapInteractionMode.ReadOnly.shouldDispatchMarkerClick())
        assertTrue(MapInteractionMode.ReadOnly.shouldConsumeMarkerClick())
        assertTrue(MapInteractionMode.Interactive.shouldDispatchMarkerClick())
        assertTrue(MapInteractionMode.Interactive.shouldConsumeMarkerClick())
    }
}
