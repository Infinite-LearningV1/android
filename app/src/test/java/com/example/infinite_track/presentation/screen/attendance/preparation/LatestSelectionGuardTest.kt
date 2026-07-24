package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestSelectionGuardTest {

    @Test
    fun `guard rejects result from previous mode selection`() {
        val guard = LatestSelectionGuard()
        val wfaRequest = guard.next(WorkMode.WFA)
        guard.next(WorkMode.WFO)

        assertFalse(guard.isCurrent(wfaRequest, WorkMode.WFA))
    }

    @Test
    fun `guard accepts only newest request even when mode is unchanged`() {
        val guard = LatestSelectionGuard()
        val firstRequest = guard.next(WorkMode.WFA)
        val newestRequest = guard.next(WorkMode.WFA)

        assertFalse(guard.isCurrent(firstRequest, WorkMode.WFA))
        assertTrue(guard.isCurrent(newestRequest, WorkMode.WFA))
        assertFalse(guard.isCurrent(newestRequest, WorkMode.WFO))
    }
}
