package com.example.infinite_track.presentation.screen.history

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceModeDistributionCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendancePeriodFilterCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportActionsCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportNoticeCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportSummaryCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceTimelineSection
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.utils.UiState

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

    // MainScreen already applies scaffold/bottom-bar padding.
    // Match Home content insets so History does not double-pad and clip while scrolling.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = InfiniteColors.Transparent
    ) { _ ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshConnection),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
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

            if (uiState.isRefreshing) {
                item {
                    InfiniteGlassReportCard {
                        InfiniteLoadingState(message = "Refreshing attendance report...")
                    }
                }
            }

            when {
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
                        InfiniteAttendanceReportSummaryCard(
                            summaryState = uiState.summary?.let { UiState.Success(it) } ?: UiState.Loading,
                            periodInfo = uiState.periodInfo,
                            showViewReportAction = false
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
                        InfiniteAttendanceTimelineSection(
                            attendanceState = UiState.Success(uiState.records),
                            onSeeAllClick = {},
                            title = "Attendance Timeline",
                            maxItems = uiState.records.size.coerceAtLeast(1),
                            showModeLabel = false,
                            showExternalHeader = false,
                            showSeeAllAction = false
                        )
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

private const val PullToRefreshThresholdPx = 160f

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
