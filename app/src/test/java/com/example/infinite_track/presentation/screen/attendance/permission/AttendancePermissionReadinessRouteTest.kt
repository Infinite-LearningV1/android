package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionRequestOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendancePermissionReadinessRouteTest {

    @Test
    fun `granted callback remains granted`() {
        assertEquals(
            AttendancePermissionRequestOutcome.GRANTED,
            classifyPermissionOutcome(
                granted = true,
                wasRequested = true,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `first observation is never classified as permanent denial`() {
        assertEquals(
            AttendancePermissionRequestOutcome.DENIED,
            classifyPermissionOutcome(
                granted = false,
                wasRequested = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `known denial with rationale remains recoverable`() {
        assertEquals(
            AttendancePermissionRequestOutcome.DENIED,
            classifyPermissionOutcome(
                granted = false,
                wasRequested = true,
                shouldShowRationale = true
            )
        )
    }

    @Test
    fun `known denial without rationale is permanent`() {
        assertEquals(
            AttendancePermissionRequestOutcome.PERMANENTLY_DENIED,
            classifyPermissionOutcome(
                granted = false,
                wasRequested = true,
                shouldShowRationale = false
            )
        )
    }
}
