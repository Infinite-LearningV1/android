package com.example.infinite_track.presentation.screen.wfa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.loading.InlineRefreshingIndicator
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestCard
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestFilterChips
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestStatusSummary
import com.example.infinite_track.presentation.theme.Blue_100
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.White
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WfaRequestsScreen(
    modifier: Modifier = Modifier,
    viewModel: WfaRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val refreshDragDistance = remember { mutableStateOf(0f) }
    val pullToRefreshConnection = remember(listState, uiState.isLoading, uiState.isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.isAtTop() && !uiState.isLoading && !uiState.isRefreshing) {
                    refreshDragDistance.value += available.y
                    if (refreshDragDistance.value >= PullToRefreshThresholdPx) {
                        refreshDragDistance.value = 0f
                        viewModel.refresh()
                    }
                }
                if (available.y < 0) {
                    refreshDragDistance.value = 0f
                }
                return Offset.Zero
            }
        }
    }

    WfaRequestsPaginationEffect(
        uiState = uiState,
        listState = listState,
        onLoadMore = viewModel::loadMore
    )

    // MainScreen already applies scaffold/bottom-bar padding.
    // Keep page insets aligned with Home/History.
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshConnection),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WfaRequestsHeader()
        }

        if (uiState.isRefreshing) {
            item {
                InlineRefreshingIndicator(message = "Refreshing WFA requests...")
            }
        }

        item {
            WfaRequestStatusSummary(summary = uiState.summary)
        }

        item {
            WfaRequestFilterChips(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )
        }

        item {
            Text(
                text = "Recent WFA Requests",
                style = headline4,
                color = Purple_500
            )
        }

        when {
            uiState.isLoading -> {
                item { LoadingState() }
            }

            uiState.errorMessage != null && uiState.bookings.isEmpty() -> {
                item {
                    ErrorState(
                        message = uiState.errorMessage ?: "Unable to load WFA requests.",
                        onRetry = viewModel::retry
                    )
                }
            }

            uiState.isEmpty -> {
                item { EmptyState() }
            }

            else -> {
                items(
                    items = uiState.bookings,
                    key = { booking -> "${booking.bookingId}-${booking.scheduleDateRaw}-${booking.statusKey}" }
                ) { booking ->
                    WfaRequestCard(booking = booking)
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Blue_500)
                        }
                    }
                }

                if (uiState.errorMessage != null && uiState.bookings.isNotEmpty()) {
                    item {
                        ErrorState(
                            message = uiState.errorMessage ?: "Unable to load more WFA requests.",
                            onRetry = viewModel::loadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WfaRequestsHeader() {
    Text(
        text = "WFA Requests",
        style = headline4,
        color = Purple_500
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Blue_500)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Blue_500.copy(alpha = 0.18f),
                spotColor = Blue_500.copy(alpha = 0.12f)
            )
            .background(White.copy(alpha = 0.34f), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Blue_100.copy(alpha = 0.9f)), RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No WFA requests yet",
            style = headline4,
            color = Purple_500
        )
        Text(
            text = "Your recent WFA submissions will appear here.",
            style = body2,
            color = Purple_300
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Blue_500.copy(alpha = 0.18f),
                spotColor = Blue_500.copy(alpha = 0.12f)
            )
            .background(White.copy(alpha = 0.34f), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Blue_100.copy(alpha = 0.9f)), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Unable to load requests",
            style = headline4,
            color = Purple_500
        )
        Text(
            text = message,
            style = body2,
            color = Purple_300
        )
        Button(onClick = onRetry) {
            Text(text = "Retry", style = body1)
        }
    }
}

@Composable
private fun WfaRequestsPaginationEffect(
    uiState: WfaRequestsUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit
) {
    val currentState = rememberUpdatedState(uiState)
    val loadMore = rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= (total - 2).coerceAtLeast(0)
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                val state = currentState.value
                if (
                    shouldLoadMore &&
                    !state.isLoading &&
                    !state.isRefreshing &&
                    !state.isLoadingMore &&
                    state.pagination.hasNextPage
                ) {
                    loadMore.value()
                }
            }
    }
}

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

private const val PullToRefreshThresholdPx = 120f
