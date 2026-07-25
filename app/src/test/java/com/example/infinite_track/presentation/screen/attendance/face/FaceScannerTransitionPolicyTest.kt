package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceScannerTransitionPolicyTest {

    @Test
    fun `multiple faces reset a completed hold to the active challenge`() {
        val state = FaceScannerState(
            livenessState = LivenessState.LIVENESS_DETECTED,
            currentChallenge = LivenessChallenge.SMILE,
            readyToVerify = false
        )

        val updated = FaceScannerTransitionPolicy.onMultipleFaces(state)

        assertEquals(LivenessState.WAITING_FOR_LIVENESS, updated.livenessState)
        assertEquals(FaceVerificationFailureReason.MULTIPLE_FACES, updated.failureReason)
        assertFalse(updated.readyToVerify)
    }

    @Test
    fun `single face recovery clears multiple face guidance`() {
        val state = FaceScannerTransitionPolicy.onMultipleFaces(
            FaceScannerState(livenessState = LivenessState.WAITING_FOR_LIVENESS)
        )

        val updated = FaceScannerTransitionPolicy.onSingleFaceRecovered(state)

        assertEquals(LivenessState.WAITING_FOR_LIVENESS, updated.livenessState)
        assertNull(updated.failureReason)
        assertNull(updated.errorMessage)
    }

    @Test
    fun `timeout transition is terminal and rejects late detector callbacks`() {
        val timedOut = FaceScannerTransitionPolicy.onTimeout(
            state = FaceScannerTransitionPolicy.onMultipleFaces(
                FaceScannerState(livenessState = LivenessState.WAITING_FOR_LIVENESS)
            ),
            timeoutSeconds = 20
        )

        assertEquals(LivenessState.TIMEOUT, timedOut.livenessState)
        assertEquals(0, timedOut.timeRemaining)
        assertNull(timedOut.failureReason)
        assertFalse(FaceScannerTransitionPolicy.canAcceptDetection(timedOut))
    }

    @Test
    fun `hold advances only while the same challenge remains detected`() {
        val detected = FaceScannerState(
            livenessState = LivenessState.LIVENESS_DETECTED,
            currentChallenge = LivenessChallenge.TURN_LEFT
        )

        assertTrue(
            FaceScannerTransitionPolicy.canAdvanceHold(
                state = detected,
                expectedChallenge = LivenessChallenge.TURN_LEFT
            )
        )
        assertFalse(
            FaceScannerTransitionPolicy.canAdvanceHold(
                state = detected.copy(livenessState = LivenessState.DETECTING_FACE),
                expectedChallenge = LivenessChallenge.TURN_LEFT
            )
        )
        assertFalse(
            FaceScannerTransitionPolicy.canAdvanceHold(
                state = detected.copy(currentChallenge = LivenessChallenge.TURN_RIGHT),
                expectedChallenge = LivenessChallenge.TURN_LEFT
            )
        )
    }
}
