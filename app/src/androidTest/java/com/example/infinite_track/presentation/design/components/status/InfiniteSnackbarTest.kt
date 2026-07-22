package com.example.infinite_track.presentation.design.components.status

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
