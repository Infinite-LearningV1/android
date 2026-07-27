package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution

/**
 * Builds the typed submit command from already-resolved screen state.
 * Pure state projection — no business rule lives here; SubmitAttendanceUseCase
 * revalidates everything it needs before mutating.
 */
object AttendanceSubmitCommandBuilder {

    fun build(
        intent: AttendanceActionIntent,
        state: AttendanceScreenState
    ): AttendanceSubmitCommand? = when (intent) {
        AttendanceActionIntent.CHECK_IN -> {
            val preparation = state.preparation
            val resolvedTarget =
                (preparation.targetResolution as? TargetLocationResolution.Resolved)?.target
            if (resolvedTarget == null ||
                preparation.eligibility !is AttendancePreparationEligibility.Ready
            ) {
                null
            } else {
                AttendanceSubmitCommand.CheckIn(
                    workMode = preparation.selectedMode,
                    authoritativeTarget = resolvedTarget
                )
            }
        }

        AttendanceActionIntent.CHECK_OUT ->
            AttendanceSubmitCommand.CheckOut(
                activeAttendanceId = state.todayStatus?.activeAttendanceId
            )
    }
}
