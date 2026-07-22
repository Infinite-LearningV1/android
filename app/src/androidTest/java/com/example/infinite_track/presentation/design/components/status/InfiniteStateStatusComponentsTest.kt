package com.example.infinite_track.presentation.design.components.status

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette
import com.example.infinite_track.presentation.theme.toAttendanceBadgeColor
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
        composeRule.onNodeWithContentDescription("Connection. Try again").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithContentDescription("Dismiss alert").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(retried && dismissed)
    }

    @Test fun everySemanticHasDistinctAccessibleFeedbackAndIconContract() {
        InfiniteSemantic.entries.forEach { semantic ->
            composeRule.setContent { Infinite_TrackTheme { InfiniteInlineAlert(semantic.name, "Message", semantic) } }
            composeRule.onNodeWithContentDescription("${semantic.name}. Message").assertIsDisplayed()
            assertTrue(semanticIcon(semantic) != null)
        }
    }

    @Test fun legacyIllustrationSelectorWinsAndPillResolverPreservesPaletteModes() {
        assertTrue(statusDialogUsesIllustration(android.R.drawable.ic_dialog_info))
        assertTrue(!statusDialogUsesIllustration(null))
        val semantic = InfiniteSemantic.Success
        val default = resolveInfiniteStatusPillPalette(semantic, false, null, InfiniteStatusVariant.Active)
        assertEquals(infiniteFeedbackPalette(semantic).surfaceEnd, default.container)
        val override = Color.Magenta
        assertEquals(override, resolveInfiniteStatusPillPalette(semantic, false, override, InfiniteStatusVariant.Active).content)
        assertEquals(InfiniteStatusVariant.Active.toAttendanceBadgeColor(), resolveInfiniteStatusPillPalette(semantic, true, null, InfiniteStatusVariant.Active).content)
    }

    @Test fun embeddedStatesRenderAndErrorRecoveryPerformsAction() {
        var recovered = false
        var emptyAction = false
        var loadingAction = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteEmptyState("Empty", "Nothing here", actionLabel = "Add", onAction = { emptyAction = true })
                InfiniteLoadingState(message = "Loading", actionLabel = "Cancel", onAction = { loadingAction = true })
                InfiniteErrorState("Failed", "Retry the request", actionLabel = "Retry", onAction = { recovered = true })
            }
        }
        composeRule.onNodeWithText("Empty").assertIsDisplayed()
        composeRule.onNodeWithText("Loading").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Embedded state").assertIsDisplayed()
        composeRule.onNodeWithText("Add").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Cancel").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(recovered && emptyAction && loadingAction)
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

    @Test fun confirmDialogActionsRemainVisibleAt320DpAndFontScaleTwo() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(Modifier.width(320.dp).testTag("dialogHost")) { InfiniteConfirmDialogContent(
                        title = "Confirm a long action",
                        message = "The actions must reflow instead of being clipped.",
                        semantic = InfiniteSemantic.Warning,
                        showDialog = true,
                        modifier = Modifier,
                        confirmText = "Continue",
                        cancelText = "Not now",
                        onDismiss = {},
                        onConfirm = {},
                        isDestructive = false
                    ) }
                }
            }
        }
        composeRule.onNodeWithText("Not now").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Continue").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val cancel = composeRule.onNodeWithText("Not now").getUnclippedBoundsInRoot()
        val confirm = composeRule.onNodeWithText("Continue").getUnclippedBoundsInRoot()
        val host = composeRule.onNodeWithTag("dialogHost").getUnclippedBoundsInRoot()
        assertTrue(cancel.bottom <= confirm.top && cancel.left >= host.left && confirm.right <= host.right)
    }
}
