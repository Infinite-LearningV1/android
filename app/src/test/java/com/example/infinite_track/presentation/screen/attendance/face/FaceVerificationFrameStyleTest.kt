package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceVerificationFrameStyleTest {

    @Test
    fun `no face is dashed with silhouette and no rail`() {
        val s = frameStyleFor(FaceScannerState(livenessState = LivenessState.DETECTING_FACE))

        assertTrue(s.dashed)
        assertTrue(s.showSilhouette)
        assertFalse(s.showRail)
        assertFalse(s.showCheckBadge)
    }

    @Test
    fun `liveness shows solid frame with rail`() {
        val s = frameStyleFor(
            FaceScannerState(livenessState = LivenessState.WAITING_FOR_LIVENESS, challengeIndex = 2)
        )

        assertFalse(s.dashed)
        assertTrue(s.showRail)
    }

    @Test
    fun `success is badge without rail`() {
        val s = frameStyleFor(FaceScannerState(livenessState = LivenessState.SUCCESS))

        assertTrue(s.showCheckBadge)
        assertFalse(s.showRail)
        assertFalse(s.dashed)
    }
}
