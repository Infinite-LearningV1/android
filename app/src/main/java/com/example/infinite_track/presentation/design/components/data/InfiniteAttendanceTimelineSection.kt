package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.data.mapper.attendance.toReportDateLabel
import com.example.infinite_track.data.mapper.attendance.toReportStatus
import com.example.infinite_track.data.mapper.attendance.toReportTimeRangeLabel
import com.example.infinite_track.data.mapper.attendance.toReportWorkHourLabel
import com.example.infinite_track.data.mapper.attendance.toReportWorkModeLabel
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceReportStatusKind
import com.example.infinite_track.presentation.components.button.SeeAllButton
import com.example.infinite_track.presentation.components.empty.EmptyListAnimation
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.utils.UiState

@Composable
fun InfiniteAttendanceTimelineSection(
    attendanceState: UiState<List<AttendanceRecord>>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Attendance Timeline",
    maxItems: Int = 3,
    showModeLabel: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SeeAllButton(
            label = title,
            onClickButton = onSeeAllClick
        )

        when (attendanceState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingAnimation()
                }
            }

            is UiState.Success -> {
                if (attendanceState.data.isEmpty()) {
                    InfiniteGlassReportCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmptyListAnimation(modifier = Modifier.size(120.dp))
                            Text(
                                text = "No attendance records found",
                                style = body2,
                                color = Purple_300
                            )
                        }
                    }
                } else {
                    val timelineItems = attendanceState.data.take(maxItems)
                    InfiniteGlassReportCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            timelineItems.forEachIndexed { index, attendance ->
                                val status = attendance.toReportStatus()
                                InfiniteAttendanceTimelineCard(
                                    dateLabel = attendance.toReportDateLabel(),
                                    modeLabel = attendance.toReportWorkModeLabel(),
                                    timeRange = attendance.toReportTimeRangeLabel(),
                                    statusLabel = status.label,
                                    statusVariant = status.kind.toTimelineVariant(),
                                    connectorPosition = connectorPositionFor(index, timelineItems.size),
                                    workHourLabel = attendance.toReportWorkHourLabel(),
                                    locationLabel = attendance.location,
                                    showModeLabel = showModeLabel,
                                    modeKey = attendance.modeKey ?: attendance.modeLabel
                                )
                                if (index != timelineItems.lastIndex) {
                                    HorizontalDivider(
                                        color = InfiniteColors.AttendanceReportGlassBorder
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is UiState.Error -> {
                InfiniteGlassReportCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyListAnimation(modifier = Modifier.size(120.dp))
                        Text(
                            text = attendanceState.errorMessage,
                            style = body2,
                            color = Purple_300
                        )
                    }
                }
            }

            is UiState.Idle -> Unit
        }
    }
}

private fun connectorPositionFor(index: Int, totalCount: Int): TimelineConnectorPosition = when {
    totalCount <= 1 -> TimelineConnectorPosition.None
    index == 0 -> TimelineConnectorPosition.First
    index == totalCount - 1 -> TimelineConnectorPosition.Last
    else -> TimelineConnectorPosition.Middle
}

private fun AttendanceReportStatusKind.toTimelineVariant(): InfiniteStatusVariant = when (this) {
    AttendanceReportStatusKind.ActiveSession -> InfiniteStatusVariant.Active
    AttendanceReportStatusKind.OnTime -> InfiniteStatusVariant.OnTime
    AttendanceReportStatusKind.Late -> InfiniteStatusVariant.Late
    AttendanceReportStatusKind.Alpha -> InfiniteStatusVariant.Alpha
    AttendanceReportStatusKind.Unknown -> InfiniteStatusVariant.Unknown
    AttendanceReportStatusKind.Neutral -> InfiniteStatusVariant.Neutral
}
