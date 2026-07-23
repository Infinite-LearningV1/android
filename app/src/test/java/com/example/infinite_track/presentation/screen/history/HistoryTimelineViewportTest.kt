package com.example.infinite_track.presentation.screen.history

import com.example.infinite_track.presentation.design.components.data.HistoryFocusTransform
import com.example.infinite_track.presentation.design.components.data.resolveHistoryFocusTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTimelineViewportTest {
    @Test
    fun `history key round trips a stable record id`() {
        assertEquals("history-71", historyItemKey(71))
        assertEquals(71, historyRecordIdFromKey("history-71"))
        assertEquals(null, historyRecordIdFromKey("summary"))
    }

    @Test
    fun `focus is one at center and clamps to zero outside range`() {
        assertEquals(1f, calculateHistoryFocusFraction(500f, 500f, 1000f), 0.0001f)
        assertEquals(0f, calculateHistoryFocusFraction(1200f, 500f, 1000f), 0.0001f)
        assertTrue(calculateHistoryFocusFraction(700f, 500f, 1000f) in 0f..1f)
    }

    @Test
    fun `timeline progress interpolates between visible record centers`() {
        val visible = listOf(
            VisibleHistoryItem(recordIndex = 2, center = 400f),
            VisibleHistoryItem(recordIndex = 3, center = 600f)
        )

        assertEquals(0.625f, calculateHistoryTimelineProgress(visible, 5, 500f), 0.0001f)
    }

    @Test
    fun `timeline progress handles empty and zero range`() {
        assertEquals(0f, calculateHistoryTimelineProgress(emptyList(), 0, 500f), 0f)
        assertEquals(
            0f,
            calculateHistoryTimelineProgress(listOf(VisibleHistoryItem(0, 500f)), 1, 500f),
            0f
        )
    }

    @Test
    fun `reduced motion keeps content fully readable`() {
        assertEquals(
            HistoryFocusTransform(alpha = 1f, scale = 1f, translationYDp = 0f, elevationDp = 0f),
            resolveHistoryFocusTransform(focusFraction = 0.2f, motionEnabled = false)
        )
    }

    @Test
    fun `center focus reaches approved transform envelope`() {
        assertEquals(
            HistoryFocusTransform(alpha = 1f, scale = 1f, translationYDp = -3f, elevationDp = 3f),
            resolveHistoryFocusTransform(focusFraction = 1f, motionEnabled = true)
        )
        assertEquals(
            HistoryFocusTransform(alpha = 0.70f, scale = 0.965f, translationYDp = 0f, elevationDp = 0f),
            resolveHistoryFocusTransform(focusFraction = 0f, motionEnabled = true)
        )
    }

    @Test
    fun `load more uses last visible record index not outer lazy index`() {
        assertTrue(shouldLoadMoreHistory(7, 10, canLoadMore = true, loading = false))
        assertFalse(shouldLoadMoreHistory(5, 10, canLoadMore = true, loading = false))
        assertFalse(shouldLoadMoreHistory(7, 10, canLoadMore = false, loading = false))
    }

    @Test
    fun `half progress completes the first segment and middle node`() {
        val first = resolveHistoryConnectorAccent(0, 3, 0.5f)
        val middle = resolveHistoryConnectorAccent(1, 3, 0.5f)

        assertEquals(1f, first.bottomFraction, 0f)
        assertEquals(1f, middle.topFraction, 0f)
        assertTrue(middle.nodeComplete)
        assertEquals(0f, middle.bottomFraction, 0f)
    }

    @Test
    fun `full progress accents every available connector half`() {
        val accents = (0..2).map { resolveHistoryConnectorAccent(it, 3, 1f) }

        assertEquals(0f, accents[0].topFraction, 0f)
        assertEquals(1f, accents[0].bottomFraction, 0f)
        assertEquals(1f, accents[1].topFraction, 0f)
        assertEquals(1f, accents[1].bottomFraction, 0f)
        assertEquals(1f, accents[2].topFraction, 0f)
        assertEquals(0f, accents[2].bottomFraction, 0f)
        assertTrue(accents.all { it.nodeComplete })
    }
}
