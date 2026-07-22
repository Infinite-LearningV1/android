package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.data.soucre.local.permission.AttendancePermissionPlatformSnapshot
import com.example.infinite_track.data.soucre.local.permission.DeviceLocationPlatformStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReason
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness

fun AttendancePermissionPlatformSnapshot.toAttendancePermissionReadiness(): AttendancePermissionReadiness {
    val deviceLocationIssue = if (deviceLocationStatus == DeviceLocationPlatformStatus.UNAVAILABLE) {
        AttendancePermissionInspectionIssue(
            failure = AttendancePermissionFailure.DEVICE_LOCATION_STATUS_UNAVAILABLE,
            affectedAccesses = setOf(AttendanceAccess.DEVICE_LOCATION)
        )
    } else null
    val readiness = AttendancePermissionReadiness(
        entries = listOf(
            AttendanceAccessReadiness(
                access = AttendanceAccess.PRECISE_LOCATION,
                status = if (fineLocationGranted) AttendanceAccessStatus.READY else AttendanceAccessStatus.ACTION_REQUIRED,
                reason = if (coarseLocationGranted && !fineLocationGranted) AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY else AttendanceAccessReason.NONE,
                recovery = if (fineLocationGranted) AttendanceAccessRecovery.NONE else AttendanceAccessRecovery.REQUEST_PERMISSION
            ),
            AttendanceAccessReadiness(AttendanceAccess.CAMERA, if (cameraGranted) AttendanceAccessStatus.READY else AttendanceAccessStatus.ACTION_REQUIRED, recovery = if (cameraGranted) AttendanceAccessRecovery.NONE else AttendanceAccessRecovery.REQUEST_PERMISSION),
            deviceLocationReadiness(),
            optionalReadiness(AttendanceAccess.NOTIFICATION, sdkInt >= 33, notificationGranted, AttendanceAccessRecovery.REQUEST_PERMISSION),
            optionalReadiness(AttendanceAccess.BACKGROUND_LOCATION, sdkInt >= 29, backgroundLocationGranted, if (sdkInt == 29) AttendanceAccessRecovery.REQUEST_PERMISSION else AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS)
        )
    )
    return readiness.applyingInspectionIssues(snapshotIssues(deviceLocationIssue))
}

private fun AttendancePermissionPlatformSnapshot.deviceLocationReadiness() = when (deviceLocationStatus) {
    DeviceLocationPlatformStatus.ENABLED -> AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, AttendanceAccessStatus.READY)
    DeviceLocationPlatformStatus.DISABLED -> AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, AttendanceAccessStatus.DEVICE_LOCATION_DISABLED, recovery = AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS)
    DeviceLocationPlatformStatus.UNAVAILABLE -> AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, AttendanceAccessStatus.ACTION_REQUIRED)
}

private fun optionalReadiness(access: AttendanceAccess, supported: Boolean, granted: Boolean, recovery: AttendanceAccessRecovery) = AttendanceAccessReadiness(
    access = access,
    status = when { !supported -> AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE; granted -> AttendanceAccessStatus.READY; else -> AttendanceAccessStatus.DEGRADED },
    recovery = if (supported && !granted) recovery else AttendanceAccessRecovery.NONE
)

private fun AttendancePermissionPlatformSnapshot.snapshotIssues(
    deviceLocationIssue: AttendancePermissionInspectionIssue?
): List<AttendancePermissionInspectionIssue> {
    if (deviceLocationIssue == null) return inspectionIssues
    return inspectionIssues.mapNotNull { issue ->
        val unaffectedAccesses = issue.affectedAccesses - AttendanceAccess.DEVICE_LOCATION
        issue.takeIf { unaffectedAccesses.isNotEmpty() }?.copy(affectedAccesses = unaffectedAccesses)
    } + deviceLocationIssue
}
