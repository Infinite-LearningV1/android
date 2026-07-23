package com.example.infinite_track.data.location.current

interface CurrentLocationDataSource {
    suspend fun getCurrentLocation(): PlatformLocationSnapshot?
}

data class PlatformLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val capturedAtEpochMillis: Long,
    val provider: String?,
    val isMock: Boolean
)
