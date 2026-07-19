package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500

enum class TimelineConnectorPosition {
    None,
    First,
    Middle,
    Last
}

@Composable
fun InfiniteTimelineRow(
    dateLabel: String,
    modeLabel: String,
    timeRange: String,
    statusLabel: String,
    statusVariant: InfiniteStatusVariant,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = Icons.Rounded.CalendarMonth,
    connectorPosition: TimelineConnectorPosition = TimelineConnectorPosition.None,
    onClick: (() -> Unit)? = null,
    supportingText: String? = null,
    supportingColor: Color = Purple_300,
    supportingMaxLines: Int = 1,
    supportingOverflow: TextOverflow = TextOverflow.Ellipsis,
    showModeLabel: Boolean = false,
    modeAccentColor: Color? = null
) {
    val accentColor = modeAccentColor ?: InfiniteColors.Primary
    val primaryLine = if (showModeLabel && modeLabel.isNotBlank()) {
        "$dateLabel · $modeLabel · $timeRange"
    } else {
        "$dateLabel · $timeRange"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineRail(
            connectorPosition = connectorPosition,
            accentColor = accentColor,
            leadingIcon = leadingIcon
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = primaryLine,
                style = body1,
                color = Purple_500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = body2,
                    color = supportingColor,
                    maxLines = supportingMaxLines,
                    overflow = supportingOverflow
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        InfiniteStatusPill(
            label = statusLabel,
            variant = statusVariant,
            size = InfiniteSize.Small,
            useSharedRequestPalette = true
        )
    }
}

@Composable
private fun TimelineRail(
    connectorPosition: TimelineConnectorPosition,
    accentColor: Color,
    leadingIcon: ImageVector?
) {
    Box(
        modifier = Modifier
            .width(26.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
            val x = size.width / 2f
            val showTop = connectorPosition == TimelineConnectorPosition.Middle ||
                connectorPosition == TimelineConnectorPosition.Last
            val showBottom = connectorPosition == TimelineConnectorPosition.Middle ||
                connectorPosition == TimelineConnectorPosition.First
            if (showTop) {
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height / 2f),
                    strokeWidth = size.width
                )
            }
            if (showBottom) {
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(x, size.height / 2f),
                    end = Offset(x, size.height),
                    strokeWidth = size.width
                )
            }
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .background(accentColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon ?: Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
