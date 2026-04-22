package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
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
    val targetLocation: Location? = null,
    val wfoLocation: Location? = null,
    val wfhLocation: Location? = null,
    val wfaRecommendations: List<WfaRecommendation> = emptyList(),
    val selectedWfaLocation: WfaRecommendation? = null,
    val selectedWfaMarkerInfo: WfaRecommendation? = null,
    val isWfaModeActive: Boolean = false,
    val isLoadingWfaRecommendations: Boolean = false,
    val currentUserAddress: String = "",
    val currentUserLatitude: Double? = null,
    val currentUserLongitude: Double? = null,
    val isBookingEnabled: Boolean = false,
    val selectedWorkMode: String = "Work From Office",
    val targetLocationMarker: Location? = null,
    val selectedMarkerInfo: Location? = null,
    val pickedLocation: LocationResult? = null,
    val isPickOnMapModeActive: Boolean = false,
    val error: String? = null,
    val buttonText: String = "Loading...",
    val isButtonEnabled: Boolean = false,
    val isCheckInMode: Boolean = true,
    val navigationTarget: NavigationTarget? = null,
    val activeDialog: DialogState? = null,
    val mapAnimationTarget: MapAnimationTarget? = null,
    val showPermissionDialog: Boolean = false,
    val permissionResult: LocationPermissionHelper.PermissionResult? = null,
    val permissionMessage: String = ""
)