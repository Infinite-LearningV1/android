package com.example.infinite_track.domain.model.booking

data class BookingHistoryPage(
    val bookings: List<BookingHistoryItem>,
    val summary: BookingHistorySummary = BookingHistorySummary(),
    val pagination: BookingHistoryPagination = BookingHistoryPagination(),
    val hasNextPage: Boolean = pagination.hasNextPage
)

data class BookingHistorySummary(
    val total: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0
)

data class BookingHistoryPagination(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val itemsPerPage: Int = 10,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false
)
