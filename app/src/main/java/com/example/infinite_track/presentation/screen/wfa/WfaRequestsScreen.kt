package com.example.infinite_track.presentation.screen.wfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.loading.InlineRefreshingIndicator
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestCard
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestFilterChips
import com.example.infinite_track.presentation.screen.wfa.components.WfaRequestStatusSummary
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshConnection)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    style = MaterialTheme.typography.titleLarge,
                    color = Purple_500,
                    fontWeight = FontWeight.Bold
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
}

@Composable
private fun WfaRequestsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WFA Requests",
            style = headline3
        )
    }
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
            .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No WFA requests yet",
            style = MaterialTheme.typography.titleMedium,
            color = Purple_500,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a WFA request from Attendance when you need to work from another location.",
            style = MaterialTheme.typography.bodyMedium,
            color = Purple_300,
            textAlign = TextAlign.Center
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
            .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}


private const val PullToRefreshThresholdPx = 160f

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

@Composable
private fun WfaRequestsPaginationEffect(
    uiState: WfaRequestsUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit
) {
    val latestUiState by rememberUpdatedState(uiState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleItemIndex ->
                val currentState = latestUiState
                val shouldLoadMore =
                    lastVisibleItemIndex != null &&
                        currentState.errorMessage == null &&
                        currentState.bookings.isNotEmpty() &&
                        lastVisibleItemIndex >= currentState.bookings.size + 2 &&
                        currentState.pagination.hasNextPage &&
                        !currentState.isLoading &&
                        !currentState.isRefreshing &&
                        !currentState.isLoadingMore

                if (shouldLoadMore) onLoadMore()
            }
    }
}
