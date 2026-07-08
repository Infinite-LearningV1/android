package com.example.infinite_track.presentation.design.components.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun InfiniteAttendanceTimelineCard(
    dateLabel: String,
    modeLabel: String,
    timeRange: String,
    statusLabel: String,
    statusVariant: InfiniteStatusVariant,
    modifier: Modifier = Modifier,
    connectorPosition: TimelineConnectorPosition = TimelineConnectorPosition.None,
    workHourLabel: String? = null,
    locationLabel: String? = null
) {
    InfiniteGlassReportCard(modifier = modifier) {
        InfiniteTimelineRow(
            dateLabel = dateLabel,
            modeLabel = modeLabel,
            timeRange = timeRange,
            statusLabel = statusLabel,
            statusVariant = statusVariant,
            connectorPosition = connectorPosition,
            supportingContent = {
                workHourLabel?.let { label ->
                    Text(
                        text = label,
                        color = InfiniteColors.Text.copy(alpha = 0.54f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                locationLabel?.takeIf { it.isNotBlank() }?.let { location ->
                    Text(
                        text = location,
                        color = InfiniteColors.Text.copy(alpha = 0.46f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}
