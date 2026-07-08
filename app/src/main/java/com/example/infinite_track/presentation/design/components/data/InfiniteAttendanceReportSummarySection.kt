package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Composable
fun InfiniteAttendanceReportSummarySection(
    attendanceRateValue: String,
    workHoursValue: String,
    lateCount: Int,
    alphaCount: Int,
    modifier: Modifier = Modifier,
    attendanceRateSubtitle: String = "Backend denominator unavailable",
    workHoursSubtitle: String = "From returned work_hour",
    title: String = "Report Summary",
    subtitle: String = "Backend summary with deterministic Android display"
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfiniteSectionHeader(
            title = title,
            subtitle = subtitle
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfiniteMetricCard(
                title = "Attendance Rate",
                value = attendanceRateValue,
                subtitle = attendanceRateSubtitle,
                semantic = InfiniteSemantic.Primary,
                modifier = Modifier.weight(1f)
            )
            InfiniteMetricCard(
                title = "Work Hours",
                value = workHoursValue,
                subtitle = workHoursSubtitle,
                semantic = InfiniteSemantic.Info,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfiniteMetricCard(
                title = "Late",
                value = lateCount.toString(),
                subtitle = "Backend summary",
                semantic = InfiniteSemantic.Warning,
                modifier = Modifier.weight(1f)
            )
            InfiniteMetricCard(
                title = "Alpha",
                value = alphaCount.toString(),
                subtitle = "Backend summary",
                semantic = InfiniteSemantic.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
