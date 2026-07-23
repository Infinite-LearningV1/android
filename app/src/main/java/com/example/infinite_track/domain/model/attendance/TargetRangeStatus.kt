package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.location.DistanceMeters

sealed interface TargetRangeStatus {
    data class Inside(val distance: DistanceMeters) : TargetRangeStatus
    data class Outside(val distance: DistanceMeters) : TargetRangeStatus
    data class Unknown(val reason: TargetRangeUnknownReason) : TargetRangeStatus
}

enum class TargetRangeUnknownReason {
    CURRENT_LOCATION_UNAVAILABLE,
    CURRENT_LOCATION_STALE
}
