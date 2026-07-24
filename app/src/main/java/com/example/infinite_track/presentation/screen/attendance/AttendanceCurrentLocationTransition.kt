package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.use_case.attendance.EvaluateAttendancePreparationUseCase
import com.example.infinite_track.domain.use_case.attendance.EvaluateTargetRangeUseCase
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState

/**
 * Applies one current-location result to preparation evidence atomically.
 * A failure is evidence too: it must replace any previous in-range result instead of leaving a
 * stale Ready state active.
 */
internal object AttendanceCurrentLocationTransition {
    fun apply(
        preparation: AttendancePreparationState,
        current: CurrentLocationResult,
        nowEpochMillis: Long,
        evaluateRange: EvaluateTargetRangeUseCase,
        evaluateEligibility: EvaluateAttendancePreparationUseCase
    ): AttendancePreparationState {
        val resolution = preparation.targetResolution as? TargetLocationResolution.Resolved
            ?: return preparation.copy(currentLocation = current)
        val range = evaluateRange(
            target = resolution.target,
            current = current,
            nowEpochMillis = nowEpochMillis
        )
        return preparation.copy(
            currentLocation = current,
            rangeStatus = range,
            eligibility = evaluateEligibility(resolution, range)
        )
    }
}
