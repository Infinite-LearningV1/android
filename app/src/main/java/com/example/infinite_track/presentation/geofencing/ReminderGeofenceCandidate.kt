package com.example.infinite_track.presentation.geofencing

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate

data class ReminderGeofenceCandidate(
    val id: String,
    val modeKey: String,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val source: String
)
