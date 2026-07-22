package com.example.infinite_track.data.soucre.local.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import kotlinx.coroutines.CancellationException

class AndroidAttendancePermissionDataSource(
    private val context: Context
) : AttendancePermissionDataSource {
    override fun readSnapshot(): AttendancePermissionSnapshotResult = try {
        val issues = mutableListOf<AttendancePermissionInspectionIssue>()
        val fine = readPermission(Manifest.permission.ACCESS_FINE_LOCATION, AttendanceAccess.PRECISE_LOCATION, issues)
        val coarse = readPermission(Manifest.permission.ACCESS_COARSE_LOCATION, AttendanceAccess.PRECISE_LOCATION, issues)
        val camera = readPermission(Manifest.permission.CAMERA, AttendanceAccess.CAMERA, issues)
        val notification = if (Build.VERSION.SDK_INT >= 33) readPermission(Manifest.permission.POST_NOTIFICATIONS, AttendanceAccess.NOTIFICATION, issues) else true
        val background = if (Build.VERSION.SDK_INT >= 29) readPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, AttendanceAccess.BACKGROUND_LOCATION, issues) else true
        AttendancePermissionSnapshotResult.Success(AttendancePermissionPlatformSnapshot(Build.VERSION.SDK_INT, fine, coarse, camera, notification, background, readDeviceLocation(issues), issues))
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        AttendancePermissionSnapshotResult.Failure(error.toFailure(), requiredAccesses)
    }

    private fun readPermission(permission: String, access: AttendanceAccess, issues: MutableList<AttendancePermissionInspectionIssue>): Boolean = try {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        issues += AttendancePermissionInspectionIssue(error.toFailure(), setOf(access))
        false
    }

    private fun readDeviceLocation(issues: MutableList<AttendancePermissionInspectionIssue>): DeviceLocationPlatformStatus {
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return DeviceLocationPlatformStatus.UNAVAILABLE.also { issues += unavailableDeviceLocationIssue() }
            if (LocationManagerCompat.isLocationEnabled(manager)) DeviceLocationPlatformStatus.ENABLED else DeviceLocationPlatformStatus.DISABLED
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            issues += AttendancePermissionInspectionIssue(error.toFailure(), setOf(AttendanceAccess.DEVICE_LOCATION))
            DeviceLocationPlatformStatus.UNAVAILABLE
        }
    }

    private fun unavailableDeviceLocationIssue() = AttendancePermissionInspectionIssue(
        AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE, setOf(AttendanceAccess.DEVICE_LOCATION)
    )

    private fun Throwable.toFailure() = if (this is SecurityException) AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE else AttendancePermissionFailure.UNKNOWN

    private companion object {
        val requiredAccesses = setOf(AttendanceAccess.PRECISE_LOCATION, AttendanceAccess.CAMERA, AttendanceAccess.DEVICE_LOCATION)
    }
}
