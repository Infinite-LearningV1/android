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
    val statusRaw: String = "",
    val statusKey: String = statusRaw,
    val statusLabel: String = status,
    val suitabilityScore: Double? = null,
    val createdAtRaw: String = "",
    val createdAt: String = "-",
    val processedAtRaw: String? = null,
    val processedAt: String = "Not processed yet",
    val locationId: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null
)
