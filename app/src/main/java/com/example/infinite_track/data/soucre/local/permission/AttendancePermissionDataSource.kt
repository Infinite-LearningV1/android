package com.example.infinite_track.data.soucre.local.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure

sealed interface AttendancePermissionSnapshotResult {
    data class Success(val snapshot: AttendancePermissionPlatformSnapshot) : AttendancePermissionSnapshotResult
    data class Failure(
        val failure: AttendancePermissionFailure,
        val affectedAccesses: Set<AttendanceAccess>
    ) : AttendancePermissionSnapshotResult
}

interface AttendancePermissionDataSource {
    fun readSnapshot(): AttendancePermissionSnapshotResult
}
