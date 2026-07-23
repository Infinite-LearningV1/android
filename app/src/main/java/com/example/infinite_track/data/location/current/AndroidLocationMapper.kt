package com.example.infinite_track.data.location.current

import android.location.Location
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import javax.inject.Inject

class AndroidLocationMapper @Inject constructor() {

    fun toSnapshot(location: Location): PlatformLocationSnapshot {
        return PlatformLocationSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy
                .takeIf { location.hasAccuracy() && it.isFinite() && it >= 0f }
                ?.toDouble(),
            capturedAtEpochMillis = location.time,
            provider = location.provider,
            isMock = location.isFromMockProvider
        )
    }

    fun toDomain(snapshot: PlatformLocationSnapshot): CurrentLocation {
        return CurrentLocation(
            coordinate = GeoCoordinate(snapshot.latitude, snapshot.longitude),
            accuracy = snapshot.accuracyMeters?.let(::DistanceMeters),
            capturedAtEpochMillis = snapshot.capturedAtEpochMillis,
            provider = snapshot.provider,
            isMock = snapshot.isMock
        )
    }
}
