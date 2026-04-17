package com.example.infinite_track.domain.model.booking

data class BookingHistoryPage(
	val bookings: List<BookingHistoryItem>,
	val hasNextPage: Boolean
) 