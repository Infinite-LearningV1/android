package com.example.infinite_track.data.soucre.local.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue

enum class DeviceLocationPlatformStatus { ENABLED, DISABLED, UNAVAILABLE }

data class AttendancePermissionPlatformSnapshot(
    val sdkInt: Int,
    val fineLocationGranted: Boolean,
    val coarseLocationGranted: Boolean,
    val cameraGranted: Boolean,
    val notificationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val deviceLocationStatus: DeviceLocationPlatformStatus,
    val inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
)
