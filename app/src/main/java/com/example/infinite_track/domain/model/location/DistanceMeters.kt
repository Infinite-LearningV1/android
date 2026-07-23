package com.example.infinite_track.domain.model.location

/** Provider-neutral non-negative distance represented in meters. */
data class DistanceMeters(val value: Double) {
    init {
        require(value.isFinite()) { "Distance must be finite" }
        require(value >= 0.0) { "Distance must not be negative" }
    }

    companion object {
        val Zero = DistanceMeters(0.0)
    }
}
