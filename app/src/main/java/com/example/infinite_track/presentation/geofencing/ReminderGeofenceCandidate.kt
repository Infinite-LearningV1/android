package com.example.infinite_track.presentation.geofencing

data class ReminderGeofenceCandidate(
    val id: String,
    val modeKey: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val source: String
)
