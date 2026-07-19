package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.attendanceModeColor

@Composable
fun InfiniteAttendanceModeDistributionCard(
    wfoCount: Int,
    wfaCount: Int,
    modifier: Modifier = Modifier,
    wfhCount: Int? = null,
    title: String = "Attendance Mode",
    subtitle: String? = "WFO and WFA distribution from backend summary",
    unavailableModeMessage: String? = "WFH is not shown because total_wfh is not available in the current Android model."
) {
    val totalKnown = wfoCount + wfaCount + (wfhCount ?: 0)

    InfiniteGlassReportCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfiniteSectionHeader(
                title = title,
                subtitle = subtitle
            )
            InfiniteAttendanceModeDistributionRow(
                label = "WFO",
                count = wfoCount,
                total = totalKnown,
                color = attendanceModeColor("wfo"),
                unitLabel = "days"
            )
            InfiniteAttendanceModeDistributionRow(
                label = "WFA",
                count = wfaCount,
                total = totalKnown,
                color = attendanceModeColor("wfa"),
                unitLabel = "days"
            )
            wfhCount?.let { count ->
                InfiniteAttendanceModeDistributionRow(
                    label = "WFH",
                    count = count,
                    total = totalKnown,
                    color = attendanceModeColor("wfh"),
                    unitLabel = "days"
                )
            } ?: unavailableModeMessage?.let { message ->
                Text(
                    text = message,
                    color = InfiniteColors.AttendanceReportMutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun InfiniteAttendanceModeDistributionRow(
    label: String,
    count: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = InfiniteColors.Primary,
    unitLabel: String = "records"
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = InfiniteColors.Text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count $unitLabel",
                color = InfiniteColors.AttendanceReportMutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(InfiniteColors.Surface.copy(alpha = 0.62f), RoundedCornerShape(999.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}
