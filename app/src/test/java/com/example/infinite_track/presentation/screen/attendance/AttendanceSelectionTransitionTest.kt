package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRecoveryAction
import com.example.infinite_track.domain.model.attendance.TargetUnavailableReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceSelectionTransitionTest {

    @Test
    fun `resolved target is interactive only when its id is selected`() {
        val preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFO,
            targetResolution = TargetLocationResolution.Resolved(officeTarget)
        )

        assertEquals(
            officeTarget,
            AttendanceSelectionTransition.resolvedTargetForInteraction(
                preparation,
                officeTarget.targetId
            )
        )
        assertNull(
            AttendanceSelectionTransition.resolvedTargetForInteraction(
                preparation,
                TargetLocationId("office:other")
            )
        )
    }

    @Test
    fun `new selection clears queued WFA request navigation`() {
        val nextPreparation = AttendancePreparationState(selectedMode = WorkMode.WFH)
        val next = AttendanceSelectionTransition.beginSelection(
            state = AttendanceScreenState(
                navigationTarget = NavigationTarget.WfaRequest(WFA_ROUTE)
            ),
            preparation = nextPreparation
        )

        assertEquals(nextPreparation, next.preparation)
        assertNull(next.navigationTarget)
    }

    @Test
    fun `new selection preserves unrelated navigation`() {
        val navigation = NavigationTarget.FaceScanner(AttendanceActionIntent.CHECK_IN)
        val next = AttendanceSelectionTransition.beginSelection(
            state = AttendanceScreenState(navigationTarget = navigation),
            preparation = AttendancePreparationState(selectedMode = WorkMode.WFH)
        )

        assertEquals(navigation, next.navigationTarget)
    }

    @Test
    fun `current WFA not requested selection auto navigates to request form`() {
        assertEquals(
            NavigationTarget.WfaRequest(WFA_ROUTE),
            AttendanceSelectionTransition.wfaRequestNavigationTarget(
                preparation = unavailableWfa(TargetUnavailableReason.WFA_NOT_REQUESTED),
                selectionIsCurrent = true,
                hasPendingNavigation = false,
                route = WFA_ROUTE
            )
        )
    }

    @Test
    fun `approved pending rejected and missing-date states never open a new request`() {
        val states = listOf(
            AttendancePreparationState(
                selectedMode = WorkMode.WFA,
                targetResolution = TargetLocationResolution.Resolved(approvedWfaTarget)
            ),
            unavailableWfa(TargetUnavailableReason.WFA_PENDING),
            unavailableWfa(TargetUnavailableReason.WFA_REJECTED),
            unavailableWfa(TargetUnavailableReason.WFA_APPROVAL_MISSING_FOR_DATE)
        )

        states.forEach { preparation ->
            assertNull(
                AttendanceSelectionTransition.wfaRequestNavigationTarget(
                    preparation = preparation,
                    selectionIsCurrent = true,
                    hasPendingNavigation = false,
                    route = WFA_ROUTE
                )
            )
        }
    }

    @Test
    fun `stale non-WFA and already queued selections cannot duplicate navigation`() {
        assertNull(
            AttendanceSelectionTransition.wfaRequestNavigationTarget(
                preparation = unavailableWfa(TargetUnavailableReason.WFA_NOT_REQUESTED),
                selectionIsCurrent = false,
                hasPendingNavigation = false,
                route = WFA_ROUTE
            )
        )
        assertNull(
            AttendanceSelectionTransition.wfaRequestNavigationTarget(
                preparation = AttendancePreparationState(selectedMode = WorkMode.WFO),
                selectionIsCurrent = true,
                hasPendingNavigation = false,
                route = WFA_ROUTE
            )
        )
        assertNull(
            AttendanceSelectionTransition.wfaRequestNavigationTarget(
                preparation = unavailableWfa(TargetUnavailableReason.WFA_NOT_REQUESTED),
                selectionIsCurrent = true,
                hasPendingNavigation = true,
                route = WFA_ROUTE
            )
        )
    }

    private fun unavailableWfa(reason: TargetUnavailableReason) = AttendancePreparationState(
        selectedMode = WorkMode.WFA,
        targetResolution = TargetLocationResolution.Unavailable(
            mode = WorkMode.WFA,
            reason = reason,
            recovery = when (reason) {
                TargetUnavailableReason.WFA_NOT_REQUESTED -> TargetRecoveryAction.OPEN_WFA_BOOKING
                else -> TargetRecoveryAction.OPEN_WFA_REQUESTS
            }
        )
    )

    private val officeTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("office:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(-0.89, 119.87),
        radius = DistanceMeters(100.0),
        displayName = "Kantor Palu"
    )

    private val approvedWfaTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("wfa:booking:42"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.88, 119.86),
        radius = DistanceMeters(100.0),
        displayName = "WFA disetujui"
    )

    private companion object {
        const val WFA_ROUTE = "wfa_request"
    }
}
