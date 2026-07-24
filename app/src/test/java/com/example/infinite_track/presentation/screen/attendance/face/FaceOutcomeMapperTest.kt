package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceOutcomeMapperTest {

    @Test
    fun `match maps to success without reason`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.success(true))

        assertEquals(LivenessState.SUCCESS, outcome.livenessState)
        assertNull(outcome.failureReason)
    }

    @Test
    fun `no match maps to NOT_MATCHED failure`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.success(false))

        assertEquals(LivenessState.FAILURE, outcome.livenessState)
        assertEquals(FaceVerificationFailureReason.NOT_MATCHED, outcome.failureReason)
    }

    @Test
    fun `verification exception maps to TECHNICAL_FAILURE`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.failure(IllegalStateException("no embedding")))

        assertEquals(LivenessState.FAILURE, outcome.livenessState)
        assertEquals(FaceVerificationFailureReason.TECHNICAL_FAILURE, outcome.failureReason)
    }

    @Test
    fun `multiple faces yields MULTIPLE_FACES guidance`() {
        assertEquals(
            FaceVerificationFailureReason.MULTIPLE_FACES,
            FaceCountGuidance.reasonFor(2)
        )
    }

    @Test
    fun `single or no face yields no guidance`() {
        assertNull(FaceCountGuidance.reasonFor(1))
        assertNull(FaceCountGuidance.reasonFor(0))
    }
}
