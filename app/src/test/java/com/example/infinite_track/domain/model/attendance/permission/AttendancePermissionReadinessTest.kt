package com.example.infinite_track.domain.model.attendance.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionReadinessTest {

    @Test
    fun allRequiredEntriesReady_allowsAttendanceAndCountsAllRequiredEntries() {
        val result = readiness()

        assertTrue(result.canEnterAttendance)
        assertEquals(3, result.requiredReadyCount)
        assertEquals(3, result.requiredTotalCount)
        assertEquals(AttendanceOptionalCapabilitySummary.READY, result.optionalCapabilitySummary)
    }

    @Test
    fun approximateOnlyPreciseLocation_blocksAttendanceWithPreciseLocationReason() {
        val result = readiness(
            precise = AttendanceAccessStatus.ACTION_REQUIRED
        ).copy(
            entries = listOf(
                AttendanceAccessReadiness(
                    AttendanceAccess.PRECISE_LOCATION,
                    AttendanceAccessStatus.ACTION_REQUIRED,
                    reason = AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY,
                    recovery = AttendanceAccessRecovery.REQUEST_PERMISSION
                ),
                AttendanceAccessReadiness(AttendanceAccess.CAMERA, AttendanceAccessStatus.READY),
                AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, AttendanceAccessStatus.READY),
                AttendanceAccessReadiness(AttendanceAccess.NOTIFICATION, AttendanceAccessStatus.READY),
                AttendanceAccessReadiness(AttendanceAccess.BACKGROUND_LOCATION, AttendanceAccessStatus.READY)
            )
        )

        assertFalse(result.canEnterAttendance)
        assertEquals(AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY, result.entryOf(AttendanceAccess.PRECISE_LOCATION)?.reason)
        assertEquals(2, result.requiredReadyCount)
    }

    @Test
    fun cameraMissing_blocksAttendance() {
        val result = readiness(camera = AttendanceAccessStatus.ACTION_REQUIRED)

        assertFalse(result.canEnterAttendance)
        assertEquals(2, result.requiredReadyCount)
    }

    @Test
    fun deviceLocationDisabled_blocksAttendance() {
        val result = readiness(deviceLocation = AttendanceAccessStatus.DEVICE_LOCATION_DISABLED)

        assertFalse(result.canEnterAttendance)
        assertEquals(2, result.requiredReadyCount)
    }

    @Test
    fun optionalEntriesDegraded_remainNonBlocking() {
        val result = readiness(
            notification = AttendanceAccessStatus.DEGRADED,
            background = AttendanceAccessStatus.ACTION_REQUIRED
        )

        assertTrue(result.canEnterAttendance)
        assertEquals(AttendanceOptionalCapabilitySummary.DEGRADED, result.optionalCapabilitySummary)
    }

    @Test
    fun optionalEntriesUnsupported_areSummarizedAsNotRequired() {
        val result = readiness(
            notification = AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE,
            background = AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
        )

        assertTrue(result.canEnterAttendance)
        assertEquals(AttendanceOptionalCapabilitySummary.NOT_REQUIRED, result.optionalCapabilitySummary)
    }

    @Test
    fun missingRequiredEntry_failsClosed() {
        val result = readiness().copy(
            entries = readiness().entries.filterNot { it.access == AttendanceAccess.CAMERA }
        )

        assertFalse(result.canEnterAttendance)
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, result.statusOf(AttendanceAccess.CAMERA))
        assertEquals(2, result.requiredReadyCount)
        assertNull(result.entryOf(AttendanceAccess.CAMERA))
    }

    @Test
    fun requiredInspectionIssue_overPreviouslyReadySnapshot_blocksAttendance() {
        val issue = AttendancePermissionInspectionIssue(
            failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
            affectedAccesses = setOf(AttendanceAccess.CAMERA)
        )
        val result = readiness().applyingInspectionIssues(listOf(issue))

        assertFalse(result.canEnterAttendance)
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, result.statusOf(AttendanceAccess.CAMERA))
        assertEquals(AttendanceAccessRecovery.NONE, result.entryOf(AttendanceAccess.CAMERA)?.recovery)
    }

    @Test
    fun optionalOnlyInspectionIssue_remainsNonBlocking() {
        val issue = AttendancePermissionInspectionIssue(
            failure = AttendancePermissionFailure.UNKNOWN,
            affectedAccesses = setOf(AttendanceAccess.NOTIFICATION)
        )
        val result = readiness().applyingInspectionIssues(listOf(issue))

        assertTrue(result.canEnterAttendance)
        assertEquals(AttendanceAccessStatus.DEGRADED, result.statusOf(AttendanceAccess.NOTIFICATION))
        assertEquals(AttendanceOptionalCapabilitySummary.DEGRADED, result.optionalCapabilitySummary)
    }

    @Test
    fun deniedAndPermanentlyDeniedRequestEvidence_overlaysNonReadyEntries() {
        val result = readiness(
            camera = AttendanceAccessStatus.ACTION_REQUIRED,
            deviceLocation = AttendanceAccessStatus.ACTION_REQUIRED
        ).applyingRequestOutcomes(
            mapOf(
                AttendanceAccess.CAMERA to AttendancePermissionRequestOutcome.DENIED,
                AttendanceAccess.DEVICE_LOCATION to AttendancePermissionRequestOutcome.PERMANENTLY_DENIED,
                AttendanceAccess.PRECISE_LOCATION to AttendancePermissionRequestOutcome.GRANTED
            )
        )

        assertEquals(AttendanceAccessStatus.DENIED, result.statusOf(AttendanceAccess.CAMERA))
        assertEquals(AttendanceAccessStatus.PERMANENTLY_DENIED, result.statusOf(AttendanceAccess.DEVICE_LOCATION))
        assertEquals(
            AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS,
            result.entryOf(AttendanceAccess.DEVICE_LOCATION)?.recovery
        )
        assertEquals(AttendanceAccessStatus.READY, result.statusOf(AttendanceAccess.PRECISE_LOCATION))
    }

    @Test
    fun grantedRequestEvidence_doesNotPromoteANonReadyRequiredEntry() {
        val result = readiness(
            camera = AttendanceAccessStatus.ACTION_REQUIRED
        ).applyingRequestOutcomes(
            mapOf(AttendanceAccess.CAMERA to AttendancePermissionRequestOutcome.GRANTED)
        )

        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, result.statusOf(AttendanceAccess.CAMERA))
        assertFalse(result.canEnterAttendance)
    }

    @Test
    fun unavailable_failsClosedForRequiredAndDegradesOptionalEntries() {
        val result = AttendancePermissionReadiness.unavailable(
            AttendancePermissionFailure.DEVICE_LOCATION_STATUS_UNAVAILABLE
        )

        assertFalse(result.canEnterAttendance)
        assertEquals(0, result.requiredReadyCount)
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, result.statusOf(AttendanceAccess.CAMERA))
        assertEquals(AttendanceAccessStatus.DEGRADED, result.statusOf(AttendanceAccess.NOTIFICATION))
        assertEquals(
            AttendancePermissionFailure.DEVICE_LOCATION_STATUS_UNAVAILABLE,
            result.inspectionIssues.single().failure
        )
    }

    private fun readiness(
        precise: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        camera: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        deviceLocation: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        notification: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        background: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = AttendancePermissionReadiness(
        entries = listOf(
            AttendanceAccessReadiness(AttendanceAccess.PRECISE_LOCATION, precise),
            AttendanceAccessReadiness(AttendanceAccess.CAMERA, camera),
            AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, deviceLocation),
            AttendanceAccessReadiness(AttendanceAccess.NOTIFICATION, notification),
            AttendanceAccessReadiness(AttendanceAccess.BACKGROUND_LOCATION, background)
        ),
        inspectionIssues = inspectionIssues
    )
}
