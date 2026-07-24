package com.example.infinite_track.presentation.screen.attendance

import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePreparationStateOwnershipTest {

    @Test
    fun `screen state has one preparation owner`() {
        val names = AttendanceScreenState::class.java.declaredFields.map { it.name }.toSet()

        assertTrue("preparation" in names)
        assertTrue(
            setOf(
                "targetLocation",
                "wfoLocation",
                "wfhLocation",
                "approvedWfaLocation",
                "selectedTargetLocation",
                "targetLocationMarker",
                "selectedWfaLocation",
                "selectedWfaMarkerInfo",
                "pickedLocation",
                "buttonText",
                "isButtonEnabled",
                "isCheckInMode"
            ).none(names::contains)
        )
    }
}
