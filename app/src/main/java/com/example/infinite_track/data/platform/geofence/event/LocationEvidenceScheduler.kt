package com.example.infinite_track.data.platform.geofence.event

import java.time.Instant

interface LocationEvidenceScheduler {
    fun enqueue(
        attendanceId: Int,
        logicalId: String,
        transition: GeofenceTransition,
        occurredAt: Instant
    )
}
