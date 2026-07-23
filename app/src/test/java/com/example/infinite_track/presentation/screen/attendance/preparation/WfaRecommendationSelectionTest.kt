package com.example.infinite_track.presentation.screen.attendance.preparation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaRecommendationSelectionTest {

    @Test
    fun `selection is derived from content selected key instead of row state`() {
        val selected = recommendation("cafe@-0.90,119.88")
        val other = recommendation("library@-0.91,119.89")
        val content = WfaDiscoveryUiModel.Content(
            rows = listOf(selected, other),
            selectedKey = selected.stableKey,
            searchPreviewName = null
        )

        assertTrue(content.isRecommendationSelected(selected))
        assertFalse(content.isRecommendationSelected(other))
    }

    private fun recommendation(stableKey: String) = WfaRecommendationUiModel(
        stableKey = stableKey,
        name = "Location",
        supportingText = "Cafe - 1 km",
        suitabilityText = "Skor WFA 90",
    )
}
