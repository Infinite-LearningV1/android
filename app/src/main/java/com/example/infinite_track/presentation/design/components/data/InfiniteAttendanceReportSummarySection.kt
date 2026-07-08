package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
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
    attendanceRateSubtitle: String? = null,
    workHoursSubtitle: String? = null,
    title: String = "Report Summary",
    subtitle: String? = null
) {
    InfiniteGlassReportCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfiniteSectionHeader(
                title = title,
                subtitle = subtitle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfiniteMetricCard(
                    title = "Attendance Rate",
                    value = attendanceRateValue,
                    subtitle = attendanceRateSubtitle,
                    icon = Icons.Outlined.QueryStats,
                    semantic = InfiniteSemantic.Primary,
                    modifier = Modifier.weight(1f)
                )
                InfiniteMetricCard(
                    title = "Work Hours",
                    value = workHoursValue,
                    subtitle = workHoursSubtitle,
                    icon = Icons.Outlined.AccessTime,
                    semantic = InfiniteSemantic.Info,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfiniteMetricCard(
                    title = "Late",
                    value = lateCount.toString(),
                    subtitle = null,
                    icon = Icons.Outlined.Schedule,
                    semantic = InfiniteSemantic.Warning,
                    modifier = Modifier.weight(1f)
                )
                InfiniteMetricCard(
                    title = "Alpha",
                    value = alphaCount.toString(),
                    subtitle = null,
                    icon = Icons.Outlined.PersonOutline,
                    semantic = InfiniteSemantic.Error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
