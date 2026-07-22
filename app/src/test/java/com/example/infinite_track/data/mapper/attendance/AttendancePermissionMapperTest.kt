package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionPlatformSnapshot
import com.example.infinite_track.data.soucre.local.permission.DeviceLocationPlatformStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReason
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionMapperTest {
    @Test fun sdk28_marksOptionalCapabilitiesNotRequired() {
        val readiness = snapshot(sdkInt = 28).toAttendancePermissionReadiness()
        assertEquals(AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE, readiness.statusOf(AttendanceAccess.NOTIFICATION))
        assertEquals(AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE, readiness.statusOf(AttendanceAccess.BACKGROUND_LOCATION))
    }

    @Test fun sdk29_missingBackgroundLocationRequestsPermission() {
        val entry = snapshot(sdkInt = 29).toAttendancePermissionReadiness().entryOf(AttendanceAccess.BACKGROUND_LOCATION)!!
        assertEquals(AttendanceAccessStatus.DEGRADED, entry.status)
        assertEquals(AttendanceAccessRecovery.REQUEST_PERMISSION, entry.recovery)
    }

    @Test fun sdk30MissingBackgroundLocationOpensApplicationSettings() {
        val entry = snapshot(sdkInt = 30).toAttendancePermissionReadiness().entryOf(AttendanceAccess.BACKGROUND_LOCATION)!!
        assertEquals(AttendanceAccessStatus.DEGRADED, entry.status)
        assertEquals(AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS, entry.recovery)
    }

    @Test fun sdk33MissingNotificationIsDegradedAndRequestable() {
        val entry = snapshot(sdkInt = 33).toAttendancePermissionReadiness().entryOf(AttendanceAccess.NOTIFICATION)!!
        assertEquals(AttendanceAccessStatus.DEGRADED, entry.status)
        assertEquals(AttendanceAccessRecovery.REQUEST_PERMISSION, entry.recovery)
    }

    @Test fun sdk34GrantedOptionalCapabilitiesAreReady() {
        val readiness = snapshot(sdkInt = 34, notificationGranted = true, backgroundLocationGranted = true)
            .toAttendancePermissionReadiness()
        assertEquals(AttendanceAccessStatus.READY, readiness.statusOf(AttendanceAccess.NOTIFICATION))
        assertEquals(AttendanceAccessStatus.READY, readiness.statusOf(AttendanceAccess.BACKGROUND_LOCATION))
    }

    @Test fun coarseOnlyLocationBlocksPreciseLocationWithRecovery() {
        val entry = snapshot(fineLocationGranted = false, coarseLocationGranted = true)
            .toAttendancePermissionReadiness().entryOf(AttendanceAccess.PRECISE_LOCATION)!!
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, entry.status)
        assertEquals(AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY, entry.reason)
        assertEquals(AttendanceAccessRecovery.REQUEST_PERMISSION, entry.recovery)
    }

    @Test fun noLocationGrantBlocksPreciseLocationWithoutApproximateReason() {
        val entry = snapshot(fineLocationGranted = false, coarseLocationGranted = false)
            .toAttendancePermissionReadiness().entryOf(AttendanceAccess.PRECISE_LOCATION)!!
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, entry.status)
        assertEquals(AttendanceAccessReason.NONE, entry.reason)
        assertEquals(AttendanceAccessRecovery.REQUEST_PERMISSION, entry.recovery)
    }

    @Test fun missingCameraBlocksWithPermissionRecovery() {
        val entry = snapshot(cameraGranted = false).toAttendancePermissionReadiness().entryOf(AttendanceAccess.CAMERA)!!
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, entry.status)
        assertEquals(AttendanceAccessRecovery.REQUEST_PERMISSION, entry.recovery)
    }

    @Test fun disabledDeviceLocationHasSettingsRecovery() {
        val entry = snapshot(deviceLocationStatus = DeviceLocationPlatformStatus.DISABLED)
            .toAttendancePermissionReadiness().entryOf(AttendanceAccess.DEVICE_LOCATION)!!
        assertEquals(AttendanceAccessStatus.DEVICE_LOCATION_DISABLED, entry.status)
        assertEquals(AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS, entry.recovery)
    }

    @Test fun unavailableDeviceLocationFailsClosedWithScopedIssue() {
        val readiness = snapshot(deviceLocationStatus = DeviceLocationPlatformStatus.UNAVAILABLE).toAttendancePermissionReadiness()
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, readiness.statusOf(AttendanceAccess.DEVICE_LOCATION))
        assertEquals(AttendancePermissionFailure.DEVICE_LOCATION_STATUS_UNAVAILABLE, readiness.inspectionIssues.single().failure)
        assertEquals(setOf(AttendanceAccess.DEVICE_LOCATION), readiness.inspectionIssues.single().affectedAccesses)
    }

    @Test fun unavailableDeviceLocationNormalizesExistingDeviceLocationIssueToOneCanonicalIssue() {
        val readiness = snapshot(
            deviceLocationStatus = DeviceLocationPlatformStatus.UNAVAILABLE,
            inspectionIssues = listOf(issue(AttendanceAccess.DEVICE_LOCATION))
        ).toAttendancePermissionReadiness()
        assertEquals(1, readiness.inspectionIssues.size)
        assertEquals(AttendancePermissionFailure.DEVICE_LOCATION_STATUS_UNAVAILABLE, readiness.inspectionIssues.single().failure)
        assertEquals(setOf(AttendanceAccess.DEVICE_LOCATION), readiness.inspectionIssues.single().affectedAccesses)
    }

    @Test fun requiredInspectionFailureClearsNativeRecovery() {
        val entry = snapshot(
            cameraGranted = false,
            inspectionIssues = listOf(issue(AttendanceAccess.CAMERA))
        ).toAttendancePermissionReadiness().entryOf(AttendanceAccess.CAMERA)!!
        assertEquals(AttendanceAccessStatus.ACTION_REQUIRED, entry.status)
        assertEquals(AttendanceAccessRecovery.NONE, entry.recovery)
    }

    @Test fun optionalInspectionFailureDegradesOnlyAffectedOptionalAccess() {
        val readiness = snapshot(inspectionIssues = listOf(issue(AttendanceAccess.NOTIFICATION))).toAttendancePermissionReadiness()
        assertEquals(AttendanceAccessStatus.DEGRADED, readiness.statusOf(AttendanceAccess.NOTIFICATION))
        assertTrue(readiness.canEnterAttendance)
        assertFalse(readiness.inspectionIssues.single().blocksManualAttendance)
    }

    private fun snapshot(
        sdkInt: Int = 34,
        fineLocationGranted: Boolean = true,
        coarseLocationGranted: Boolean = true,
        cameraGranted: Boolean = true,
        notificationGranted: Boolean = false,
        backgroundLocationGranted: Boolean = false,
        deviceLocationStatus: DeviceLocationPlatformStatus = DeviceLocationPlatformStatus.ENABLED,
        inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = AttendancePermissionPlatformSnapshot(sdkInt, fineLocationGranted, coarseLocationGranted, cameraGranted, notificationGranted, backgroundLocationGranted, deviceLocationStatus, inspectionIssues)

    private fun issue(access: AttendanceAccess) = AttendancePermissionInspectionIssue(
        failure = AttendancePermissionFailure.UNKNOWN,
        affectedAccesses = setOf(access)
    )
}
