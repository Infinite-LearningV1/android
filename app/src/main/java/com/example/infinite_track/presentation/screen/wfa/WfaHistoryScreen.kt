package com.example.infinite_track.presentation.screen.wfa

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.example.infinite_track.presentation.screen.home.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WfaHistoryScreen() {
    WfaRequestsScreen()
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

private fun HomeViewModel.BookingHistoryDetailsState.hasNotStartedDetailedBookingLoad(): Boolean =
    isLoading &&
        bookings.isEmpty() &&
        error == null &&
        selectedStatus == null &&
        currentPage == 1 &&
        canLoadMore
