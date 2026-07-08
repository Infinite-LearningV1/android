package com.example.infinite_track.presentation.screen.attendance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceActionStateTest {

    @Test
    fun readyCheckIn_exposesEnabledCheckInBridge() {
        val state = AttendanceActionState.Ready(
            intent = AttendanceActionIntent.CHECK_IN,
            label = "Check-in di sini"
        )

        assertEquals("Check-in di sini", state.ctaLabel)
        assertTrue(state.isCtaEnabled)
        assertTrue(state.legacyIsCheckInMode)
    }

    @Test
    fun readyCheckOut_exposesEnabledCheckOutBridge() {
        val state = AttendanceActionState.Ready(
            intent = AttendanceActionIntent.CHECK_OUT,
            label = "Check-out di sini"
        )

        assertEquals("Check-out di sini", state.ctaLabel)
        assertTrue(state.isCtaEnabled)
        assertFalse(state.legacyIsCheckInMode)
    }

    @Test
    fun blocked_exposesDisabledBridgeAndReasonCopy() {
        val state = AttendanceActionState.Blocked(
            reason = AttendanceBlockReason.WFH_LOCATION_MISSING,
            title = "Lokasi WFH belum tersedia",
            message = "Lengkapi lokasi WFH sebelum absen dari rumah."
        )

        assertEquals("Lokasi WFH belum tersedia", state.ctaLabel)
        assertFalse(state.isCtaEnabled)
        assertTrue(state.legacyIsCheckInMode)
    }

    @Test
    fun completed_exposesHonestDisabledCompletedCopy() {
        val state = AttendanceActionState.Completed

        assertEquals("Anda sudah absen hari ini", state.ctaLabel)
        assertFalse(state.isCtaEnabled)
        assertFalse(state.legacyIsCheckInMode)
    }
}
