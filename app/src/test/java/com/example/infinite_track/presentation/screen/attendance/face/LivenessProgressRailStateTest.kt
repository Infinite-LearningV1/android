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

    @Test
    fun `four challenge placements follow the mockup border positions`() {
        val placements = railNodePlacements(4)

        assertEquals(
            listOf(
                RailNodePlacement(RailSide.LEFT, 0.20f),
                RailNodePlacement(RailSide.LEFT, 0.45f),
                RailNodePlacement(RailSide.RIGHT, 0.35f),
                RailNodePlacement(RailSide.RIGHT, 0.55f)
            ),
            placements
        )
    }

    @Test
    fun `non-standard totals alternate sides with even spacing`() {
        val placements = railNodePlacements(2)

        assertEquals(RailSide.LEFT, placements[0].side)
        assertEquals(RailSide.RIGHT, placements[1].side)
        assertEquals(1f / 3f, placements[0].heightFraction, 0.0001f)
        assertEquals(2f / 3f, placements[1].heightFraction, 0.0001f)
    }
}
