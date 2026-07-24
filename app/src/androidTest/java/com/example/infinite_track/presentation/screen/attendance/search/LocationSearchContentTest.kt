package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationSearchContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun results_render_only_inside_explicit_search_content() {
        compose.setContent {
            Infinite_TrackTheme {
                LocationSearchContent(
                    query = "kopi",
                    state = SearchUiState.Success(
                        listOf(PlaceSuggestion("id-1", "Kopi Palu", "Tondo", null))
                    ),
                    resolvingPlaceId = null,
                    onQueryChange = {},
                    onClear = {},
                    onRetry = {},
                    onSuggestionSelected = {}
                )
            }
        }

        compose.onNodeWithTag("wfaSearchResults").assertIsDisplayed()
        compose.onNodeWithText("Kopi Palu").assertIsDisplayed()
        compose.onNodeWithText("Tondo").assertIsDisplayed()
    }

    @Test
    fun search_field_uses_localized_description_without_exposing_leading_icon() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fieldDescription = context.getString(R.string.wfa_search_field_content_description)

        compose.setContent {
            Infinite_TrackTheme {
                LocationSearchContent(
                    query = "",
                    state = SearchUiState.Idle,
                    resolvingPlaceId = null,
                    onQueryChange = {},
                    onClear = {},
                    onRetry = {},
                    onSuggestionSelected = {}
                )
            }
        }

        compose.onAllNodes(hasContentDescription(fieldDescription)).assertCountEquals(1)
    }

    @Test
    fun resolving_card_exposes_selected_semantics() {
        compose.setContent {
            Infinite_TrackTheme {
                WfaPlaceResultCard(
                    suggestion = PlaceSuggestion("id-1", "Kopi Palu", "Tondo", null),
                    selected = true,
                    onClick = {}
                )
            }
        }

        compose.onNodeWithTag("wfaPlace:id-1").assertIsSelected()
    }

    @Test
    fun failure_renders_retry_action_and_invokes_callback() {
        var retried = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val retryLabel = context.getString(R.string.wfa_search_retry)
        compose.setContent {
            Infinite_TrackTheme {
                LocationSearchContent(
                    query = "kopi",
                    state = SearchUiState.Error(R.string.wfa_search_error_network),
                    resolvingPlaceId = null,
                    onQueryChange = {},
                    onClear = {},
                    onRetry = { retried = true },
                    onSuggestionSelected = {}
                )
            }
        }

        compose.onNodeWithText(retryLabel).assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(retried) }
    }
}
