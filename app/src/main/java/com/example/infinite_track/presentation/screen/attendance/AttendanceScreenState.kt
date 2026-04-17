package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.location.LocationResult
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.utils.LocationPermissionHelper
import com.example.infinite_track.utils.UiState
import com.mapbox.geojson.Point

/**
 * Comprehensive state class untuk AttendanceScreen
 * Mengelola semua state termasuk permission handling untuk geofencing
 */
data class AttendanceScreenState(
    // Core UI state
    val uiState: UiState = UiState.Loading,
    val error: String? = null,
    
    // User data
    val todayStatus: TodayStatus? = null,
    val isCheckInMode: Boolean = true,
    val isButtonEnabled: Boolean = false,
    val selectedWorkMode: String = "",
    
    // Location data
    val currentUserLatitude: Double? = null,
    val currentUserLongitude: Double? = null,
    val currentUserAddress: String = "Mengambil alamat...",
    val wfhLocation: Location? = null,
    val wfoLocation: Location? = null,
    
    // WFA (Work From Anywhere) related
    val isWfaModeActive: Boolean = false,
    val wfaRecommendations: List<WfaRecommendation> = emptyList(),
    val isLoadingWfaRecommendations: Boolean = false,
    val selectedWfaLocation: WfaRecommendation? = null,
    val selectedWfaMarkerInfo: WfaRecommendation? = null,
    
    // Map related
    val mapAnimationTarget: Point? = null,
    val selectedMarkerInfo: Location? = null,
    val isPickOnMapModeActive: Boolean = false,
    val pickedLocation: LocationResult? = null,
    
    // Dialog and navigation
    val activeDialog: DialogState? = null,
    val navigationTarget: NavigationTarget? = null,
    
    // Permission handling for geofencing
    val showPermissionDialog: Boolean = false,
    val permissionResult: LocationPermissionHelper.PermissionResult? = null,
    val permissionMessage: String = ""
)