package com.example.infinite_track.presentation.screen.home.details

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.components.button.InfiniteTracButtonBack
import com.example.infinite_track.presentation.screen.home.HomeViewModel
import com.example.infinite_track.presentation.screen.wfa.BookingHistoryPaginationEffect
import com.example.infinite_track.presentation.screen.wfa.InitializeBookingHistoryEffect
import com.example.infinite_track.presentation.screen.wfa.WfaBookingHistoryContent
import com.example.infinite_track.presentation.screen.wfa.retryStatusFilter

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsMyBooking(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
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
            InfiniteTracButtonBack(
                title = "Riwayat Booking",
                navigationBack = onBackClick,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    ) { paddingValues ->
        WfaBookingHistoryContent(
            uiState = uiState,
            listState = listState,
            onStatusSelected = viewModel::onBookingStatusFilterChanged,
            onRetryClick = { viewModel.onBookingStatusFilterChanged(uiState.retryStatusFilter()) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        )
    }
}
