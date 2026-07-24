package com.example.infinite_track.presentation.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.map.model.MapMarkerCategory
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactMapCalloutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compact_callout_shows_only_name_and_no_large_metadata() {
        composeRule.setContent {
            Infinite_TrackTheme {
                CompactMapCallout(
                    title = "Infinite Track Office Palu",
                    category = MapMarkerCategory.WFO
                )
            }
        }

        composeRule.onNodeWithTag("compactMapCallout").assertIsDisplayed()
        composeRule.onNodeWithText("Infinite Track Office Palu").assertIsDisplayed()
        composeRule.onNodeWithText("WFA score", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Radius", substring = true).assertDoesNotExist()
    }
}
