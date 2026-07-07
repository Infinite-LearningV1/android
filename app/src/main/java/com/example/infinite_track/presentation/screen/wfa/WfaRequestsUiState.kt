package com.example.infinite_track.presentation.screen.wfa

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPagination
import com.example.infinite_track.domain.model.booking.BookingHistorySummary

enum class WfaRequestStatusFilter(val key: String, val label: String) {
    All("all", "All"),
    Pending("pending", "Pending"),
    Approved("approved", "Approved"),
    Rejected("rejected", "Rejected")
}

data class WfaRequestsUiState(
    val selectedFilter: WfaRequestStatusFilter = WfaRequestStatusFilter.All,
    val summary: BookingHistorySummary = BookingHistorySummary(),
    val bookings: List<BookingHistoryItem> = emptyList(),
    val pagination: BookingHistoryPagination = BookingHistoryPagination(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && bookings.isEmpty()
}
