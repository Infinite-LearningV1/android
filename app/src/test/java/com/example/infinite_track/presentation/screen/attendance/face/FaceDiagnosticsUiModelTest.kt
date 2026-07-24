package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceDiagnosticsUiModelTest {

    @Test
    fun `null when no score`() {
        assertNull(faceDiagnosticsUiModel(null, null, null))
        assertNull(faceDiagnosticsUiModel(0.2f, null, true))
    }

    @Test
    fun `formats matched score and threshold`() {
        val m = faceDiagnosticsUiModel(0.197f, 0.15f, true)!!

        assertEquals("0.197", m.similarityText)
        assertEquals("0.15", m.thresholdText)
        assertTrue(m.matched)
    }
}
