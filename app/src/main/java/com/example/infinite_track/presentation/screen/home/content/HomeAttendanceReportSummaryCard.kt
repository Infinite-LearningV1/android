package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Orange_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Status_Approved
import com.example.infinite_track.presentation.theme.Status_Rejected
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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = Purple_500,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Attendance Report",
                        style = headline4,
                        color = Purple_500
                    )
                    Text(
                        text = periodInfo?.label ?: "This Month",
                        style = body2,
                        color = Purple_300
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (summaryState) {
                is UiState.Loading -> {
                    Text(
                        text = "Loading attendance summary...",
                        style = body2,
                        color = Purple_300
                    )
                }

                is UiState.Error -> {
                    Text(
                        text = summaryState.errorMessage,
                        style = body2,
                        color = Status_Rejected
                    )
                }

                is UiState.Success -> {
                    val summary = summaryState.data
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                        ) {
                            SummaryMetricCell(
                                title = "Attendance Rate",
                                value = summary.attendanceRateLabel ?: "—",
                                icon = Icons.Outlined.QueryStats,
                                accent = Purple_500,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(InfiniteColors.AttendanceReportGlassBorder)
                            )
                            SummaryMetricCell(
                                title = "Late",
                                value = "${summary.totalLate} times",
                                icon = Icons.Outlined.Schedule,
                                accent = Orange_500,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = InfiniteColors.AttendanceReportGlassBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                        ) {
                            SummaryMetricCell(
                                title = "Alpha",
                                value = "${summary.totalAlpha} day",
                                icon = Icons.Outlined.PersonOutline,
                                accent = Status_Rejected,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(InfiniteColors.AttendanceReportGlassBorder)
                            )
                            SummaryMetricCell(
                                title = "Work Hours",
                                value = summary.totalWorkHoursLabel ?: "—",
                                icon = Icons.Outlined.AccessTime,
                                accent = Status_Approved,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                is UiState.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = InfiniteColors.AttendanceReportGlassBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Report",
                    style = body1,
                    color = Blue_500
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Blue_500,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricCell(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title,
                style = body2,
                color = Purple_300
            )
        }
        Text(
            text = value,
            style = headline4,
            color = accent
        )
    }
}
