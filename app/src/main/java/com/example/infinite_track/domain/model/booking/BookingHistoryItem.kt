package com.example.infinite_track.domain.model.booking

data class BookingHistoryItem(
    val id: String,
    val locationDescription: String,
    val scheduleDate: String,
    val status: String,
    val notes: String,
    val suitabilityLabel: String,
    val bookingId: Int = 0,
    val scheduleDateRaw: String = "",
    val statusRaw: String = ""
)
