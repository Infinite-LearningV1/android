package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.setContent {
            Infinite_TrackTheme {
                LocationSearchContent(
                    query = "kopi",
                    state = SearchUiState.Error("Jaringan tidak tersedia."),
                    resolvingPlaceId = null,
                    onQueryChange = {},
                    onClear = {},
                    onRetry = { retried = true },
                    onSuggestionSelected = {}
                )
            }
        }

        compose.onNodeWithText("Coba lagi").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(retried) }
    }
}
