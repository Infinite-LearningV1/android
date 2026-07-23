package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

@Composable
fun AttendanceHistoryTimelinePill(
    nodeLabel: String,
    dateLabel: String,
    timeRange: String,
    supportingText: String?,
    statusLabel: String,
    statusVariant: InfiniteStatusVariant,
    connectorPosition: TimelineConnectorPosition,
    connectorAccent: HistoryTimelineConnectorAccent,
    focusFraction: Float,
    motionEnabled: Boolean,
    modeAccentColor: Color,
    modifier: Modifier = Modifier
) {
    val transform = resolveHistoryFocusTransform(focusFraction, motionEnabled)
    val density = LocalDensity.current
    val shape = RoundedCornerShape(18.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = "$dateLabel, $timeRange, $statusLabel"
            }
    ) {
        val compact = maxWidth < 360.dp || density.fontScale >= 1.5f
        val nodeDiameter = maxOf(
            32.dp,
            with(density) { body1.lineHeight.toDp() } + InfiniteSpacing.Default.sm
        )
        val railWidth = nodeDiameter + InfiniteSpacing.Default.xs

        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = railWidth + InfiniteSpacing.Default.sm,
                        top = InfiniteSpacing.Default.xs,
                        bottom = InfiniteSpacing.Default.xs
                    )
                    .graphicsLayer {
                        alpha = transform.alpha
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationY = with(density) { transform.translationYDp.dp.toPx() }
                        shadowElevation = with(density) { transform.elevationDp.dp.toPx() }
                        this.shape = shape
                        clip = false
                    },
                shape = shape,
                color = InfiniteColors.AttendanceReportGlassSurface,
                border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder)
            ) {
                Box(Modifier.padding(InfiniteSpacing.Default.lg)) {
                    val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                        Column(
                            modifier = copyModifier.testTag("history-pill-copy"),
                            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                        ) {
                            Text(
                                text = dateLabel,
                                style = body1,
                                color = InfiniteColors.Text
                            )
                            Text(
                                text = timeRange,
                                style = body1,
                                color = InfiniteColors.AttendanceReportBodyText
                            )
                            supportingText?.let {
                                Text(
                                    text = it,
                                    style = body2,
                                    color = InfiniteColors.AttendanceReportMutedText
                                )
                            }
                        }
                    }
                    val status: @Composable () -> Unit = {
                        Box(Modifier.testTag("history-pill-status")) {
                            InfiniteStatusPill(
                                label = statusLabel,
                                variant = statusVariant,
                                size = InfiniteSize.Small,
                                useSharedRequestPalette = true
                            )
                        }
                    }
                    if (compact) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
                        ) {
                            copy(Modifier.fillMaxWidth())
                            status()
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
                        ) {
                            copy(Modifier.weight(1f))
                            status()
                        }
                    }
                }
            }
            Box(Modifier.matchParentSize()) {
                HistoryTimelineRail(
                    nodeLabel = nodeLabel,
                    nodeDiameter = nodeDiameter,
                    connectorPosition = connectorPosition,
                    connectorAccent = connectorAccent,
                    accent = modeAccentColor,
                    modifier = Modifier
                        .width(railWidth)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun HistoryTimelineRail(
    nodeLabel: String,
    nodeDiameter: Dp,
    connectorPosition: TimelineConnectorPosition,
    connectorAccent: HistoryTimelineConnectorAccent,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val showTop = connectorPosition == TimelineConnectorPosition.Middle ||
        connectorPosition == TimelineConnectorPosition.Last
    val showBottom = connectorPosition == TimelineConnectorPosition.Middle ||
        connectorPosition == TimelineConnectorPosition.First

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width / 2f
            val centerY = size.height / 2f
            val stroke = 2.dp.toPx()
            val muted = InfiniteColors.Neutral.copy(alpha = 0.22f)
            if (showTop) {
                drawLine(muted, Offset(x, 0f), Offset(x, centerY), stroke)
            }
            if (showBottom) {
                drawLine(muted, Offset(x, centerY), Offset(x, size.height), stroke)
            }
            if (showTop && connectorAccent.topFraction > 0f) {
                drawLine(
                    accent,
                    Offset(x, 0f),
                    Offset(x, centerY * connectorAccent.topFraction.coerceIn(0f, 1f)),
                    stroke
                )
            }
            if (showBottom && connectorAccent.bottomFraction > 0f) {
                drawLine(
                    accent,
                    Offset(x, centerY),
                    Offset(
                        x,
                        centerY + (size.height - centerY) *
                            connectorAccent.bottomFraction.coerceIn(0f, 1f)
                    ),
                    stroke
                )
            }
        }
        Surface(
            modifier = Modifier
                .size(nodeDiameter)
                .testTag("history-pill-node"),
            shape = CircleShape,
            color = accent.copy(alpha = if (connectorAccent.nodeComplete) 0.20f else 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = nodeLabel,
                    style = body1,
                    color = accent,
                    modifier = Modifier.testTag("history-pill-node-label")
                )
            }
        }
    }
}
