package com.example.infinite_track.presentation.design.components.status

import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class InfiniteInlineAlertPolicyTest {

    @Test
    fun `transient semantic durations are four and eight seconds`() {
        assertEquals(4_000, InfiniteInlineAlertDuration.Short.timeoutMillis)
        assertEquals(8_000, InfiniteInlineAlertDuration.Long.timeoutMillis)
        assertEquals(
            InfiniteInlineAlertDuration.Short,
            InfiniteSemantic.Success.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Short,
            InfiniteSemantic.Info.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Long,
            InfiniteSemantic.Warning.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Long,
            InfiniteSemantic.Error.defaultInlineAlertDuration()
        )
        assertEquals(
            InfiniteInlineAlertDuration.Persistent,
            InfiniteSemantic.Warning.defaultInlineAlertDuration(hasAction = true)
        )
    }
}
