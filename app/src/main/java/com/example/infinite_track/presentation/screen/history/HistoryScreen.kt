package com.example.infinite_track.presentation.screen.history

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.data.mapper.attendance.toReportDateLabel
import com.example.infinite_track.data.mapper.attendance.toReportStatus
import com.example.infinite_track.data.mapper.attendance.toReportTimeRangeLabel
import com.example.infinite_track.data.mapper.attendance.toReportTotalWorkHoursLabel
import com.example.infinite_track.data.mapper.attendance.toReportWorkHourLabel
import com.example.infinite_track.data.mapper.attendance.toReportWorkModeLabel
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceReportStatusKind
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceModeDistributionCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendancePeriodFilterCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportActionsCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportNoticeCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportSummarySection
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceTimelineCard
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.data.TimelineConnectorPosition
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

private enum class ReportAction {
    Preview,
    Share
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val pendingReportAction = remember { mutableStateOf<ReportAction?>(null) }
    val refreshDragDistance = remember { mutableStateOf(0f) }
    val pullToRefreshConnection = remember(lazyListState, uiState.isLoading, uiState.isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && lazyListState.isAtTop() && !uiState.isLoading && !uiState.isRefreshing) {
                    refreshDragDistance.value += available.y
                    if (refreshDragDistance.value >= PullToRefreshThresholdPx) {
                        refreshDragDistance.value = 0f
                        viewModel.refreshHistory()
                    }
                }
                if (available.y < 0) {
                    refreshDragDistance.value = 0f
                }
                return Offset.Zero
            }
        }
    }
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                lastVisibleItem.index >= uiState.records.size - 3 &&
                uiState.canLoadMore &&
                !uiState.isLoadingMore &&
                !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) viewModel.loadNextPage()
    }

    LaunchedEffect(uiState.exportState) {
        when (val exportState = uiState.exportState) {
            is ReportExportUiState.Success -> {
                try {
                    when (pendingReportAction.value) {
                        ReportAction.Preview -> {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(exportState.localUri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        ReportAction.Share -> {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, exportState.localUri)
                                putExtra(Intent.EXTRA_TITLE, exportState.fileName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report"))
                        }
                        null -> Unit
                    }
                } catch (_: ActivityNotFoundException) {
                    viewModel.clearExportState()
                    pendingReportAction.value = null
                }
                pendingReportAction.value = null
                viewModel.clearExportState()
            }
            is ReportExportUiState.Error -> {
                pendingReportAction.value = null
            }
            ReportExportUiState.Downloading,
            ReportExportUiState.Idle -> Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = InfiniteColors.Transparent
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullToRefreshConnection),
            contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfiniteAttendancePeriodFilterCard(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::onFilterChanged,
                    title = "My Attendance Report",
                    subtitle = uiState.periodInfo?.label
                )
            }

            if (uiState.selectedPeriod == AttendancePeriod.CUSTOM) {
                item {
                    InfiniteAttendanceReportNoticeCard(
                        title = "Custom range needs verification",
                        message = "Date range picker is not available in this branch yet. The Custom filter is visible for the report contract, but runtime date-range behavior still needs verification."
                    )
                }
            }

            if (uiState.isRefreshing) {
                item {
                    InfiniteGlassReportCard {
                        InfiniteLoadingState(message = "Refreshing attendance report...")
                    }
                }
            }

            when {
                uiState.selectedPeriod == AttendancePeriod.CUSTOM -> {
                    item {
                        InfiniteGlassReportCard {
                            InfiniteEmptyState(
                                title = "Custom report not available yet",
                                message = "Choose Daily, Weekly, or Monthly to load backend report data while Custom range support is awaiting verification."
                            )
                        }
                    }
                }

                uiState.isLoading && uiState.records.isEmpty() -> {
                    item {
                        InfiniteGlassReportCard {
                            InfiniteLoadingState(message = "Loading personal attendance report...")
                        }
                    }
                }

                uiState.error != null && uiState.records.isEmpty() -> {
                    item {
                        InfiniteGlassReportCard {
                            InfiniteErrorState(
                                title = "Report unavailable",
                                message = uiState.error ?: "Unable to load attendance report."
                            )
                        }
                    }
                }

                else -> {
                    item {
                        InfiniteAttendanceReportSummarySection(
                            attendanceRateValue = uiState.summary?.attendanceRateLabel ?: "—",
                            workHoursValue = remember(uiState.records, uiState.summary?.totalWorkHoursLabel) {
                                uiState.records.toReportTotalWorkHoursLabel(uiState.summary?.totalWorkHoursLabel)
                            },
                            lateCount = uiState.summary?.totalLate ?: 0,
                            alphaCount = uiState.summary?.totalAlpha ?: 0,
                            subtitle = uiState.periodInfo?.label
                        )
                    }

                    item {
                        InfiniteAttendanceModeDistributionCard(
                            wfoCount = uiState.summary?.modeDistribution?.wfo?.count ?: (uiState.summary?.totalWfo ?: 0),
                            wfaCount = uiState.summary?.modeDistribution?.wfa?.count ?: (uiState.summary?.totalWfa ?: 0),
                            wfhCount = uiState.summary?.modeDistribution?.wfh?.count,
                            subtitle = null,
                            unavailableModeMessage = null
                        )
                    }

                    item {
                        InfiniteAttendanceReportActionsCard(
                            selectedPeriod = uiState.selectedPeriod,
                            exportEnabled = uiState.exportState !is ReportExportUiState.Downloading,
                            shareEnabled = uiState.exportState !is ReportExportUiState.Downloading,
                            onExportClick = {
                                pendingReportAction.value = ReportAction.Preview
                                viewModel.previewReportPdf()
                            },
                            onShareClick = {
                                pendingReportAction.value = ReportAction.Share
                                viewModel.exportReportPdf()
                            },
                            subtitle = null
                        )
                    }

                    if (uiState.exportState is ReportExportUiState.Error) {
                        item {
                            InfiniteAttendanceReportNoticeCard(
                                title = "PDF unavailable",
                                message = (uiState.exportState as ReportExportUiState.Error).message
                            )
                        }
                    }

                    item {
                        InfiniteSectionHeader(
                            title = "Attendance Timeline",
                            subtitle = null
                        )
                    }

                    if (uiState.records.isEmpty()) {
                        item {
                            InfiniteGlassReportCard {
                                InfiniteEmptyState(
                                    title = "No attendance records found",
                                    message = "Backend did not return attendance records for this period."
                                )
                            }
                        }
                    } else {
                        itemsIndexed(uiState.records) { index, record ->
                            ReportTimelineRow(
                                record = record,
                                connectorPosition = when {
                                    uiState.records.size == 1 -> TimelineConnectorPosition.None
                                    index == 0 -> TimelineConnectorPosition.First
                                    index == uiState.records.lastIndex -> TimelineConnectorPosition.Last
                                    else -> TimelineConnectorPosition.Middle
                                }
                            )
                        }
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            InfiniteGlassReportCard {
                                InfiniteLoadingState(message = "Loading more attendance records...")
                            }
                        }
                    }

                    if (uiState.error != null && uiState.records.isNotEmpty()) {
                        item {
                            InfiniteAttendanceReportNoticeCard(
                                title = "Some records may be missing",
                                message = uiState.error ?: "Unable to load more attendance records."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTimelineRow(
    record: AttendanceRecord,
    connectorPosition: TimelineConnectorPosition
) {
    val status = record.toReportStatus()
    InfiniteAttendanceTimelineCard(
        dateLabel = record.toReportDateLabel(),
        modeLabel = record.toReportWorkModeLabel(),
        timeRange = record.toReportTimeRangeLabel(),
        statusLabel = status.label,
        statusVariant = status.kind.toInfiniteStatusVariant(),
        connectorPosition = connectorPosition,
        workHourLabel = record.toReportWorkHourLabel(),
        locationLabel = record.location
    )
}

private fun AttendanceReportStatusKind.toInfiniteStatusVariant(): InfiniteStatusVariant = when (this) {
    AttendanceReportStatusKind.ActiveSession -> InfiniteStatusVariant.Active
    AttendanceReportStatusKind.OnTime -> InfiniteStatusVariant.OnTime
    AttendanceReportStatusKind.Late -> InfiniteStatusVariant.Late
    AttendanceReportStatusKind.Alpha -> InfiniteStatusVariant.Alpha
    AttendanceReportStatusKind.Unknown -> InfiniteStatusVariant.Unknown
    AttendanceReportStatusKind.Neutral -> InfiniteStatusVariant.Neutral
}

private const val PullToRefreshThresholdPx = 160f

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
