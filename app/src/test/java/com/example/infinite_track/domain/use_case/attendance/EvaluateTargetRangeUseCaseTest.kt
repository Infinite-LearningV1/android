package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateTargetRangeUseCaseTest {

    private val rangeUseCase = EvaluateTargetRangeUseCase()

    @Test
    fun `inside range returns provider-neutral distance evidence`() {
        val status = rangeUseCase(
            target = target(radiusMeters = 100.0),
            current = currentLocation(latitude = -0.90, longitude = 119.88, capturedAt = NOW),
            nowEpochMillis = NOW
        ) as TargetRangeStatus.Inside

        assertEquals(0.0, status.distance.value, 0.0)
    }

    @Test
    fun `outside range retains authoritative target`() {
        val status = rangeUseCase(
            target = target(radiusMeters = 100.0),
            current = currentLocation(latitude = -0.91, longitude = 119.89, capturedAt = NOW),
            nowEpochMillis = NOW
        ) as TargetRangeStatus.Outside

        assertTrue(status.distance.value > 100.0)
    }

    @Test
    fun `unavailable current location is unknown instead of changing target`() {
        val status = rangeUseCase(
            target = target(),
            current = CurrentLocationResult.Failure.Unavailable,
            nowEpochMillis = NOW
        )

        assertEquals(
            TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE),
            status
        )
    }

    @Test
    fun `location older than sixty seconds is stale`() {
        val status = rangeUseCase(
            target = target(),
            current = currentLocation(capturedAt = NOW - 60_001L),
            nowEpochMillis = NOW
        )

        assertEquals(
            TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_STALE),
            status
        )
    }

    @Test
    fun `location captured exactly sixty seconds ago remains eligible for range evaluation`() {
        val status = rangeUseCase(
            target = target(),
            current = currentLocation(capturedAt = NOW - 60_000L),
            nowEpochMillis = NOW
        )

        assertTrue(status is TargetRangeStatus.Inside)
    }

    private fun target(
        latitude: Double = -0.90,
        longitude: Double = 119.88,
        radiusMeters: Double = 100.0
    ) = AuthoritativeTargetLocation(
        targetId = TargetLocationId("status:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(latitude, longitude),
        radius = DistanceMeters(radiusMeters),
        displayName = "Kantor Infinite Track"
    )

    private fun currentLocation(
        latitude: Double = -0.90,
        longitude: Double = 119.88,
        capturedAt: Long
    ) = CurrentLocationResult.Success(
        CurrentLocation(
            coordinate = GeoCoordinate(latitude, longitude),
            accuracy = null,
            capturedAtEpochMillis = capturedAt,
            provider = "test",
            isMock = false
        )
    )

    private companion object {
        const val NOW = 1_720_000_000_000L
    }
}
