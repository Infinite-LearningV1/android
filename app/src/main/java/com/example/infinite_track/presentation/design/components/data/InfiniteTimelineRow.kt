package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

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
    supportingColor: Color = InfiniteColors.AttendanceReportMutedText,
    supportingMaxLines: Int = 1,
    supportingOverflow: TextOverflow = TextOverflow.Ellipsis
) {
    val accentColor = timelineAccentColor(statusVariant)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineMarker(
            connectorPosition = connectorPosition,
            leadingIcon = leadingIcon,
            accentColor = accentColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = InfiniteColors.Surface.copy(alpha = 0.64f),
            border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leadingIcon?.let {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = InfiniteColors.Surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$dateLabel · $modeLabel · $timeRange",
                        color = InfiniteColors.Text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    supportingText?.let {
                        Text(
                            text = it,
                            color = supportingColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = supportingMaxLines,
                            overflow = supportingOverflow
                        )
                    }
                }

                InfiniteStatusPill(
                    label = statusLabel,
                    variant = statusVariant
                )
            }
        }
    }
}

@Composable
private fun TimelineMarker(
    connectorPosition: TimelineConnectorPosition,
    leadingIcon: ImageVector?,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(2.dp, 16.dp)) {
            if (connectorPosition == TimelineConnectorPosition.Middle || connectorPosition == TimelineConnectorPosition.Last) {
                drawLine(
                    accentColor.copy(alpha = 0.55f),
                    Offset(size.width / 2, 0f),
                    Offset(size.width / 2, size.height),
                    strokeWidth = size.width
                )
            }
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(InfiniteColors.Surface, CircleShape)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(accentColor, CircleShape)
            )
        }
        Canvas(modifier = Modifier.size(2.dp, 16.dp)) {
            if (connectorPosition == TimelineConnectorPosition.Middle || connectorPosition == TimelineConnectorPosition.First) {
                drawLine(
                    accentColor.copy(alpha = 0.55f),
                    Offset(size.width / 2, 0f),
                    Offset(size.width / 2, size.height),
                    strokeWidth = size.width
                )
            }
        }
    }
}

private fun timelineAccentColor(variant: InfiniteStatusVariant): Color = when (variant) {
    InfiniteStatusVariant.Active -> InfiniteColors.Primary
    InfiniteStatusVariant.OnTime -> InfiniteColors.Accent
    InfiniteStatusVariant.Late -> InfiniteColors.Secondary
    InfiniteStatusVariant.Alpha -> InfiniteColors.Error
    else -> InfiniteColors.Primary
}
