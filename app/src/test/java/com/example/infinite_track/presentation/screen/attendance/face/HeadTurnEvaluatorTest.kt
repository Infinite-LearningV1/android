package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.data.face.LivenessResult
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadTurnEvaluatorTest {

    @Test
    fun `turn right success when eulerY beyond positive threshold`() {
        assertEquals(
            LivenessResult.SUCCESS,
            HeadTurnEvaluator.evaluate(28f, LivenessChallenge.TURN_RIGHT)
        )
    }

    @Test
    fun `turn left success when eulerY beyond negative threshold`() {
        assertEquals(
            LivenessResult.SUCCESS,
            HeadTurnEvaluator.evaluate(-28f, LivenessChallenge.TURN_LEFT)
        )
    }

    @Test
    fun `in progress near threshold`() {
        assertEquals(
            LivenessResult.IN_PROGRESS,
            HeadTurnEvaluator.evaluate(14f, LivenessChallenge.TURN_RIGHT)
        )
    }

    @Test
    fun `failure when centered`() {
        assertEquals(
            LivenessResult.FAILURE,
            HeadTurnEvaluator.evaluate(2f, LivenessChallenge.TURN_RIGHT)
        )
    }

    @Test
    fun `wrong direction is failure`() {
        assertEquals(
            LivenessResult.FAILURE,
            HeadTurnEvaluator.evaluate(28f, LivenessChallenge.TURN_LEFT)
        )
    }
}
