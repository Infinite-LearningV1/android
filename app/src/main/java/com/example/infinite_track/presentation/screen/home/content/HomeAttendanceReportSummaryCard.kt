package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportSummaryCard
import com.example.infinite_track.utils.UiState

/**
 * Home-facing wrapper so existing call sites stay stable while the shared
 * report summary component lives in the global design/components package.
 */
@Composable
fun HomeAttendanceReportSummaryCard(
    summaryState: UiState<AttendanceSummaryInfo>,
    periodInfo: AttendancePeriodInfo?,
    onViewReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteAttendanceReportSummaryCard(
        summaryState = summaryState,
        periodInfo = periodInfo,
        modifier = modifier,
        showViewReportAction = true,
        onViewReportClick = onViewReportClick
    )
}
