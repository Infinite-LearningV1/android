package com.example.infinite_track.domain.model.booking

data class BookingHistoryItem(
    val id: String,
    val locationDescription: String,
    val scheduleDate: String,
    val status: String,
    val notes: String,
    val suitabilityLabel: String,
    val bookingId: Int? = null,
    val scheduleDateRaw: String? = null,
    val statusRaw: String? = null
)
