package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaMapPickInteractionState

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
        mapPickSessionForCameraIdle(preparation) != null

    fun beginMapPick(
        preparation: AttendancePreparationState,
        sessionId: Long
    ): AttendancePreparationState {
        if (preparation.selectedMode != WorkMode.WFA) return preparation
        return preparation.copy(
            mapPickInteraction = WfaMapPickInteractionState.Active(sessionId)
        )
    }

    fun mapPickSessionForCameraIdle(
        preparation: AttendancePreparationState
    ): WfaMapPickInteractionState.Active? {
        if (preparation.selectedMode != WorkMode.WFA) return null
        return preparation.mapPickInteraction as? WfaMapPickInteractionState.Active
    }

    fun consumeMapPick(
        preparation: AttendancePreparationState,
        session: WfaMapPickInteractionState.Active
    ): AttendancePreparationState? {
        val active = mapPickSessionForCameraIdle(preparation) ?: return null
        if (active != session) return null
        return preparation.copy(mapPickInteraction = WfaMapPickInteractionState.Inactive)
    }

    fun cancelMapPick(preparation: AttendancePreparationState): AttendancePreparationState =
        preparation.copy(mapPickInteraction = WfaMapPickInteractionState.Inactive)

    fun beginSelection(
        state: AttendanceScreenState,
        preparation: AttendancePreparationState
    ): AttendanceScreenState = state.copy(
        preparation = cancelMapPick(preparation),
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
