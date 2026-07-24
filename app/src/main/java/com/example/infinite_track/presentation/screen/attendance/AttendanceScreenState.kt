package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.UiState

/**
 * Unified state class for AttendanceScreen.
 * Keeps the existing attendance/map/navigation contract from this branch
 * while retaining the permission-dialog fields introduced from master.
 */
data class AttendanceScreenState(
    val uiState: UiState<Unit> = UiState.Loading,
    val todayStatus: TodayStatus? = null,
    val preparation: AttendancePreparationState = AttendancePreparationState(),
    val currentUserAddress: String = "",
    val error: String? = null,
    val actionState: AttendanceActionState = AttendanceActionState.Loading,
    val geofenceRuntime: GeofenceRuntimeUiState = GeofenceRuntimeUiState(),
    val navigationTarget: NavigationTarget? = null,
    val showPermissionDialog: Boolean = false,
    val permissionResult: LocationPermissionHelper.PermissionResult? = null,
    val permissionMessage: String = ""
)
