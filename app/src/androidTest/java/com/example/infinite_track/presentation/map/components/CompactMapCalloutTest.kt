package com.example.infinite_track.presentation.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.map.model.MapMarkerRecommendationInfo
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import com.example.infinite_track.presentation.format.formatDistanceMeters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactMapCalloutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recommendation_callout_shows_fuzzy_AHP_metadata() {
        composeRule.setContent {
            Infinite_TrackTheme {
                CompactMapCallout(
                    title = "Infinite Track Office Palu",
                    category = MapMarkerCategory.WFA,
                    recommendationInfo = MapMarkerRecommendationInfo(
                        category = "Cafe",
                        distance = DistanceMeters(1_250.0),
                        fuzzyAhpScore = 0.91,
                        suitabilityLabel = "Sangat sesuai"
                    )
                )
            }
        }

        composeRule.onNodeWithTag("compactMapCallout").assertIsDisplayed()
        composeRule.onNodeWithText("Infinite Track Office Palu").assertIsDisplayed()
        composeRule.onNodeWithText("Cafe").assertIsDisplayed()
        val locale = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.locales[0]
        composeRule.onNodeWithText(formatDistanceMeters(1_250.0, locale)).assertIsDisplayed()
        composeRule.onNodeWithText("91%", substring = true).assertIsDisplayed()
    }

    @Test
    fun ordinary_marker_callout_does_not_show_recommendation_metadata() {
        composeRule.setContent {
            Infinite_TrackTheme {
                CompactMapCallout(
                    title = "Current location",
                    category = MapMarkerCategory.CURRENT_LOCATION
                )
            }
        }

        composeRule.onNodeWithText("Current location").assertIsDisplayed()
        composeRule.onNodeWithText("AHP", substring = true).assertDoesNotExist()
    }
}
