package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionTimelinePillColorTest {

    @Test
    fun subduedColorMultipliesExistingAlpha() {
        val color = Color(red = 0.2f, green = 0.4f, blue = 0.6f, alpha = 0.2f)

        val subdued = color.withMultipliedAlpha(0.55f)

        assertEquals(color.alpha * 0.55f, subdued.alpha, 0.001f)
        assertEquals(color.red, subdued.red, 0.0001f)
        assertEquals(color.green, subdued.green, 0.0001f)
        assertEquals(color.blue, subdued.blue, 0.0001f)
    }
}
