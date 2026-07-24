package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LivenessSequencerTest {

    @Test
    fun `starts at first challenge`() {
        val s = LivenessSequencer()

        assertEquals(LivenessChallenge.BLINK, s.current)
        assertEquals(1, s.index)
        assertEquals(0, s.passedCount)
        assertFalse(s.isComplete)
    }

    @Test
    fun `advances through fixed order`() {
        val s = LivenessSequencer()

        s.pass()
        assertEquals(LivenessChallenge.SMILE, s.current)
        assertEquals(2, s.index)

        s.pass()
        assertEquals(LivenessChallenge.TURN_LEFT, s.current)

        s.pass()
        assertEquals(LivenessChallenge.TURN_RIGHT, s.current)
        assertFalse(s.isComplete)

        s.pass()
        assertEquals(4, s.passedCount)
        assertTrue(s.isComplete)
    }

    @Test
    fun `reset returns to start`() {
        val s = LivenessSequencer()
        s.pass()
        s.pass()

        s.reset()

        assertEquals(LivenessChallenge.BLINK, s.current)
        assertEquals(0, s.passedCount)
        assertFalse(s.isComplete)
    }
}
