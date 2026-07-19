package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.data.InfiniteMetricCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.utils.UiState

@Composable
fun HomeAttendanceReportSummaryCard(
    summaryState: UiState<AttendanceSummaryInfo>,
    periodInfo: AttendancePeriodInfo?,
    onViewReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteGlassReportCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewReportClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = InfiniteColors.Primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Attendance Report",
                        style = MaterialTheme.typography.titleMedium,
                        color = InfiniteColors.Text,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = periodInfo?.label ?: "This Month",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.AttendanceReportMutedText
                    )
                }
            }

            when (summaryState) {
                is UiState.Loading -> {
                    Text(
                        text = "Loading attendance summary...",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.AttendanceReportMutedText
                    )
                }

                is UiState.Error -> {
                    Text(
                        text = summaryState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.Error
                    )
                }

                is UiState.Success -> {
                    val summary = summaryState.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfiniteMetricCard(
                            title = "Attendance Rate",
                            value = summary.attendanceRateLabel ?: "—",
                            subtitle = null,
                            icon = Icons.Outlined.QueryStats,
                            semantic = InfiniteSemantic.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        InfiniteMetricCard(
                            title = "Late",
                            value = "${summary.totalLate} times",
                            subtitle = null,
                            icon = Icons.Outlined.Schedule,
                            semantic = InfiniteSemantic.Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfiniteMetricCard(
                            title = "Alpha",
                            value = "${summary.totalAlpha} day",
                            subtitle = null,
                            icon = Icons.Outlined.PersonOutline,
                            semantic = InfiniteSemantic.Error,
                            modifier = Modifier.weight(1f)
                        )
                        InfiniteMetricCard(
                            title = "Work Hours",
                            value = summary.totalWorkHoursLabel ?: "—",
                            subtitle = null,
                            icon = Icons.Outlined.AccessTime,
                            semantic = InfiniteSemantic.Info,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is UiState.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Report",
                    style = MaterialTheme.typography.labelLarge,
                    color = InfiniteColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = InfiniteColors.Primary
                )
            }
        }
    }
}
