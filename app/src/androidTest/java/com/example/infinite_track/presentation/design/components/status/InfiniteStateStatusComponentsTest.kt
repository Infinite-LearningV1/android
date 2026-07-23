package com.example.infinite_track.presentation.design.components.status

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
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

    @Test fun actionableAlertIsPersistentAndExposesOnlyItsRecoveryTarget() {
        var retried = false
        var dismissCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteInlineAlert(
                    title = "Connection",
                    message = "Try again",
                    semantic = InfiniteSemantic.Error,
                    actionLabel = "Retry",
                    onAction = { retried = true },
                    onDismiss = { dismissCount += 1 }
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(15_000)
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Connection. Try again").assertIsDisplayed()
        composeRule.onNodeWithTag(INLINE_ALERT_TIMER_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Dismiss alert").assertDoesNotExist()
        composeRule.onNodeWithText("Retry").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        composeRule.runOnIdle {
            assertTrue(retried)
            assertEquals(0, dismissCount)
        }
    }

    @Test fun persistentAlertLeavingCompositionDoesNotInvokeDismissCallback() {
        var showAlert by mutableStateOf(true)
        var dismissCount = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                if (showAlert) {
                    InfiniteInlineAlert(
                        title = "Connection",
                        message = "Try again",
                        semantic = InfiniteSemantic.Error,
                        actionLabel = "Retry",
                        onAction = {},
                        onDismiss = { dismissCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.runOnIdle { showAlert = false }
        composeRule.runOnIdle { assertEquals(0, dismissCount) }
    }

    @Test fun transientWarningShowsTimerAndDismissThenTimesOutExactlyOnce() {
        var dismissCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides null) {
                Infinite_TrackTheme {
                    InfiniteInlineAlert(
                        title = "Location warning",
                        message = "Move closer to the office.",
                        semantic = InfiniteSemantic.Warning,
                        onDismiss = { dismissCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Location warning").assertIsDisplayed()
        composeRule.onNodeWithTag(INLINE_ALERT_TIMER_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss alert")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.mainClock.advanceTimeBy(8_250)
        composeRule.onNodeWithText("Location warning").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test fun dismissingTransientAlertByXInvokesCallbackOnlyOnce() {
        var dismissCount = 0
        var showAlert by mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Infinite_TrackTheme {
                if (showAlert) {
                    InfiniteInlineAlert(
                        title = "Attendance saved",
                        message = "Check-in recorded.",
                        semantic = InfiniteSemantic.Success,
                        onDismiss = { dismissCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Dismiss alert").performClick()
        composeRule.runOnIdle { showAlert = false }
        composeRule.mainClock.advanceTimeBy(5_000)
        composeRule.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test fun replacingTransientAlertDismissesOutgoingThenTimesIncomingExactlyOnce() {
        var activeTitle by mutableStateOf<String?>("First warning")
        var outgoingDismissCount = 0
        var incomingDismissCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides null) {
                Infinite_TrackTheme {
                    activeTitle?.let { title ->
                        InfiniteInlineAlert(
                            title = title,
                            message = "Move closer to the office.",
                            semantic = InfiniteSemantic.Warning,
                            onDismiss = if (title == "First warning") {
                                { outgoingDismissCount += 1 }
                            } else {
                                { incomingDismissCount += 1 }
                            }
                        )
                    }
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(7_000)
        composeRule.runOnIdle { activeTitle = "Replacement warning" }
        composeRule.runOnIdle {
            assertEquals(1, outgoingDismissCount)
            assertEquals(0, incomingDismissCount)
        }
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.onNodeWithText("Replacement warning").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, outgoingDismissCount)
            assertEquals(0, incomingDismissCount)
        }
        composeRule.mainClock.advanceTimeBy(6_750)
        composeRule.onNodeWithText("Replacement warning").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, outgoingDismissCount)
            assertEquals(1, incomingDismissCount)
            activeTitle = null
        }
        composeRule.runOnIdle {
            assertEquals(1, outgoingDismissCount)
            assertEquals(1, incomingDismissCount)
        }
    }

    @Test fun transientDismissUsesLatestCallbackWithoutRestartingTimer() {
        var callbackVersion by mutableStateOf(1)
        var firstCallbackCount = 0
        var latestCallbackCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteInlineAlert(
                    title = "Attendance saved",
                    message = "Check-in recorded.",
                    semantic = InfiniteSemantic.Success,
                    onDismiss = if (callbackVersion == 1) {
                        { firstCallbackCount += 1 }
                    } else {
                        { latestCallbackCount += 1 }
                    }
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { callbackVersion = 2 }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithContentDescription("Dismiss alert").performClick()
        composeRule.runOnIdle {
            assertEquals(0, firstCallbackCount)
            assertEquals(1, latestCallbackCount)
        }
    }

    @Test fun accessibilityRecommendedTimeoutGovernsTimerAndDismissal() {
        var dismissCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAccessibilityManager provides FixedTimeoutAccessibilityManager(12_000)
            ) {
                Infinite_TrackTheme {
                    InfiniteInlineAlert(
                        title = "Accessible warning",
                        message = "This stays visible longer.",
                        semantic = InfiniteSemantic.Warning,
                        onDismiss = { dismissCount += 1 }
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(8_250)
        composeRule.onNodeWithText("Accessible warning").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, dismissCount) }
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.onNodeWithText("Accessible warning").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test fun transientWithoutExternalCallbackStillReportsDismissControlToAccessibility() {
        val accessibilityManager = FixedTimeoutAccessibilityManager(12_000)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides accessibilityManager) {
                Infinite_TrackTheme {
                    InfiniteInlineAlert(
                        title = "Attendance saved",
                        message = "Check-in recorded.",
                        semantic = InfiniteSemantic.Success
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Dismiss alert").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(4_000L, accessibilityManager.originalTimeoutMillis)
            assertTrue(accessibilityManager.containsIcons)
            assertTrue(accessibilityManager.containsText)
            assertTrue(accessibilityManager.containsControls)
        }
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
        assertEquals(infiniteFeedbackPalette(semantic).surface, default.container)
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
                    Box(Modifier.width(320.dp).testTag("dialogHost")) { InfiniteConfirmDialogBody(
                        title = "Confirm a long action",
                        message = "The actions must reflow instead of being clipped.",
                        semantic = InfiniteSemantic.Warning,
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

    @Test fun confirmLoadingKeepsOneDialogAndDisablesBothActions() {
        var dismissCount = 0
        var confirmCount = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteConfirmDialogBody(
                    title = "Log out",
                    message = "Please wait while logout completes.",
                    semantic = InfiniteSemantic.Warning,
                    confirmText = "Log out",
                    cancelText = "Cancel",
                    onDismiss = { dismissCount += 1 },
                    onConfirm = { confirmCount += 1 },
                    isDestructive = true,
                    confirmLoading = true
                )
            }
        }

        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
        composeRule.onNodeWithText("Log out").assertIsNotEnabled()
        composeRule.onNodeWithTag(CONFIRM_LOADING_INDICATOR_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(18.dp)
            .assertHeightIsEqualTo(18.dp)
        composeRule.runOnIdle {
            assertEquals(0, dismissCount)
            assertEquals(0, confirmCount)
        }
    }

    private class FixedTimeoutAccessibilityManager(
        private val timeoutMillis: Long
    ) : AccessibilityManager {
        var originalTimeoutMillis: Long? = null
        var containsIcons: Boolean = false
        var containsText: Boolean = false
        var containsControls: Boolean = false

        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean
        ): Long {
            this.originalTimeoutMillis = originalTimeoutMillis
            this.containsIcons = containsIcons
            this.containsText = containsText
            this.containsControls = containsControls
            return timeoutMillis
        }
    }
}
