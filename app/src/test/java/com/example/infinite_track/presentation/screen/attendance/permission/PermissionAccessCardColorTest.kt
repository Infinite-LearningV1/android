package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PermissionAccessCardColorTest {

    @Test
    fun incompleteAccessUsesSolidDefaultSurface() {
        val surface = Color.White

        val result = permissionAccessContainerColor(
            isReady = false,
            stateContainer = Color.Green.copy(alpha = 0.16f),
            surface = surface
        )

        assertEquals(surface, result)
        assertEquals(1f, result.alpha, 0f)
    }

    @Test
    fun readyAccessUsesOpaqueStateTint() {
        val surface = Color.White

        val result = permissionAccessContainerColor(
            isReady = true,
            stateContainer = Color.Green.copy(alpha = 0.16f),
            surface = surface
        )

        assertNotEquals(surface, result)
        assertEquals(1f, result.alpha, 0f)
    }
}
