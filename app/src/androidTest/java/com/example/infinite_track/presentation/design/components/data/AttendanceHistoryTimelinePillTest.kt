package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendanceHistoryTimelinePillTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pillRendersRecordCopyAndExistingStatusPillSemantics() {
        composeRule.setContent {
            Infinite_TrackTheme {
                AttendanceHistoryTimelinePill(
                    nodeLabel = "22",
                    dateLabel = "22 July 2026",
                    timeRange = "08:00 - 17:00",
                    supportingText = "Head Office",
                    statusLabel = "On Time",
                    statusVariant = InfiniteStatusVariant.OnTime,
                    connectorPosition = TimelineConnectorPosition.Middle,
                    connectorAccent = HistoryTimelineConnectorAccent(1f, true, 0.5f),
                    focusFraction = 1f,
                    motionEnabled = true,
                    modeAccentColor = InfiniteColors.Primary,
                    modifier = Modifier.testTag("history-pill")
                )
            }
        }

        composeRule.onNodeWithText("22 July 2026").assertIsDisplayed()
        composeRule.onNodeWithText("08:00 - 17:00").assertIsDisplayed()
        composeRule.onNodeWithText("Head Office").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("On Time").assertIsDisplayed()
        composeRule.onNodeWithTag("history-pill")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "22 July 2026, 08:00 - 17:00, On Time"
                )
            )
    }

    @Test
    fun pillRemainsWithin320DpHostAtLargeFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(Modifier.width(320.dp).testTag("history-pill-host")) {
                        AttendanceHistoryTimelinePill(
                            nodeLabel = "22",
                            dateLabel = "22 July 2026",
                            timeRange = "08:00 - 17:00",
                            supportingText = "Head Office",
                            statusLabel = "On Time",
                            statusVariant = InfiniteStatusVariant.OnTime,
                            connectorPosition = TimelineConnectorPosition.Middle,
                            connectorAccent = HistoryTimelineConnectorAccent(1f, true, 0.5f),
                            focusFraction = 1f,
                            motionEnabled = true,
                            modeAccentColor = InfiniteColors.Primary,
                            modifier = Modifier.testTag("history-pill")
                        )
                    }
                }
            }
        }

        val host = composeRule.onNodeWithTag("history-pill-host").getUnclippedBoundsInRoot()
        val pill = composeRule.onNodeWithTag("history-pill").getUnclippedBoundsInRoot()
        assertTrue(
            pill.left >= host.left &&
                pill.top >= host.top &&
                pill.right <= host.right &&
                pill.bottom <= host.bottom
        )
    }
}
