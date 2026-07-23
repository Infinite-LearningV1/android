package com.example.infinite_track.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionNavigationTest {
    @Test
    fun `ready navigation removes readiness destination and is single top`() {
        val options = attendanceReadyNavOptions()

        assertEquals(Screen.AttendancePermissionReadiness.route, options.popUpToRoute)
        assertTrue(options.isPopUpToInclusive())
        assertTrue(options.shouldLaunchSingleTop())
    }
}
