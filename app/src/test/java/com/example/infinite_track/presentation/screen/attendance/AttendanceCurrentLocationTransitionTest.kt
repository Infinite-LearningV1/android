package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.use_case.attendance.EvaluateAttendancePreparationUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateTargetRangeUseCase
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AttendanceCurrentLocationTransitionTest {

    private val rangeUseCase = EvaluateTargetRangeUseCase()
    private val preparationUseCase = EvaluateAttendancePreparationUseCase()

    @Test
    fun `GPS failure after ready invalidates stale range eligibility with refresh recovery`() {
        val initial = AttendancePreparationState(
            selectedMode = WorkMode.WFO,
            targetResolution = TargetLocationResolution.Resolved(target)
        )
        val ready = transition(
            preparation = initial,
            current = CurrentLocationResult.Success(
                CurrentLocation(
                    coordinate = target.coordinate,
                    accuracy = null,
                    capturedAtEpochMillis = NOW,
                    provider = "test",
                    isMock = false
                )
            )
        )

        val unavailable = transition(
            preparation = ready,
            current = CurrentLocationResult.Failure.Unavailable
        )

        assertSame(CurrentLocationResult.Failure.Unavailable, unavailable.currentLocation)
        assertEquals(
            TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE),
            unavailable.rangeStatus
        )
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                reason = AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE,
                recovery = AttendancePreparationRecovery.REFRESH_LOCATION
            ),
            unavailable.eligibility
        )
    }

    @Test
    fun `successful refresh restores ready eligibility after GPS failure`() {
        val unavailable = transition(
            preparation = AttendancePreparationState(
                selectedMode = WorkMode.WFO,
                targetResolution = TargetLocationResolution.Resolved(target)
            ),
            current = CurrentLocationResult.Failure.ProviderError
        )

        val recovered = transition(
            preparation = unavailable,
            current = CurrentLocationResult.Success(
                CurrentLocation(
                    coordinate = target.coordinate,
                    accuracy = null,
                    capturedAtEpochMillis = NOW,
                    provider = "test",
                    isMock = false
                )
            )
        )

        assertEquals(
            AttendancePreparationEligibility.Ready(
                target = target,
                range = TargetRangeStatus.Inside(DistanceMeters(0.0))
            ),
            recovered.eligibility
        )
    }

    private fun transition(
        preparation: AttendancePreparationState,
        current: CurrentLocationResult
    ): AttendancePreparationState = AttendanceCurrentLocationTransition.apply(
        preparation = preparation,
        current = current,
        nowEpochMillis = NOW,
        evaluateRange = rangeUseCase,
        evaluateEligibility = preparationUseCase
    )

    private val target = AuthoritativeTargetLocation(
        targetId = TargetLocationId("status:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(-0.90, 119.88),
        radius = DistanceMeters(100.0),
        displayName = "Infinite Track Office"
    )

    private companion object {
        const val NOW = 1_720_000_000_000L
    }
}
