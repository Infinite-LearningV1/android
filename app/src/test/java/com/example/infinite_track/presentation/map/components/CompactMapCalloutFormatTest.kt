package com.example.infinite_track.presentation.map.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactMapCalloutFormatTest {
    @Test
    fun `fuzzy AHP score is presented as a bounded percentage`() {
        assertEquals(91, formatFuzzyAhpRatingPercent(0.91))
        assertEquals(0, formatFuzzyAhpRatingPercent(-1.0))
        assertEquals(100, formatFuzzyAhpRatingPercent(2.0))
        assertEquals(0, formatFuzzyAhpRatingPercent(Double.NaN))
    }
}
