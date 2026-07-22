package com.example.infinite_track.presentation.geofencing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofencePermissionContractTest {

    @Test
    fun `precise foreground and background allow automatic monitoring`() {
        val result = GeofencePermissionContract.evaluate(
            hasPreciseForegroundLocation = true,
            hasBackgroundLocation = true
        )

        assertEquals(GeofencePermissionCapability.READY, result.capability)
        assertTrue(result.canRegisterAutomaticMonitoring)
    }

    @Test
    fun `missing precise foreground blocks automatic geofence registration`() {
        val result = GeofencePermissionContract.evaluate(
            hasPreciseForegroundLocation = false,
            hasBackgroundLocation = true
        )

        assertEquals(
            GeofencePermissionCapability.PRECISE_FOREGROUND_REQUIRED,
            result.capability
        )
        assertFalse(result.canRegisterAutomaticMonitoring)
        assertTrue(result.message.contains("lokasi presisi", ignoreCase = true))
        assertFalse(result.message.contains("coarse", ignoreCase = true))
    }

    @Test
    fun `missing background degrades monitoring without blocking manual attendance`() {
        val result = GeofencePermissionContract.evaluate(
            hasPreciseForegroundLocation = true,
            hasBackgroundLocation = false
        )

        assertEquals(
            GeofencePermissionCapability.BACKGROUND_LOCATION_DEGRADED,
            result.capability
        )
        assertFalse(result.canRegisterAutomaticMonitoring)
        assertTrue(result.message.contains("absensi manual", ignoreCase = true))
    }
}
