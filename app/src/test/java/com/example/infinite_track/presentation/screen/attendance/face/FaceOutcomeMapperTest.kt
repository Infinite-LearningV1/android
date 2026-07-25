package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.domain.use_case.auth.VerifyFaceMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceOutcomeMapperTest {

    @Test
    fun `match maps to success carrying score`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.success(VerifyFaceMatch(true, 0.42f, 0.15f)))

        assertEquals(LivenessState.SUCCESS, outcome.livenessState)
        assertNull(outcome.failureReason)
        assertEquals(0.42f, outcome.similarity)
        assertEquals(0.15f, outcome.threshold)
    }

    @Test
    fun `no match carries score and NOT_MATCHED`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.success(VerifyFaceMatch(false, 0.05f, 0.15f)))

        assertEquals(LivenessState.FAILURE, outcome.livenessState)
        assertEquals(FaceVerificationFailureReason.NOT_MATCHED, outcome.failureReason)
        assertEquals(0.05f, outcome.similarity)
    }

    @Test
    fun `failure maps to TECHNICAL_FAILURE without score`() {
        val outcome = FaceOutcomeMapper.fromMatch(Result.failure(IllegalStateException("no embedding")))

        assertEquals(LivenessState.FAILURE, outcome.livenessState)
        assertEquals(FaceVerificationFailureReason.TECHNICAL_FAILURE, outcome.failureReason)
        assertNull(outcome.similarity)
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
