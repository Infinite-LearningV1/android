package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Test

class LivenessProgressRailStateTest {

    @Test
    fun `node states reflect progress`() {
        val states = railNodeStates(passedCount = 1, activeIndex = 2)

        assertEquals(RailNodeState.PASSED, states[0])
        assertEquals(RailNodeState.ACTIVE, states[1])
        assertEquals(RailNodeState.PENDING, states[2])
        assertEquals(RailNodeState.PENDING, states[3])
    }

    @Test
    fun `all passed when complete`() {
        val states = railNodeStates(passedCount = 4, activeIndex = 4)

        assertEquals(List(4) { RailNodeState.PASSED }, states)
    }
}
