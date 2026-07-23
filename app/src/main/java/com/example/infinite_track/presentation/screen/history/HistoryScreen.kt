package com.example.infinite_track.presentation.screen.history

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceModeDistributionCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendancePeriodFilterCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportActionsCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportNoticeCard
import com.example.infinite_track.presentation.design.components.data.InfiniteAttendanceReportSummaryCard
import com.example.infinite_track.presentation.design.components.data.InfiniteGlassReportCard
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.utils.UiState
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val motionEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
    val recordIndexById = remember(uiState.records) {
        uiState.records.mapIndexed { index, record -> record.id to index }.toMap()
    }
    var historyViewport by remember(recordIndexById) {
        mutableStateOf(HistoryViewportSnapshot())
    }
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
    LaunchedEffect(lazyListState, recordIndexById, uiState.records.size) {
        snapshotFlow {
            val layout = lazyListState.layoutInfo
            val viewportStart = layout.viewportStartOffset.toFloat()
            val viewportEnd = layout.viewportEndOffset.toFloat()
            val viewportCenter = (viewportStart + viewportEnd) / 2f
            val viewportHeight = (viewportEnd - viewportStart).coerceAtLeast(0f)
            val visible = layout.visibleItemsInfo.mapNotNull { info ->
                val recordId = historyRecordIdFromKey(info.key) ?: return@mapNotNull null
                val recordIndex = recordIndexById[recordId] ?: return@mapNotNull null
                val center = info.offset + info.size / 2f
                Triple(historyItemKey(recordId), recordIndex, center)
            }
            val visibleMathItems = visible.map { (_, index, center) ->
                VisibleHistoryItem(recordIndex = index, center = center)
            }
            HistoryViewportSnapshot(
                focusByKey = visible.associate { (key, _, center) ->
                    key to calculateHistoryFocusFraction(center, viewportCenter, viewportHeight)
                },
                timelineProgress = calculateHistoryTimelineProgress(
                    visibleItems = visibleMathItems,
                    totalRecordCount = uiState.records.size,
                    viewportCenter = viewportCenter
                ),
                lastVisibleRecordIndex = visibleMathItems.maxOfOrNull { it.recordIndex }
            )
        }
            .distinctUntilChanged()
            .collect { historyViewport = it }
    }
    val shouldLoadMore = remember(
        historyViewport.lastVisibleRecordIndex,
        uiState.records.size,
        uiState.canLoadMore,
        uiState.isLoadingMore,
        uiState.isLoading
    ) {
        derivedStateOf {
            shouldLoadMoreHistory(
                lastVisibleRecordIndex = historyViewport.lastVisibleRecordIndex,
                recordCount = uiState.records.size,
                canLoadMore = uiState.canLoadMore,
                loading = uiState.isLoadingMore || uiState.isLoading
            )
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
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
        ) {
            historySectionItem(topPadding = 0.dp) {
                InfiniteAttendancePeriodFilterCard(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::onFilterChanged,
                    title = "My Attendance Report",
                    subtitle = uiState.periodInfo?.label
                )
            }

            if (uiState.isRefreshing) {
                historySectionItem {
                    InfiniteGlassReportCard {
                        InfiniteLoadingState(message = "Refreshing attendance report...")
                    }
                }
            }

            when {
                uiState.isLoading && uiState.records.isEmpty() -> {
                    historySectionItem {
                        InfiniteGlassReportCard {
                            InfiniteLoadingState(message = "Loading personal attendance report...")
                        }
                    }
                }

                uiState.error != null && uiState.records.isEmpty() -> {
                    historySectionItem {
                        InfiniteGlassReportCard {
                            InfiniteErrorState(
                                title = "Report unavailable",
                                message = uiState.error ?: "Unable to load attendance report."
                            )
                        }
                    }
                }

                else -> {
                    historySectionItem {
                        InfiniteAttendanceReportSummaryCard(
                            summaryState = uiState.summary?.let { UiState.Success(it) } ?: UiState.Loading,
                            periodInfo = uiState.periodInfo,
                            showViewReportAction = false
                        )
                    }

                    historySectionItem {
                        InfiniteAttendanceModeDistributionCard(
                            wfoCount = uiState.summary?.modeDistribution?.wfo?.count ?: (uiState.summary?.totalWfo ?: 0),
                            wfaCount = uiState.summary?.modeDistribution?.wfa?.count ?: (uiState.summary?.totalWfa ?: 0),
                            wfhCount = uiState.summary?.modeDistribution?.wfh?.count,
                            subtitle = null,
                            unavailableModeMessage = null
                        )
                    }

                    historySectionItem {
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
                        historySectionItem {
                            InfiniteAttendanceReportNoticeCard(
                                title = "PDF unavailable",
                                message = (uiState.exportState as ReportExportUiState.Error).message
                            )
                        }
                    }

                    historySectionItem(
                        key = "history-timeline-header",
                        contentType = "history-header",
                        bottomPadding = InfiniteSpacing.Default.md / 2
                    ) {
                        InfiniteSectionHeader(
                            title = "Attendance Timeline",
                            subtitle = "Scroll untuk memusatkan detail kehadiran",
                            leadingIcon = Icons.Outlined.CalendarMonth
                        )
                    }
                    if (uiState.records.isEmpty()) {
                        historySectionItem(
                            key = "history-timeline-empty",
                            contentType = "history-empty",
                            topPadding = InfiniteSpacing.Default.md / 2
                        ) {
                            InfiniteGlassReportCard {
                                InfiniteEmptyState(
                                    title = "No attendance records",
                                    message = "No attendance records found for this period."
                                )
                            }
                        }
                    } else {
                        attendanceHistoryTimelineItems(
                            records = uiState.records,
                            focusByKey = historyViewport.focusByKey,
                            timelineProgress = historyViewport.timelineProgress,
                            motionEnabled = motionEnabled
                        )
                    }

                    if (uiState.error != null && uiState.records.isNotEmpty()) {
                        historySectionItem(topPadding = InfiniteSpacing.Default.md / 2) {
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

private const val PullToRefreshThresholdPx = 160f

private fun LazyListScope.historySectionItem(
    key: Any? = null,
    contentType: Any? = null,
    topPadding: Dp = InfiniteSpacing.Default.md,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    item(key = key, contentType = contentType) {
        Box(
            modifier = Modifier.padding(
                top = topPadding,
                bottom = bottomPadding
            )
        ) {
            content()
        }
    }
}

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
