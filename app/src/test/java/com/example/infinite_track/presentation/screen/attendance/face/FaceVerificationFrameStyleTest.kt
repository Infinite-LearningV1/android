package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class FaceVerificationFrameStyleTest {

    private fun styleOf(
        state: LivenessState,
        readyToVerify: Boolean = false
    ): FrameStyle = frameStyleFor(
        FaceScannerState(livenessState = state, readyToVerify = readyToVerify)
    )

    @Test
    fun `idle and detecting are dashed purple with silhouette and no rail`() {
        listOf(LivenessState.IDLE, LivenessState.DETECTING_FACE).forEach { s ->
            val style = styleOf(s)
            assertEquals(true, style.dashed)
            assertEquals(Color(0xFF8A3DFF), style.color)
            assertEquals(RailMode.HIDDEN, style.railMode)
            assertEquals(FrameBadge.NONE, style.badge)
            assertEquals(FrameInnerContent.SILHOUETTE, style.inner)
        }
    }

    @Test
    fun `waiting for liveness shows progress rail on solid purple frame`() {
        val style = styleOf(LivenessState.WAITING_FOR_LIVENESS)
        assertEquals(false, style.dashed)
        assertEquals(Color(0xFF8A3DFF), style.color)
        assertEquals(RailMode.PROGRESS, style.railMode)
        assertEquals(FrameBadge.NONE, style.badge)
        assertEquals(FrameInnerContent.NONE, style.inner)
    }

    @Test
    fun `low light hides the rail`() {
        val style = styleOf(LivenessState.LOW_LIGHT)
        assertEquals(false, style.dashed)
        assertEquals(RailMode.HIDDEN, style.railMode)
        assertEquals(FrameInnerContent.NONE, style.inner)
    }

    @Test
    fun `liveness detected shows progress until ready to verify`() {
        assertEquals(
            RailMode.PROGRESS,
            styleOf(LivenessState.LIVENESS_DETECTED).railMode
        )
        assertEquals(
            RailMode.ALL_PASSED,
            styleOf(LivenessState.LIVENESS_DETECTED, readyToVerify = true).railMode
        )
    }

    @Test
    fun `verifying keeps the all-passed rail`() {
        val style = styleOf(LivenessState.VERIFYING_FACE)
        assertEquals(RailMode.ALL_PASSED, style.railMode)
        assertEquals(FrameBadge.NONE, style.badge)
    }

    @Test
    fun `success is cyan with check badge and no rail`() {
        val style = styleOf(LivenessState.SUCCESS)
        assertEquals(Color(0xFF38F9F5), style.color)
        assertEquals(FrameBadge.CHECK, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
        assertEquals(false, style.dashed)
    }

    @Test
    fun `failure is cyan with cross badge not a red frame`() {
        val style = styleOf(LivenessState.FAILURE)
        assertEquals(Color(0xFF38F9F5), style.color)
        assertEquals(FrameBadge.CROSS, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
    }

    @Test
    fun `timeout is purple with timeout inner content`() {
        val style = styleOf(LivenessState.TIMEOUT)
        assertEquals(Color(0xFF8A3DFF), style.color)
        assertEquals(FrameInnerContent.TIMEOUT_INFO, style.inner)
        assertEquals(FrameBadge.NONE, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
    }
}
