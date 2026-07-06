package com.example.infinite_track.presentation.screen.wfa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.screen.home.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WfaHistoryScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.bookingHistoryDetailsState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    InitializeBookingHistoryEffect(
        uiState = uiState,
        onLoadAllBookings = { viewModel.onBookingStatusFilterChanged("all") }
    )
    BookingHistoryPaginationEffect(
        uiState = uiState,
        listState = listState,
        onLoadMoreBookings = viewModel::loadMoreBookings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            Text(
                text = "Riwayat Booking WFA",
                style = headline2,
                modifier = Modifier.padding(start = 16.dp, top = 32.dp)
            )
        }
    ) { innerPadding ->
        WfaBookingHistoryContent(
            uiState = uiState,
            listState = listState,
            onStatusSelected = viewModel::onBookingStatusFilterChanged,
            onRetryClick = { viewModel.onBookingStatusFilterChanged(uiState.retryStatusFilter()) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun InitializeBookingHistoryEffect(
    uiState: HomeViewModel.BookingHistoryDetailsState,
    onLoadAllBookings: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (uiState.hasNotStartedDetailedBookingLoad()) {
            onLoadAllBookings()
        }
    }
}

@Composable
fun BookingHistoryPaginationEffect(
    uiState: HomeViewModel.BookingHistoryDetailsState,
    listState: LazyListState,
    onLoadMoreBookings: () -> Unit
) {
    val latestUiState by rememberUpdatedState(uiState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleItemIndex ->
                val currentState = latestUiState
                val shouldLoadMore =
                    lastVisibleItemIndex != null &&
                        currentState.error == null &&
                        currentState.bookings.isNotEmpty() &&
                        lastVisibleItemIndex >= currentState.bookings.size - 3 &&
                        currentState.canLoadMore &&
                        !currentState.isLoading

                if (shouldLoadMore) {
                    onLoadMoreBookings()
                }
            }
    }
}

fun HomeViewModel.BookingHistoryDetailsState.retryStatusFilter(): String = selectedStatus ?: "all"

private fun HomeViewModel.BookingHistoryDetailsState.hasNotStartedDetailedBookingLoad(): Boolean =
    isLoading &&
        bookings.isEmpty() &&
        error == null &&
        selectedStatus == null &&
        currentPage == 1 &&
        canLoadMore
