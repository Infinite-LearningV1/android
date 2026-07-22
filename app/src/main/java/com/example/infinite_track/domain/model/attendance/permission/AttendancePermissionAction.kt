package com.example.infinite_track.domain.model.attendance.permission

sealed interface AttendancePermissionNextAction {
    data class RequestPermission(val access: AttendanceAccess) : AttendancePermissionNextAction
    data class OpenApplicationSettings(val access: AttendanceAccess) : AttendancePermissionNextAction
    data object OpenDeviceLocationSettings : AttendancePermissionNextAction
    data object ContinueToWorkMode : AttendancePermissionNextAction
    data object RetryRefresh : AttendancePermissionNextAction
    data object None : AttendancePermissionNextAction
}
