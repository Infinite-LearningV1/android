package com.example.infinite_track.presentation.design.components.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.attendanceModeColor

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
    locationLabel: String? = null,
    showModeLabel: Boolean = false,
    modeKey: String? = null
) {
    InfiniteTimelineRow(
        dateLabel = dateLabel,
        modeLabel = modeLabel,
        timeRange = timeRange,
        statusLabel = statusLabel,
        statusVariant = statusVariant,
        connectorPosition = connectorPosition,
        modifier = modifier,
        supportingText = locationLabel?.takeIf { it.isNotBlank() } ?: workHourLabel,
        supportingMaxLines = 1,
        supportingOverflow = TextOverflow.Ellipsis,
        supportingColor = InfiniteColors.AttendanceReportMutedText,
        showModeLabel = showModeLabel,
        modeAccentColor = attendanceModeColor(modeKey ?: modeLabel)
    )
}
