package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class EvaluateTargetRangeUseCase @Inject constructor() {

    operator fun invoke(
        target: AuthoritativeTargetLocation,
        current: CurrentLocationResult?,
        nowEpochMillis: Long
    ): TargetRangeStatus {
        val location = (current as? CurrentLocationResult.Success)?.location
            ?: return TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)

        if (nowEpochMillis - location.capturedAtEpochMillis > MAX_LOCATION_AGE_MILLIS) {
            return TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_STALE)
        }

        val distance = DistanceMeters(
            haversineDistanceMeters(
                firstLatitude = target.coordinate.latitude,
                firstLongitude = target.coordinate.longitude,
                secondLatitude = location.coordinate.latitude,
                secondLongitude = location.coordinate.longitude
            )
        )
        return if (distance.value <= target.radius.value) {
            TargetRangeStatus.Inside(distance)
        } else {
            TargetRangeStatus.Outside(distance)
        }
    }

    private fun haversineDistanceMeters(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Double {
        val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
        val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
        val firstLatitudeRadians = Math.toRadians(firstLatitude)
        val secondLatitudeRadians = Math.toRadians(secondLatitude)
        val sinLatitude = sin(latitudeDelta / 2.0)
        val sinLongitude = sin(longitudeDelta / 2.0)
        val haversine = sinLatitude * sinLatitude +
            cos(firstLatitudeRadians) * cos(secondLatitudeRadians) * sinLongitude * sinLongitude
        val centralAngle = 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))

        return EARTH_RADIUS_METERS * centralAngle
    }

    private companion object {
        const val MAX_LOCATION_AGE_MILLIS = 60_000L
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
