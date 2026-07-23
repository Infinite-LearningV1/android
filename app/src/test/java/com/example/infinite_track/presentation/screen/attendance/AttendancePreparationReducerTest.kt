package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocation
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationReducer
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendancePreparationReducerTest {

    @Test
    fun `recommendation selection changes preview only`() {
        val searchPreview = LocationResult(
            placeName = "Lokasi pencarian lama",
            address = "Jalan Lama",
            latitude = -0.91,
            longitude = 119.89
        )
        val state = stateWithApprovedTarget().withDiscovery(
            selectedKey = null,
            searchPreview = searchPreview
        )

        val next = AttendancePreparationReducer.selectRecommendation(state, recommendation)

        assertEquals(approvedTarget, (next.targetResolution as TargetLocationResolution.Resolved).target)
        val discovery = next.wfaDiscovery as WfaDiscoveryState.Content
        assertEquals(recommendation.stableKey, discovery.selectedKey)
        assertEquals(null, discovery.searchPreview)
        assertPreparationEvidenceUnchanged(state, next)
    }

    @Test
    fun `search selection changes preview only`() {
        val state = stateWithApprovedTarget().withDiscovery(
            selectedKey = recommendation.stableKey,
            searchPreview = null
        )
        val preview = LocationResult(
            placeName = "Lokasi pencarian",
            address = "Jalan Uji",
            latitude = -0.91,
            longitude = 119.89
        )

        val next = AttendancePreparationReducer.selectSearchPreview(state, preview)

        val discovery = next.wfaDiscovery as WfaDiscoveryState.Content
        assertEquals(preview, discovery.searchPreview)
        assertEquals(null, discovery.selectedKey)
        assertPreparationEvidenceUnchanged(state, next)
    }

    @Test
    fun `resolved target projects bottom sheet location from preparation`() {
        val projected = stateWithApprovedTarget().toBottomSheetTargetLocationInfo()

        assertEquals(WorkMode.WFA, projected?.mode)
        assertEquals("WFA disetujui", projected?.displayName)
        assertEquals(approvedTarget.coordinate, projected?.location?.coordinate)
        assertEquals(approvedTarget.approvedWfaContext?.bookingId, projected?.location?.locationId)
    }

    private fun AttendancePreparationState.withDiscovery(
        selectedKey: String?,
        searchPreview: LocationResult?
    ): AttendancePreparationState = copy(
        wfaDiscovery = (wfaDiscovery as WfaDiscoveryState.Content).copy(
            selectedKey = selectedKey,
            searchPreview = searchPreview
        )
    )

    private fun assertPreparationEvidenceUnchanged(
        before: AttendancePreparationState,
        after: AttendancePreparationState
    ) {
        assertEquals(before.selectedMode, after.selectedMode)
        assertEquals(before.targetResolution, after.targetResolution)
        assertEquals(before.currentLocation, after.currentLocation)
        assertEquals(before.rangeStatus, after.rangeStatus)
        assertEquals(before.eligibility, after.eligibility)
    }

    private fun stateWithApprovedTarget(): AttendancePreparationState {
        val range = TargetRangeStatus.Inside(DistanceMeters(20.0))
        return AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolved(approvedTarget),
            currentLocation = CurrentLocationResult.Success(
                CurrentLocation(
                    coordinate = GeoCoordinate(-0.90, 119.88),
                    accuracy = DistanceMeters(5.0),
                    capturedAtEpochMillis = 1_000L,
                    provider = "test",
                    isMock = true
                )
            ),
            rangeStatus = range,
            wfaDiscovery = WfaDiscoveryState.Content(listOf(recommendation)),
            eligibility = AttendancePreparationEligibility.Ready(approvedTarget, range)
        )
    }

    private val approvedTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("booking:88"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.90, 119.88),
        radius = DistanceMeters(100.0),
        displayName = "WFA disetujui",
        approvedWfaContext = ApprovedWfaTargetContext(
            bookingId = 88,
            scheduleDate = "2026-07-23"
        )
    )

    private val recommendation = WfaRecommendation(
        stableKey = "cafe@-0.900000,119.880000",
        name = "Cafe Palu",
        address = "Palu",
        coordinate = GeoCoordinate(-0.90, 119.88),
        category = "Cafe",
        suitabilityScore = 0.91,
        suitabilityLabel = "Sangat sesuai",
        distanceMeters = DistanceMeters(1_250.0)
    )
}
