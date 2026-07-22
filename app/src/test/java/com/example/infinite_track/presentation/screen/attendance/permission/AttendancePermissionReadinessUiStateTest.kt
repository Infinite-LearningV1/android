package com.example.infinite_track.presentation.screen.attendance.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionReadinessUiStateTest {

    @Test
    fun `default state is a valid loading state`() {
        val state = AttendancePermissionReadinessUiState()

        assertTrue(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.requiredItems.isEmpty())
        assertTrue(state.optionalItems.isEmpty())
        assertEquals(3, state.requiredTotalCount)
        assertFalse(state.primaryActionEnabled)
    }

    @Test
    fun `typed state keeps required progress independent from optional items`() {
        val state = AttendancePermissionReadinessUiState(
            isLoading = false,
            requiredReadyCount = 2,
            requiredTotalCount = 3,
            optionalItems = emptyList(),
            canContinue = false
        )

        assertEquals(2, state.requiredReadyCount)
        assertEquals(3, state.requiredTotalCount)
        assertFalse(state.canContinue)
    }

    @Test
    fun `optional degradation does not override an approved continuation`() {
        val state = AttendancePermissionReadinessUiState(
            isLoading = false,
            requiredReadyCount = 3,
            requiredTotalCount = 3,
            canContinue = true,
            primaryActionLabel = "Lanjut ke Mode Kerja",
            primaryActionEnabled = true
        )

        assertTrue(state.canContinue)
        assertEquals("Lanjut ke Mode Kerja", state.primaryActionLabel)
    }
}
