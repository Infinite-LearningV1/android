package com.example.infinite_track.presentation.design.components.status

import androidx.compose.ui.platform.AccessibilityManager
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `per content dismissal owner invokes each alert callback exactly once`() {
        val callbacks = mutableListOf<String>()
        val outgoing = InlineAlertDismissalOwner { callbacks += "A" }
        val incoming = InlineAlertDismissalOwner { callbacks += "B" }

        assertTrue(outgoing.dismiss())
        assertFalse(outgoing.dismiss())
        assertEquals(listOf("A"), callbacks)

        assertTrue(incoming.dismiss())
        assertFalse(incoming.dismiss())
        assertEquals(listOf("A", "B"), callbacks)
    }

    @Test
    fun `transient accessibility timeout reports icon text and dismiss control`() {
        val accessibilityManager = RecordingAccessibilityManager(recommendedTimeoutMillis = 12_000)

        assertEquals(
            12_000L,
            recommendedInlineAlertTimeoutMillis(
                duration = InfiniteInlineAlertDuration.Short,
                accessibilityManager = accessibilityManager
            )
        )
        assertEquals(4_000L, accessibilityManager.originalTimeoutMillis)
        assertTrue(accessibilityManager.containsIcons)
        assertTrue(accessibilityManager.containsText)
        assertTrue(accessibilityManager.containsControls)
    }

    private class RecordingAccessibilityManager(
        private val recommendedTimeoutMillis: Long
    ) : AccessibilityManager {
        var originalTimeoutMillis: Long? = null
        var containsIcons: Boolean = false
        var containsText: Boolean = false
        var containsControls: Boolean = false

        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean
        ): Long {
            this.originalTimeoutMillis = originalTimeoutMillis
            this.containsIcons = containsIcons
            this.containsText = containsText
            this.containsControls = containsControls
            return recommendedTimeoutMillis
        }
    }
}
