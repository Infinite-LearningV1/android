package com.example.infinite_track.presentation.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.domain.model.attendance.AttendancePeriod
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.presentation.mapper.attendance.fullDateLabel
import com.example.infinite_track.presentation.mapper.attendance.reportStatus
import com.example.infinite_track.presentation.mapper.attendance.timeRangeLabel
import com.example.infinite_track.presentation.mapper.attendance.totalWorkHoursLabel
import com.example.infinite_track.presentation.mapper.attendance.workHourLabel
import com.example.infinite_track.presentation.mapper.attendance.workModeLabel
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceModeDistributionCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendancePeriodFilterCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportActionsCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportHeroCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportNoticeCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportSummarySection
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceTimelineCard
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.data.TimelineConnectorPosition
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = InfiniteColors.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InfiniteColors.AttendanceReportBackground)
                .padding(innerPadding)
        ) {
            DecorativeOrb(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp),
                color = InfiniteColors.Accent.copy(alpha = 0.32f),
                size = 132
            )
            DecorativeOrb(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 128.dp, start = 18.dp),
                color = InfiniteColors.Secondary.copy(alpha = 0.20f),
                size = 84
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { InfiniteAttendanceReportHeroCard() }

                item {
                    InfiniteAttendancePeriodFilterCard(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = viewModel::onFilterChanged
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
                                attendanceRateValue = "—",
                                workHoursValue = remember(uiState.records) { uiState.records.totalWorkHoursLabel() },
                                lateCount = uiState.summary?.totalLate ?: 0,
                                alphaCount = uiState.summary?.totalAlpha ?: 0
                            )
                        }

                        item {
                            InfiniteAttendanceModeDistributionCard(
                                wfoCount = uiState.summary?.totalWfo ?: 0,
                                wfaCount = uiState.summary?.totalWfa ?: 0
                            )
                        }

                        item {
                            InfiniteAttendanceReportActionsCard(selectedPeriod = uiState.selectedPeriod)
                        }

                        item {
                            InfiniteSectionHeader(
                                title = "Attendance Timeline",
                                subtitle = "Recent attendance records only"
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
}

@Composable
private fun ReportTimelineRow(
    record: AttendanceRecord,
    connectorPosition: TimelineConnectorPosition
) {
    val status = record.reportStatus()
    InfiniteAttendanceTimelineCard(
        dateLabel = record.fullDateLabel(),
        modeLabel = record.workModeLabel(),
        timeRange = record.timeRangeLabel(),
        statusLabel = status.label,
        statusVariant = status.variant,
        connectorPosition = connectorPosition,
        workHourLabel = record.workHourLabel(),
        locationLabel = record.location
    )
}

@Composable
private fun DecorativeOrb(
    modifier: Modifier,
    color: Color,
    size: Int
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}
