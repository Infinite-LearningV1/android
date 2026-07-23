package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSelectionTransitionTest {

    @Test
    fun `mode change cannot retain popup data from the previous target`() {
        val oldTargetId = officeTarget.targetId
        val wfaPreparation = readyPreparation(approvedWfaTarget)

        val popupTarget = AttendanceSelectionTransition.resolvedTargetForInteraction(
            preparation = wfaPreparation,
            selectedTargetId = oldTargetId
        )

        assertNull(popupTarget)
        assertEquals(
            approvedWfaTarget,
            AttendanceSelectionTransition.resolvedTargetForInteraction(
                preparation = wfaPreparation,
                selectedTargetId = approvedWfaTarget.targetId
            )
        )
    }

    @Test
    fun `initial WFA preparation enables map pick and WFO disables it`() {
        val initialWfaPreparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
            wfaDiscovery = WfaDiscoveryState.Loading,
            eligibility = AttendancePreparationEligibility.Resolving
        )
        assertTrue(
            AttendanceSelectionTransition.isMapPickEnabled(
                initialWfaPreparation
            )
        )
        assertFalse(
            AttendanceSelectionTransition.isMapPickEnabled(
                readyPreparation(officeTarget)
            )
        )
    }

    @Test
    fun `WFA booking navigation is cleared when WFO selection starts before consumption`() {
        val queuedState = AttendanceScreenState(
            preparation = readyPreparation(approvedWfaTarget),
            navigationTarget = NavigationTarget.WfaBooking("wfa_booking?latitude=-0.9&longitude=119.88")
        )
        val wfoPreparing = AttendancePreparationState(
            selectedMode = WorkMode.WFO,
            targetResolution = TargetLocationResolution.Resolving(WorkMode.WFO),
            wfaDiscovery = WfaDiscoveryState.Hidden,
            eligibility = AttendancePreparationEligibility.Resolving
        )

        val next = AttendanceSelectionTransition.beginSelection(
            state = queuedState,
            preparation = wfoPreparing
        )

        assertEquals(WorkMode.WFO, next.preparation.selectedMode)
        assertNull(next.navigationTarget)
    }

    private fun readyPreparation(
        target: AuthoritativeTargetLocation
    ): AttendancePreparationState {
        val range = TargetRangeStatus.Inside(DistanceMeters(20.0))
        return AttendancePreparationState(
            selectedMode = target.mode,
            targetResolution = TargetLocationResolution.Resolved(target),
            rangeStatus = range,
            wfaDiscovery = if (target.mode == WorkMode.WFA) {
                WfaDiscoveryState.Empty
            } else {
                WfaDiscoveryState.Hidden
            },
            eligibility = AttendancePreparationEligibility.Ready(target, range)
        )
    }

    private val officeTarget = target(
        id = "office:1",
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        name = "Kantor lama"
    )

    private val approvedWfaTarget = target(
        id = "booking:88",
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        name = "WFA disetujui"
    )

    private fun target(
        id: String,
        mode: WorkMode,
        source: TargetLocationSource,
        name: String
    ) = AuthoritativeTargetLocation(
        targetId = TargetLocationId(id),
        mode = mode,
        source = source,
        coordinate = GeoCoordinate(-0.90, 119.88),
        radius = DistanceMeters(100.0),
        displayName = name
    )
}
