package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetUnavailableReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState

internal object AttendanceSelectionTransition {
    fun resolvedTargetForInteraction(
        preparation: AttendancePreparationState,
        selectedTargetId: TargetLocationId?
    ): AuthoritativeTargetLocation? {
        val target = (preparation.targetResolution as? TargetLocationResolution.Resolved)?.target
        return target?.takeIf { it.targetId == selectedTargetId }
    }

    fun beginSelection(
        state: AttendanceScreenState,
        preparation: AttendancePreparationState
    ): AttendanceScreenState = state.copy(
        preparation = preparation,
        navigationTarget = state.navigationTarget
            .takeUnless { it is NavigationTarget.WfaRequest }
    )

    fun wfaRequestNavigationTarget(
        preparation: AttendancePreparationState,
        selectionIsCurrent: Boolean,
        hasPendingNavigation: Boolean,
        route: String
    ): NavigationTarget.WfaRequest? {
        if (!selectionIsCurrent || hasPendingNavigation) return null
        if (preparation.selectedMode != WorkMode.WFA) return null
        val unavailable = preparation.targetResolution as? TargetLocationResolution.Unavailable
            ?: return null
        if (unavailable.reason != TargetUnavailableReason.WFA_NOT_REQUESTED) return null
        return NavigationTarget.WfaRequest(route)
    }
}
