package com.example.infinite_track.presentation.design.components.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfiniteSnackbarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun semanticVariantsRenderReadableMessage() {
        InfiniteSemantic.entries.forEach { semantic ->
            setSnackbarContent(
                FakeSnackbarData(
                    InfiniteSnackbarVisuals(message = semantic.name, semantic = semantic)
                )
            )

            composeRule.onNodeWithText(semantic.name).assertIsDisplayed()
        }
    }

    @Test
    fun materialVisualsUseSafeInfoFallbackWithMergedReadableSemantics() {
        setSnackbarContent(FakeSnackbarData(DefaultVisuals(message = "Fallback message")))

        composeRule.onNodeWithText("Fallback message", useUnmergedTree = false)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun actionPerformsSnackbarActionAndMeetsMinimumTarget() {
        val data = FakeSnackbarData(
            InfiniteSnackbarVisuals(
                message = "Connection is weak",
                semantic = InfiniteSemantic.Warning,
                actionLabel = "Retry"
            )
        )
        setSnackbarContent(data)

        composeRule.onNodeWithText("Retry")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(data.actionPerformed)
    }

    @Test
    fun explicitDismissUsesAccessibleMinimumTarget() {
        val data = FakeSnackbarData(
            InfiniteSnackbarVisuals(
                message = "Dismissible message",
                semantic = InfiniteSemantic.Info,
                withDismissAction = true
            )
        )
        setSnackbarContent(data)

        composeRule.onNodeWithContentDescription("Tutup notifikasi")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(data.dismissed)
    }

    @Test
    fun fallbackVisualsStillPerformTheirAction() {
        val data = FakeSnackbarData(
            DefaultVisuals(message = "Fallback action", actionLabel = "Coba lagi")
        )
        setSnackbarContent(data)

        composeRule.onNodeWithText("Coba lagi").performClick()

        assertTrue(data.actionPerformed)
    }

    @Test
    fun fallbackVisualsStillExposeDismissAction() {
        val data = FakeSnackbarData(
            DefaultVisuals(message = "Fallback dismiss", withDismissAction = true)
        )
        setSnackbarContent(data)

        composeRule.onNodeWithContentDescription("Tutup notifikasi").performClick()

        assertTrue(data.dismissed)
    }

    @Test
    fun glassSurfaceDecorationDoesNotExpandContentInsideTallBoundedParent() {
        composeRule.setContent {
            Infinite_TrackTheme {
                Box(Modifier.size(width = 320.dp, height = 600.dp)) {
                    InfiniteFeedbackGlassSurface(
                        semantic = InfiniteSemantic.Info,
                        modifier = Modifier.testTag("contentSizedGlass")
                    ) {
                        Box(Modifier.size(width = 200.dp, height = 72.dp))
                    }
                }
            }
        }

        val surfaceBounds = composeRule.onNodeWithTag("contentSizedGlass")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertEquals(72.dp, surfaceBounds.bottom - surfaceBounds.top)
    }

    @Test
    fun longMessageAtFontScaleTwoGrowsWithoutClippingAndKeepsTargetsUsable() {
        val message = "Location access is still unavailable. Open settings, allow precise location, " +
            "then return to Infinite Track and try attendance again so your work location can be verified."
        val data = FakeSnackbarData(
            InfiniteSnackbarVisuals(
                message = message,
                semantic = InfiniteSemantic.Warning,
                actionLabel = "Settings",
                withDismissAction = true
            )
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                Infinite_TrackTheme {
                    Box(
                        Modifier
                            .size(width = 320.dp, height = 600.dp)
                            .testTag("snackbarHost")
                    ) {
                        InfiniteSnackbar(
                            data = data,
                            modifier = Modifier.testTag("snackbarRoot")
                        )
                    }
                }
            }
        }

        val messageNode = composeRule.onNodeWithText(message, useUnmergedTree = true)
            .assertTextEquals(message)
            .assertIsDisplayed()
        val messageBounds = messageNode.getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onNodeWithTag("snackbarRoot").getUnclippedBoundsInRoot()
        val hostBounds = composeRule.onNodeWithTag("snackbarHost").getUnclippedBoundsInRoot()

        assertTrue(messageBounds.bottom - messageBounds.top > 80.dp)
        assertTrue(messageBounds.top >= rootBounds.top && messageBounds.bottom <= rootBounds.bottom)
        assertTrue(rootBounds.left >= hostBounds.left && rootBounds.right <= hostBounds.right)
        assertTrue(rootBounds.top >= hostBounds.top && rootBounds.bottom <= hostBounds.bottom)

        composeRule.onNodeWithText("Settings")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithContentDescription("Tutup notifikasi")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(data.actionPerformed)
        assertTrue(data.dismissed)
    }

    @Test
    fun snackbarHostQueuesCallsSequentially() {
        val hostState = androidx.compose.material3.SnackbarHostState()
        val results = mutableListOf<androidx.compose.material3.SnackbarResult>()
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteSnackbarHost(hostState = hostState)
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    launch {
                        results += hostState.showSnackbar(
                            InfiniteSnackbarVisuals("First", InfiniteSemantic.Success)
                        )
                    }
                    launch {
                        results += hostState.showSnackbar(
                            InfiniteSnackbarVisuals("Second", InfiniteSemantic.Info)
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("First").assertIsDisplayed()
        composeRule.runOnIdle { hostState.currentSnackbarData?.dismiss() }
        composeRule.onNodeWithText("Second").assertIsDisplayed()
        composeRule.runOnIdle { hostState.currentSnackbarData?.dismiss() }
        composeRule.runOnIdle { assertEquals(2, results.size) }
    }

    private fun setSnackbarContent(data: SnackbarData) {
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteSnackbar(data = data)
            }
        }
    }

    private data class DefaultVisuals(
        override val message: String,
        override val actionLabel: String? = null,
        override val withDismissAction: Boolean = false,
        override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarVisuals

    private class FakeSnackbarData(
        override val visuals: SnackbarVisuals
    ) : SnackbarData {
        var actionPerformed = false
        var dismissed = false

        override fun performAction() {
            actionPerformed = true
        }

        override fun dismiss() {
            dismissed = true
        }
    }
}
