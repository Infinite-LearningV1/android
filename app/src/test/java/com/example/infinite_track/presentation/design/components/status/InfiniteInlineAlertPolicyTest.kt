package com.example.infinite_track.presentation.design.components.status

import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class InfiniteInlineAlertPolicyTest {

    @Test
    fun `success and info without actions use short timed feedback`() {
        assertEquals(
            InfiniteInlineAlertDuration.Short,
            InfiniteSemantic.Success.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Short,
            InfiniteSemantic.Info.defaultInlineAlertDuration()
        )
    }

    @Test
    fun `actionable warning error and processing overrides remain persistent`() {
        assertEquals(
            InfiniteInlineAlertDuration.Persistent,
            InfiniteSemantic.Warning.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Persistent,
            InfiniteSemantic.Error.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Persistent,
            InfiniteSemantic.Success.defaultInlineAlertDuration(hasAction = true)
        )
    }
}
