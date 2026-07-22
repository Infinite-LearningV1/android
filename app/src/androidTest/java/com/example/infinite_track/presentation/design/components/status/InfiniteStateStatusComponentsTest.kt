package com.example.infinite_track.presentation.design.components.status

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfiniteStateStatusComponentsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun inlineAlertIsPersistentAndExposesRetryAndDismissTargets() {
        var retried = false
        var dismissed = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteInlineAlert("Connection", "Try again", InfiniteSemantic.Error, "Retry", { retried = true }, onDismiss = { dismissed = true })
            }
        }
        composeRule.mainClock.advanceTimeBy(15_000)
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithContentDescription("Dismiss alert").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(retried && dismissed)
    }

    @Test fun embeddedStatesRenderAndErrorRecoveryPerformsAction() {
        var recovered = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteEmptyState("Empty", "Nothing here")
                InfiniteLoadingState(message = "Loading")
                InfiniteErrorState("Failed", "Retry the request", actionLabel = "Retry", onAction = { recovered = true })
            }
        }
        composeRule.onNodeWithText("Empty").assertIsDisplayed()
        composeRule.onNodeWithText("Loading").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(recovered)
    }

    @Test fun compactPillAndDialogsExposeExplicitActions() {
        var confirmed = false
        var cancelled = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteStatusPill("Ready", InfiniteStatusVariant.Active)
                InfiniteStatusDialog("Done", "Saved", InfiniteSemantic.Success, true, onDismiss = {}, onConfirm = { confirmed = true })
                InfiniteConfirmDialog("Delete", "Cannot undo", InfiniteSemantic.Error, true, onDismiss = { cancelled = true }, onConfirm = { confirmed = true })
            }
        }
        composeRule.onNodeWithText("Ready").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(confirmed && cancelled)
    }
}
