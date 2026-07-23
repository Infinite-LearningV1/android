package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState

internal object AttendanceSelectionTransition {
    fun resolvedTargetForInteraction(
        preparation: AttendancePreparationState,
        selectedTargetId: TargetLocationId?
    ): AuthoritativeTargetLocation? {
        val target = (preparation.targetResolution as? TargetLocationResolution.Resolved)
            ?.target
        return target?.takeIf { it.targetId == selectedTargetId }
    }

    fun isMapPickEnabled(preparation: AttendancePreparationState): Boolean =
        preparation.selectedMode == WorkMode.WFA

    fun beginSelection(
        state: AttendanceScreenState,
        preparation: AttendancePreparationState
    ): AttendanceScreenState = state.copy(
        preparation = preparation,
        navigationTarget = state.navigationTarget
            .takeUnless { it is NavigationTarget.WfaBooking }
    )

    fun wfaBookingNavigationTarget(
        preparation: AttendancePreparationState,
        selectionIsCurrent: Boolean,
        route: String
    ): NavigationTarget.WfaBooking? {
        if (!selectionIsCurrent || preparation.selectedMode != WorkMode.WFA) return null
        return NavigationTarget.WfaBooking(route)
    }
}
