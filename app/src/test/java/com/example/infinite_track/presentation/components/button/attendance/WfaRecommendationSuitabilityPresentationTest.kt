package com.example.infinite_track.presentation.components.button.attendance

import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WfaRecommendationSuitabilityPresentationTest {

    @Test
    fun `error semantic reaches final option as outside with error accessibility`() {
        val presentation = WfaRecommendationSuitabilityPresentationMapper.map(
            InfiniteSemantic.Error
        )

        assertEquals(InfiniteStatusVariant.Outside, presentation.variant)
        assertEquals(InfiniteIcons.Error, presentation.icon)
        assertEquals(
            R.string.attendance_wfa_suitability_error_state,
            presentation.contentDescriptionRes
        )
        assertNotEquals(InfiniteStatusVariant.Recommended, presentation.variant)
        assertNotEquals(InfiniteIcons.Success, presentation.icon)
    }

    @Test
    fun `warning semantic reaches final option as needs review`() {
        val presentation = WfaRecommendationSuitabilityPresentationMapper.map(
            InfiniteSemantic.Warning
        )

        assertEquals(InfiniteStatusVariant.NeedsReview, presentation.variant)
        assertEquals(InfiniteIcons.Warning, presentation.icon)
        assertEquals(
            R.string.attendance_wfa_suitability_warning_state,
            presentation.contentDescriptionRes
        )
    }

    @Test
    fun `success semantic reaches final option as excellent`() {
        val presentation = WfaRecommendationSuitabilityPresentationMapper.map(
            InfiniteSemantic.Success
        )

        assertEquals(InfiniteStatusVariant.Excellent, presentation.variant)
        assertEquals(InfiniteIcons.Success, presentation.icon)
        assertEquals(
            R.string.attendance_wfa_suitability_success_state,
            presentation.contentDescriptionRes
        )
    }
}
