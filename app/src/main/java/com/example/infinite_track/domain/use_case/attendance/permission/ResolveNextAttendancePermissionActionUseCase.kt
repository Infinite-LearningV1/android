package com.example.infinite_track.domain.use_case.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRequirement
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionNextAction
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import javax.inject.Inject

class ResolveNextAttendancePermissionActionUseCase @Inject constructor() {
    private val requiredOrder = listOf(
        AttendanceAccess.PRECISE_LOCATION,
        AttendanceAccess.CAMERA,
        AttendanceAccess.DEVICE_LOCATION
    )

    fun forPrimary(
        readiness: AttendancePermissionReadiness
    ): AttendancePermissionNextAction {
        if (readiness.inspectionIssues.any { it.blocksManualAttendance }) {
            return AttendancePermissionNextAction.RetryRefresh
        }
        val nextAccess = requiredOrder.firstOrNull {
            readiness.statusOf(it) != AttendanceAccessStatus.READY
        } ?: return AttendancePermissionNextAction.ContinueToWorkMode
        return forAccess(readiness, nextAccess)
    }

    fun forAccess(
        readiness: AttendancePermissionReadiness,
        access: AttendanceAccess
    ): AttendancePermissionNextAction {
        val entry = readiness.entryOf(access)
            ?: return if (access.requirement == AttendanceAccessRequirement.REQUIRED) {
                AttendancePermissionNextAction.RetryRefresh
            } else {
                AttendancePermissionNextAction.None
            }
        if (
            entry.status == AttendanceAccessStatus.READY ||
            entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
        ) {
            return AttendancePermissionNextAction.None
        }
        return when (entry.recovery) {
            AttendanceAccessRecovery.REQUEST_PERMISSION ->
                AttendancePermissionNextAction.RequestPermission(access)
            AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS ->
                AttendancePermissionNextAction.OpenApplicationSettings(access)
            AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS ->
                AttendancePermissionNextAction.OpenDeviceLocationSettings
            AttendanceAccessRecovery.NONE ->
                AttendancePermissionNextAction.RetryRefresh
        }
    }
}
